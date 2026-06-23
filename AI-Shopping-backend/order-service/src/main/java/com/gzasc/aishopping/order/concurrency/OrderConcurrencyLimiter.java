package com.gzasc.aishopping.order.concurrency;

import com.gzasc.aishopping.order.exception.OrderException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 下单接口并发限流器(单机)。
 *
 * 通过 {@link Semaphore} 控制同时执行的下单数量,
 * 排队等待超时则抛 OrderException,由 GlobalExceptionHandler 转为 ApiResponse.error。
 *
 * 学习要点:
 * 1. Semaphore 构造的两个参数: permits(许可数)与 fair(是否公平)
 * 2. tryAcquire(timeout) 三种返回: true / false / InterruptedException
 * 3. InterruptedException 需还原中断标志
 * 4. release() 必须放在 finally,确保任务异常时也不丢许可
 * 5. waitingCount 用 AtomicLong,避免多线程竞态
 *
 * 配置通过 Nacos 配置中心(或 application.yml 降级)动态注入,
 * {@link RefreshScope} 确保 Nacos 推送时重建 bean,无需重启。
 */
@Slf4j
@Component
@RefreshScope
public class OrderConcurrencyLimiter {

    @Value("${order.concurrency.max-permits:3}")
    private int maxPermits;

    @Value("${order.concurrency.wait-timeout-ms:5000}")
    private long timeoutMs;

    @Value("${order.concurrency.fair:true}")
    private boolean fair;

    /** JUC 提供的"许可证"同步器,内部基于 AQS,在 init() 中创建 */
    private Semaphore semaphore;

    /** 当前正在排队等待许可的线程数(仅用于监控/日志,非业务用) */
    private final AtomicLong waitingCount = new AtomicLong(0);

    /** Spring 专用: @Value + @PostConstruct 组装 */
    public OrderConcurrencyLimiter() {
    }

    /** 测试专用: 直接传参,不依赖 Spring */
    public OrderConcurrencyLimiter(int maxPermits, long timeoutMs, boolean fair) {
        this.maxPermits = maxPermits;
        this.timeoutMs = timeoutMs;
        this.fair = fair;
        init();
    }

    @PostConstruct
    void init() {
        this.semaphore = new Semaphore(maxPermits, fair);
        log.info("[Limiter] 初始化: maxPermits={}, fair={}, timeoutMs={}", maxPermits, fair, timeoutMs);
    }

    /**
     * 在限流保护下执行 task。
     * 先尝试获取一个许可,拿到后执行 task,执行结束(无论成功失败)释放许可。
     *
     * @param task 业务逻辑(典型用法: () -> orderService.createOrder(...))
     * @return task 的返回值
     * @throws OrderException 等待许可超时或被中断
     */
    public <T> T execute(Supplier<T> task) {
        long start = System.currentTimeMillis();
        long waiting = waitingCount.incrementAndGet();
        log.info("[Limiter] 请求进入,等待中={}, 剩余许可={}",
                waiting, semaphore.availablePermits());

        boolean acquired;
        try {
            // tryAcquire(timeout) 三种返回:
            //  1) 拿到许可 -> true
            //  2) 等满 timeout 仍拿不到 -> false
            //  3) 等待途中被 interrupt -> 抛 InterruptedException
            acquired = semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // 还原中断标志: 不要吞掉中断状态,让上层(线程池/调度器)能感知
            Thread.currentThread().interrupt();
            waitingCount.decrementAndGet();
            log.warn("[Limiter] 等待许可时被中断");
            throw new OrderException("下单请求被中断,请稍后重试");
        }

        if (!acquired) {
            waitingCount.decrementAndGet();
            log.warn("[Limiter] 获取许可超时 {}ms,拒绝请求", timeoutMs);
            throw new OrderException("下单请求过多,请稍后重试");
        }

        // 拿到许可后,等待计数回退;此线程现在是"执行中"而非"等待中"
        waitingCount.decrementAndGet();
        long acquiredAt = System.currentTimeMillis();
        log.info("[Limiter] 获取许可成功,等待耗时={}ms, 剩余许可={}",
                acquiredAt - start, semaphore.availablePermits());

        try {
            return task.get();
        } finally {
            // 关键: release 必须放 finally。如果 task 抛异常时不释放,
            // 一次异常就永久"消耗"一个许可,系统会逐渐变慢直至卡死。
            semaphore.release();
            log.info("[Limiter] 释放许可,总耗时={}ms, 剩余许可={}",
                    System.currentTimeMillis() - start, semaphore.availablePermits());
        }
    }

    /** 仅供测试/监控使用 */
    public int availablePermits() {
        return semaphore.availablePermits();
    }
}
