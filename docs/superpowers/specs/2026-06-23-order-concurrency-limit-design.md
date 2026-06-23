# 下单接口并发限流设计 (Semaphore 学习版)

- 日期: 2026-06-23
- 模块: AI-Shopping-backend/order-service
- 学习目标: Semaphore、tryAcquire(timeout)、公平模式、try-finally 异常安全、@ConfigurationProperties 配置绑定、并发单元测试

---

## 1. 背景与目标

当前下单接口 `POST /api/user/order/place`(`OrderUserController.placeOrder`)由 Tomcat 线程池直接处理,理论并发上限由 Tomcat 决定,下游 Feign / 数据库压力随之放大。

本次目标是给下单接口加一个**学习性质**的并发限流:

- 全局同一时刻最多 **3** 个下单请求在执行
- 第 4 个请求开始排队等待
- 等待最长 **5 秒**,超时后向调用方返回错误,不让请求被无限阻塞
- 实现保持单机即可(不做分布式限流)
- 代码加注释,便于回顾时理解每一行的作用

## 2. 范围

| 项 | 是否在本次范围 |
|---|---|
| order-service 内新增 Limiter 组件 | 是 |
| 改造 OrderUserController.placeOrder 包一层 Limiter | 是 |
| 新增配置项 order.concurrency.* | 是 |
| 新增并发单元测试 | 是 |
| 改动 OrderServiceImpl.createOrder 内部逻辑 | 否 |
| 接入 Sentinel 或分布式限流 | 否 |
| 抽成 AOP 注解 | 否(留作后续) |
| 其他接口(payOrder/cancelOrder 等)也加限流 | 否 |

## 3. 用户决策记录

| 项 | 决策 |
|---|---|
| 实现方式 | 方案 A:Semaphore |
| 限流维度 | 全局(单 JVM 实例内一个 Semaphore) |
| 超出行为 | 排队等待,超时返回错误 |
| 等待超时 | 5000ms |
| 许可证数 | 3 |
| 参数位置 | application.yml 可配置 |
| 错误返回 | 抛 OrderException,由现有 GlobalExceptionHandler 统一处理 |
| 拦截位置 | 抽 OrderConcurrencyLimiter 组件,Controller 调用 |
| 公平模式 | 公平模式 (new Semaphore(3, true)) |
| 日志 | 详细日志(请求进入 / 获取许可 / 释放 / 超时) |
| 验证方式 | JUnit 并发单元测试 |
| 代码风格 | 关键步骤加中文注释 |

## 4. 总体设计

### 4.1 时序示意

```
T1, T2, T3 同时到达
   tryAcquire 立即成功 -> 并发执行 createOrder

T4 到达
   tryAcquire 阻塞,等待 <= 5s
   某个 Tx 完成 release -> T4 立即拿到许可,继续执行

T5..Tn 在 5s 内拿不到许可
   tryAcquire 返回 false
   -> 抛 OrderException("下单请求过多,请稍后重试")
   -> GlobalExceptionHandler -> ApiResponse.error
```

### 4.2 组件清单(新增)

```
order-service/src/main/java/com/gzasc/aishopping/order/
  concurrency/
    OrderConcurrencyProperties.java   # @ConfigurationProperties 配置类
    OrderConcurrencyLimiter.java      # Semaphore 封装组件 (核心)

order-service/src/test/java/com/gzasc/aishopping/order/
  concurrency/
    OrderConcurrencyLimiterTest.java  # 并发单元测试
```

改造 1 处:`OrderUserController.placeOrder` 包一层 `limiter.execute(...)`。

配置新增 1 处:`order-service/src/main/resources/application.yml` 末尾追加 `order.concurrency.{max-permits, wait-timeout-ms, fair}`。

## 5. 详细设计

### 5.1 配置类 OrderConcurrencyProperties

```java
@ConfigurationProperties(prefix = "order.concurrency")
@Data
public class OrderConcurrencyProperties {
    /** 允许同时执行的下单请求数,默认 3 */
    private int maxPermits = 3;
    /** 排队等待许可的最长时间,超过即抛 OrderException,单位毫秒,默认 5000 */
    private long waitTimeoutMs = 5000L;
    /** 是否公平模式 (FIFO),默认 true 便于学习 Semaphore 公平/非公平差异 */
    private boolean fair = true;
}
```

注册方式:在 `OrderServiceApplication` 上加 `@EnableConfigurationProperties(OrderConcurrencyProperties.class)`,不在配置类上加 `@Component`,避免双重注册。

### 5.2 限流器 OrderConcurrencyLimiter

职责:

1. 持有全局唯一 Semaphore(许可数 = maxPermits,模式 = fair)
2. 暴露 `<T> T execute(Supplier<T> task)`:在执行 task 前 tryAcquire(timeout),执行完 release()
3. 超时抛 OrderException
4. 中断 -> 还原中断标志 -> 抛 OrderException
5. 关键事件打日志:请求进入 / 获取成功 / 获取超时 / 释放许可

伪代码(实现时需保留这些中文注释):

```java
@Slf4j
@Component
public class OrderConcurrencyLimiter {

    // Semaphore: JUC 提供的"许可证"同步器,内部基于 AQS 实现
    private final Semaphore semaphore;
    private final long timeoutMs;
    // 多线程读写,用 AtomicLong 避免竞态;只用作监控指标
    private final AtomicLong waitingCount = new AtomicLong(0);

    public OrderConcurrencyLimiter(OrderConcurrencyProperties props) {
        // 第二参数 fair=true: 公平模式,等待最久的线程优先拿到许可,
        // 避免某些线程长期"插队"造成饥饿;代价是吞吐略低
        this.semaphore = new Semaphore(props.getMaxPermits(), props.isFair());
        this.timeoutMs = props.getWaitTimeoutMs();
        log.info("[Limiter] 初始化: maxPermits={}, fair={}, timeoutMs={}",
                props.getMaxPermits(), props.isFair(), props.getWaitTimeoutMs());
    }

    public <T> T execute(Supplier<T> task) {
        long start = System.currentTimeMillis();
        long waiting = waitingCount.incrementAndGet();
        log.info("[Limiter] 请求进入,等待中={},剩余许可={}",
                waiting, semaphore.availablePermits());

        boolean acquired;
        try {
            // tryAcquire(timeout) 三种返回:
            //  1) 立即/在 timeout 内拿到许可 -> 返回 true
            //  2) timeout 时间内始终拿不到 -> 返回 false
            //  3) 等待途中被 interrupt -> 抛 InterruptedException
            acquired = semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // 还原中断标志(Java 并发最佳实践:不要吞掉中断状态)
            Thread.currentThread().interrupt();
            waitingCount.decrementAndGet();
            throw new OrderException("下单请求被中断,请稍后重试");
        }

        if (!acquired) {
            waitingCount.decrementAndGet();
            log.warn("[Limiter] 获取许可超时 {}ms,拒绝请求", timeoutMs);
            throw new OrderException("下单请求过多,请稍后重试");
        }

        // 拿到许可后,把等待计数回退;此线程不再"等待"而是"执行中"
        waitingCount.decrementAndGet();
        long acquiredAt = System.currentTimeMillis();
        log.info("[Limiter] 获取许可成功,等待耗时={}ms,剩余许可={}",
                acquiredAt - start, semaphore.availablePermits());

        try {
            return task.get();
        } finally {
            // 关键:release 必须放 finally,确保 task 抛异常时也能释放许可,
            // 否则一次异常就"永久消耗"一个许可,导致系统逐渐变慢甚至卡死
            semaphore.release();
            log.info("[Limiter] 释放许可,总耗时={}ms,剩余许可={}",
                    System.currentTimeMillis() - start, semaphore.availablePermits());
        }
    }
}
```

注释要点(实现时必须保留):

- Semaphore 构造的两个参数解释(permits / fair)
- tryAcquire(timeout) 三种返回情况
- Thread.currentThread().interrupt() 为什么要还原中断标志
- try-finally release() 为什么必须放在 finally
- waitingCount 用 AtomicLong 而不是 int 的原因

### 5.3 配置 yml 改动

`order-service/src/main/resources/application.yml` 末尾追加:

```yaml
# ============================================================
# 下单接口并发限流(Semaphore 学习版)
# ============================================================
order:
  concurrency:
    # 同时允许执行的下单请求数
    max-permits: 3
    # 排队等待许可的最长时间,超过即返回"下单请求过多"
    wait-timeout-ms: 5000
    # 公平模式: true=FIFO 避免线程饥饿;false=吞吐略高但可能饥饿
    fair: true
```

### 5.4 Controller 改造

只动 `placeOrder` 一个方法,其它方法不变:

```java
private final OrderConcurrencyLimiter limiter;

@PostMapping("/place")
public ApiResponse<String> placeOrder(@RequestBody @Valid PlaceOrderRequest request,
                                      @RequestHeader("X-User-Id") Long userId) {
    // 通过 limiter 包装:全局同时最多 3 个 createOrder 在执行
    String orderId = limiter.execute(() -> orderService.createOrder(request, userId));
    return ApiResponse.success("创建订单成功", orderId);
}
```

### 5.5 异常处理

OrderException 已被 order-service 的 GlobalExceptionHandler 捕获并转为 ApiResponse.error(...)。实现时需要先确认项目内 `controller/GlobalExceptionHandler` 与 `config/GlobalExceptionHandler` 哪一个真正生效(由 `@RestControllerAdvice` + 包扫描决定),然后据此核对响应体格式。本次设计不新增异常类型,也不改全局异常处理逻辑。


## 6. 单元测试设计

文件: `OrderConcurrencyLimiterTest.java`(纯 Java,不依赖 Spring 容器,通过 `new OrderConcurrencyLimiter(new OrderConcurrencyProperties())` 直接构造,便于在测试中按用例改 permits/timeout)。

| 用例 | 描述 | 断言 |
|---|---|---|
| `concurrent_running_never_exceeds_max_permits` | 起 6 个线程并发调用 `limiter.execute(慢任务)`,慢任务里 `AtomicInteger.incrementAndGet()` 跟踪"正在执行中的线程数"并记录峰值 | 峰值 <= 3 |
| `fourth_request_waits_until_release` | 3 个线程占住 1 秒慢任务,第 4 个线程紧随其后入场 | 第 4 个线程的等待耗时 ≈ 1 秒(容差 ±300ms) |
| `timeout_throws_OrderException` | 3 个线程占住 3 秒慢任务,limiter 配置 timeout=300ms,新线程立刻入场 | 抛 OrderException,message 含"下单请求过多" |
| `release_on_task_exception` | task 内部抛 RuntimeException | 1) 异常被原样抛出;2) `availablePermits()` 恢复到 maxPermits |
| `interrupt_during_wait` | 用一个线程等待许可,主线程对它调用 `interrupt()` | 抛 OrderException 且该线程 `isInterrupted()` 为 true |
| `fairness_preserves_order` | (可选,公平模式下)按 FIFO 顺序入场的线程应该按 FIFO 顺序拿到许可 | 完成顺序与提交顺序一致(允许少量乱序) |

工具:`CountDownLatch` 控制起跑、`CyclicBarrier` 控制同步、`Executors.newFixedThreadPool(N)` 跑测试。所有测试用例需在 10 秒内完成,加 `@Timeout(10)`。

## 7. 不做的事 (YAGNI)

- 不动 `OrderServiceImpl.createOrder` 内部逻辑
- 不动 Redis Stream 事件、不动事务边界
- 不引入 AOP 注解(留作下一步进阶)
- 不接 Sentinel(项目已有 Sentinel,但本次专项学 Semaphore)
- 不做分布式限流(多实例分别限 3 即可)
- 不给其他接口加限流

## 8. 实现风险与对策

| 风险 | 对策 |
|---|---|
| `release()` 被漏调导致许可永久丢失 | 严格 `try { task.get(); } finally { release(); }`,单测 `release_on_task_exception` 兜底 |
| `waitingCount` 的 inc/dec 在异常路径漏减 | 三条退出路径(InterruptedException / 超时 / 拿到许可后)各 dec 一次,单测覆盖 |
| 公平模式下吞吐略低 | 学习场景可接受;后续可改 `fair: false` 对比观察 |
| 测试时间敏感导致 flaky | 容差 ±300ms,`@Timeout(10)` 兜底;CI 上若不稳可再放宽 |
| 与 Sentinel 限流叠加 | 当前 Sentinel 用于熔断降级 (`rule-type: degrade`),不与并发限流冲突 |

## 9. 验收标准

- [ ] 启动 order-service,日志看到 `[Limiter] 初始化: maxPermits=3, fair=true, timeoutMs=5000`
- [ ] 6 并发请求 `/api/user/order/place`,日志中始终最多 3 个"获取许可成功",其余等待并按 FIFO 拿到许可
- [ ] 30 并发请求且下单耗时较长时,部分请求 5s 后收到 `ApiResponse.error("下单请求过多,请稍后重试")`
- [ ] 修改 `application.yml` 的 `max-permits: 5` 重启,日志显示新值,峰值并发改为 5
- [ ] `OrderConcurrencyLimiterTest` 全部用例通过,`mvn -pl order-service test` 绿
- [ ] 代码 review 时,所有注释要点都能在源码中找到

## 10. 后续可扩展(本次不做)

- 抽 `@RateLimit(max=3, timeout=5000)` 注解 + AOP,复用到 payOrder / cancelOrder 等
- 用 Sentinel `@SentinelResource` + 并发线程数规则替代 Semaphore,对比体验
- 升级到方案 B:自定义 ThreadPoolExecutor + Future.get(timeout),亲眼看到 3 个 worker 轮转
- Micrometer 暴露 `waitingCount`、`availablePermits` 指标到 Prometheus
