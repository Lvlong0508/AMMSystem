package com.gzasc.aishopping.order.concurrency;

import com.gzasc.aishopping.order.exception.OrderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OrderConcurrencyLimiter 并发单元测试。
 * 所有用例不依赖 Spring 容器,直接 new。
 */
class OrderConcurrencyLimiterTest {

    /** 构造一个 props,便于按用例改参数 */
    private OrderConcurrencyProperties props(int maxPermits, long timeoutMs, boolean fair) {
        OrderConcurrencyProperties p = new OrderConcurrencyProperties();
        p.setMaxPermits(maxPermits);
        p.setWaitTimeoutMs(timeoutMs);
        p.setFair(fair);
        return p;
    }

    @Test
    @Timeout(10)
    @DisplayName("同时执行中的任务数永远不超过 maxPermits")
    void concurrent_running_never_exceeds_max_permits() throws Exception {
        OrderConcurrencyLimiter limiter = new OrderConcurrencyLimiter(props(3, 2000, true));

        AtomicInteger running = new AtomicInteger(0);
        AtomicInteger peak = new AtomicInteger(0);

        int N = 6;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(N);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < N; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    limiter.execute(() -> {
                        int now = running.incrementAndGet();
                        peak.accumulateAndGet(now, Math::max);
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        running.decrementAndGet();
                        return null;
                    });
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(8, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(peak.get())
                .as("限流必须真正生效: 在 N=6/permits=3 场景下,峰值应当打满到 3")
                .isEqualTo(3);
    }
}
