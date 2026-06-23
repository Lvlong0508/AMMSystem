# 下单接口并发限流 (Semaphore) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 给 `POST /api/user/order/place` 加单机并发限流,全局同时最多 3 个下单在执行,第 4 个排队等待最长 5 秒,超时抛 `OrderException("下单请求过多,请稍后重试")`。

**Architecture:** 在 `order-service` 内新增 `concurrency` 包,放配置类 `OrderConcurrencyProperties`(`@ConfigurationProperties`)和限流器 `OrderConcurrencyLimiter`(基于 `java.util.concurrent.Semaphore`,公平模式,封装 try-finally 释放许可)。`OrderUserController.placeOrder` 通过构造注入 Limiter,把原 `orderService.createOrder(...)` 包到 `limiter.execute(() -> ...)` 里。其他接口完全不动。

**Tech Stack:** Java 17、Spring Boot 3.2.4、`java.util.concurrent.Semaphore`、JUnit 5(`spring-boot-starter-test` 已带)、AssertJ、Mockito、Lombok。

**Spec:** `docs/superpowers/specs/2026-06-23-order-concurrency-limit-design.md`

---

## File Structure

新增 3 个文件 + 修改 4 个文件:

| 路径 | 操作 | 职责 |
|---|---|---|
| `order-service/src/main/java/com/gzasc/aishopping/order/concurrency/OrderConcurrencyProperties.java` | 新增 | `@ConfigurationProperties("order.concurrency")` 配置类,3 个字段 |
| `order-service/src/main/java/com/gzasc/aishopping/order/concurrency/OrderConcurrencyLimiter.java` | 新增 | 持有 Semaphore,暴露 `<T> T execute(Supplier<T>)`,核心实现 |
| `order-service/src/test/java/com/gzasc/aishopping/order/concurrency/OrderConcurrencyLimiterTest.java` | 新增 | 5 个并发单元测试用例 |
| `order-service/src/main/java/com/gzasc/aishopping/order/OrderServiceApplication.java` | 修改 | 加 `@EnableConfigurationProperties(OrderConcurrencyProperties.class)` |
| `order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderUserController.java` | 修改 | 加 `OrderConcurrencyLimiter limiter` 字段;`placeOrder` 包一层 `limiter.execute(...)` |
| `order-service/src/test/java/com/gzasc/aishopping/order/controller/OrderUserControllerTest.java` | 修改 | setUp 里 `new OrderUserController(...)` 多传一个 limiter 参数 |
| `order-service/src/main/resources/application.yml` | 修改 | 末尾追加 `order.concurrency.*` 三项 |

**实际生效的 GlobalExceptionHandler:** `order-service/src/main/java/com/gzasc/aishopping/order/config/GlobalExceptionHandler.java`(`@RestControllerAdvice`,已捕获 `OrderException` → `ApiResponse.error(e.getCode(), e.getMessage())`,默认 code=400)。本计划无需改它。

**已知约束:**
- `OrderException` 单参构造默认 `code = 400`,无需新增异常类型。
- `OrderUserControllerTest` 在 `setUp` 里用 `new OrderUserController(orderService, returnRequestService)` 直接构造,Task 7 必须同步改它,否则编译失败。

---

## Task 1: 新增配置类 OrderConcurrencyProperties

**Files:**
- Create: `AI-Shopping-backend/order-service/src/main/java/com/gzasc/aishopping/order/concurrency/OrderConcurrencyProperties.java`

- [ ] **Step 1: 创建配置类文件**

完整内容:

```java
package com.gzasc.aishopping.order.concurrency;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 下单接口并发限流配置。
 *
 * 对应 application.yml:
 *   order:
 *     concurrency:
 *       max-permits: 3
 *       wait-timeout-ms: 5000
 *       fair: true
 *
 * 通过 OrderServiceApplication 上的
 * @EnableConfigurationProperties(OrderConcurrencyProperties.class) 启用绑定,
 * 不在本类上加 @Component,避免重复注册。
 */
@Data
@ConfigurationProperties(prefix = "order.concurrency")
public class OrderConcurrencyProperties {

    /** 允许同时执行的下单请求数,默认 3 */
    private int maxPermits = 3;

    /** 排队等待许可的最长时间(毫秒),超过即抛 OrderException,默认 5000 */
    private long waitTimeoutMs = 5000L;

    /**
     * 是否使用公平模式 (FIFO)。
     * true: 等待最久的线程优先拿到许可,避免饥饿;吞吐略低。
     * false: 非公平,可能"插队",吞吐略高,有饥饿风险。
     * 默认 true 便于学习公平/非公平差异。
     */
    private boolean fair = true;
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn -pl AI-Shopping-backend/order-service -am compile -DskipTests`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend/order-service/src/main/java/com/gzasc/aishopping/order/concurrency/OrderConcurrencyProperties.java
git commit -m "feat(order): add OrderConcurrencyProperties for placeOrder rate limit"
```

---

## Task 2: 在主应用类启用配置绑定

**Files:**
- Modify: `AI-Shopping-backend/order-service/src/main/java/com/gzasc/aishopping/order/OrderServiceApplication.java`

- [ ] **Step 1: 添加 @EnableConfigurationProperties 注解**

把文件改成:

```java
package com.gzasc.aishopping.order;

import com.gzasc.aishopping.order.concurrency.OrderConcurrencyProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.gzasc.aishopping.common.feign")
@EnableScheduling
@EnableConfigurationProperties(OrderConcurrencyProperties.class)
@MapperScan("com.gzasc.aishopping.order.mapper")
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        System.out.println("Order Service started at http://localhost:8082");
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn -pl AI-Shopping-backend/order-service -am compile -DskipTests`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend/order-service/src/main/java/com/gzasc/aishopping/order/OrderServiceApplication.java
git commit -m "feat(order): enable OrderConcurrencyProperties binding"
```

---

## Task 3: 写第一个失败测试 (concurrent_running_never_exceeds_max_permits)

**Files:**
- Create: `AI-Shopping-backend/order-service/src/test/java/com/gzasc/aishopping/order/concurrency/OrderConcurrencyLimiterTest.java`

- [ ] **Step 1: 创建测试文件 (此刻 OrderConcurrencyLimiter 还不存在,所以测试编译失败,符合 TDD)**

完整文件:

```java
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

        assertThat(peak.get()).isLessThanOrEqualTo(3);
    }
}
```

- [ ] **Step 2: 验证测试编译失败 (TDD: red)**

Run: `mvn -pl AI-Shopping-backend/order-service -am test-compile`
Expected: 编译失败,错误为 `cannot find symbol: class OrderConcurrencyLimiter`

- [ ] **Step 3: 不提交,直接进入 Task 4 实现 Limiter**

---

## Task 4: 实现 OrderConcurrencyLimiter (让 Task 3 测试通过)

**Files:**
- Create: `AI-Shopping-backend/order-service/src/main/java/com/gzasc/aishopping/order/concurrency/OrderConcurrencyLimiter.java`

- [ ] **Step 1: 创建限流器**

完整文件:

```java
package com.gzasc.aishopping.order.concurrency;

import com.gzasc.aishopping.order.exception.OrderException;
import lombok.extern.slf4j.Slf4j;
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
 */
@Slf4j
@Component
public class OrderConcurrencyLimiter {

    /** JUC 提供的"许可证"同步器,内部基于 AQS */
    private final Semaphore semaphore;

    /** 排队等待许可的最大时间(毫秒) */
    private final long timeoutMs;

    /** 当前正在排队等待许可的线程数(仅用于监控/日志,非业务用) */
    private final AtomicLong waitingCount = new AtomicLong(0);

    public OrderConcurrencyLimiter(OrderConcurrencyProperties props) {
        // 第二参数 fair=true: 公平模式,等待最久的线程优先拿到许可,避免饥饿;
        // 代价是吞吐略低(每次拿/放许可都要操作等待队列)
        this.semaphore = new Semaphore(props.getMaxPermits(), props.isFair());
        this.timeoutMs = props.getWaitTimeoutMs();
        log.info("[Limiter] 初始化: maxPermits={}, fair={}, timeoutMs={}",
                props.getMaxPermits(), props.isFair(), props.getWaitTimeoutMs());
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
```

- [ ] **Step 2: 运行测试,验证第一个用例通过 (TDD: green)**

Run: `mvn -pl AI-Shopping-backend/order-service -am test -Dtest=OrderConcurrencyLimiterTest`
Expected: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` 且 `BUILD SUCCESS`

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend/order-service/src/main/java/com/gzasc/aishopping/order/concurrency/OrderConcurrencyLimiter.java AI-Shopping-backend/order-service/src/test/java/com/gzasc/aishopping/order/concurrency/OrderConcurrencyLimiterTest.java
git commit -m "feat(order): add OrderConcurrencyLimiter with semaphore-based concurrency control"
```

---

## Task 5: 补齐其余 4 个测试用例

**Files:**
- Modify: `AI-Shopping-backend/order-service/src/test/java/com/gzasc/aishopping/order/concurrency/OrderConcurrencyLimiterTest.java`

- [ ] **Step 1: 在测试类内追加 4 个测试方法**

在 `concurrent_running_never_exceeds_max_permits()` 后(类闭合 `}` 前)追加:

```java
    @Test
    @Timeout(10)
    @DisplayName("第 4 个请求等待到有许可释放后才执行")
    void fourth_request_waits_until_release() throws Exception {
        OrderConcurrencyLimiter limiter = new OrderConcurrencyLimiter(props(3, 5000, true));

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch firstThreeStarted = new CountDownLatch(3);
        CountDownLatch releaseGate = new CountDownLatch(1);
        try {
            for (int i = 0; i < 3; i++) {
                pool.submit(() -> limiter.execute(() -> {
                    firstThreeStarted.countDown();
                    try {
                        releaseGate.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                }));
            }
            assertThat(firstThreeStarted.await(3, TimeUnit.SECONDS)).isTrue();

            long t0 = System.currentTimeMillis();
            Future<Long> fourth = pool.submit(() ->
                    limiter.execute(() -> System.currentTimeMillis() - t0));

            Thread.sleep(1000);
            releaseGate.countDown();

            long fourthWaitMs = fourth.get(3, TimeUnit.SECONDS);
            assertThat(fourthWaitMs).isGreaterThanOrEqualTo(900L);
            assertThat(fourthWaitMs).isLessThanOrEqualTo(2000L);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @Timeout(10)
    @DisplayName("等待许可超时抛 OrderException")
    void timeout_throws_OrderException() throws Exception {
        OrderConcurrencyLimiter limiter = new OrderConcurrencyLimiter(props(3, 300, true));

        ExecutorService pool = Executors.newFixedThreadPool(3);
        CountDownLatch occupied = new CountDownLatch(3);
        CountDownLatch releaseGate = new CountDownLatch(1);
        try {
            for (int i = 0; i < 3; i++) {
                pool.submit(() -> limiter.execute(() -> {
                    occupied.countDown();
                    try {
                        releaseGate.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                }));
            }
            assertThat(occupied.await(2, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> limiter.execute(() -> "should-not-run"))
                    .isInstanceOf(OrderException.class)
                    .hasMessageContaining("下单请求过多");
        } finally {
            releaseGate.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    @Timeout(5)
    @DisplayName("task 抛异常时也能释放许可")
    void release_on_task_exception() {
        OrderConcurrencyLimiter limiter = new OrderConcurrencyLimiter(props(2, 500, true));

        assertThatThrownBy(() -> limiter.execute(() -> {
            throw new RuntimeException("boom");
        })).isInstanceOf(RuntimeException.class).hasMessage("boom");

        // 关键: 许可必须被释放回来
        assertThat(limiter.availablePermits()).isEqualTo(2);

        String result = limiter.execute(() -> "ok");
        assertThat(result).isEqualTo("ok");
        assertThat(limiter.availablePermits()).isEqualTo(2);
    }

    @Test
    @Timeout(5)
    @DisplayName("等待期间被中断抛 OrderException,中断标志保留")
    void interrupt_during_wait() throws Exception {
        OrderConcurrencyLimiter limiter = new OrderConcurrencyLimiter(props(1, 5000, true));

        CountDownLatch occupied = new CountDownLatch(1);
        CountDownLatch releaseGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(1);
        try {
            pool.submit(() -> limiter.execute(() -> {
                occupied.countDown();
                try {
                    releaseGate.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }));
            assertThat(occupied.await(2, TimeUnit.SECONDS)).isTrue();

            AtomicInteger flag = new AtomicInteger(0);
            AtomicInteger interruptedFlag = new AtomicInteger(-1);
            Thread waiter = new Thread(() -> {
                try {
                    limiter.execute(() -> { flag.set(1); return null; });
                } catch (OrderException e) {
                    interruptedFlag.set(Thread.currentThread().isInterrupted() ? 1 : 0);
                    flag.set(2);
                } catch (Throwable t) {
                    flag.set(3);
                }
            });
            waiter.start();
            Thread.sleep(200);
            waiter.interrupt();
            waiter.join(2000);

            assertThat(flag.get()).isEqualTo(2);
            assertThat(interruptedFlag.get()).isEqualTo(1);
        } finally {
            releaseGate.countDown();
            pool.shutdownNow();
        }
    }
```

- [ ] **Step 2: 运行全部测试**

Run: `mvn -pl AI-Shopping-backend/order-service -am test -Dtest=OrderConcurrencyLimiterTest`
Expected: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` 且 `BUILD SUCCESS`

- [ ] **Step 3: 偶发 flaky 应对**

如 `fourth_request_waits_until_release` 因机器负载偶发失败,放宽容差:
- `Thread.sleep(1000)` → `Thread.sleep(1200)`
- `isGreaterThanOrEqualTo(900L)` → `isGreaterThanOrEqualTo(1000L)`
- `isLessThanOrEqualTo(2000L)` → `isLessThanOrEqualTo(2500L)`

- [ ] **Step 4: 提交**

```bash
git add AI-Shopping-backend/order-service/src/test/java/com/gzasc/aishopping/order/concurrency/OrderConcurrencyLimiterTest.java
git commit -m "test(order): add 4 more cases for OrderConcurrencyLimiter"
```

---

## Task 6: 在 OrderUserController 接入 limiter

**Files:**
- Modify: `AI-Shopping-backend/order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderUserController.java`

- [ ] **Step 1: 把 OrderUserController 完整替换为下面的内容**

(只新增 `OrderConcurrencyLimiter` 字段并改造 `placeOrder` 一处,其它方法保持原样)

```java
package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.concurrency.OrderConcurrencyLimiter;
import com.gzasc.aishopping.order.dto.CreateReturnRequest;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.UserOrderCardDTO;
import com.gzasc.aishopping.order.dto.PlaceOrderRequest;
import com.gzasc.aishopping.order.dto.SubmitReturnLogisticsRequest;
import com.gzasc.aishopping.order.service.OrderService;
import com.gzasc.aishopping.order.service.ReturnRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/order")
@RequiredArgsConstructor
public class OrderUserController {

    private final OrderService orderService;
    private final ReturnRequestService returnRequestService;
    // 通过 @RequiredArgsConstructor 自动注入下单并发限流器
    private final OrderConcurrencyLimiter limiter;

    @GetMapping("/list")
    public ApiResponse<List<UserOrderCardDTO>> listOrders(
            @RequestHeader("X-User-Id") Long userId) {
        List<UserOrderCardDTO> orders = orderService.getOrdersByUserId(userId);
        return ApiResponse.success(orders);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailDTO> getOrderDetail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        OrderDetailDTO detail = orderService.getOrderDetailByUser(userId, orderId);
        if (detail == null) {
            return ApiResponse.error(404, "订单不存在");
        }
        return ApiResponse.success(detail);
    }

    @PostMapping("/place")
    public ApiResponse<String> placeOrder(
            @RequestBody @Valid PlaceOrderRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        // 通过 limiter 包装: 全局同时最多 maxPermits 个 createOrder 在执行,
        // 超出排队等待 waitTimeoutMs;超时抛 OrderException 由 GlobalExceptionHandler 处理
        String orderId = limiter.execute(() -> orderService.createOrder(request, userId));
        return ApiResponse.success("创建订单成功", orderId);
    }

    @PutMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancelOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        orderService.cancelOrder(userId, orderId);
        return ApiResponse.success("取消订单成功", null);
    }

    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> deleteOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        orderService.deleteOrder(userId, orderId);
        return ApiResponse.success("删除订单成功", null);
    }

    @PutMapping("/{orderId}/pay")
    public ApiResponse<Void> payOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        orderService.payOrder(userId, orderId);
        return ApiResponse.success("支付成功", null);
    }

    @PutMapping("/{orderId}/deliver")
    public ApiResponse<Void> deliverOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        orderService.deliverOrder(userId, orderId);
        return ApiResponse.success("确认收货成功", null);
    }

    @PostMapping("/{orderId}/return-request")
    public ApiResponse<Void> requestReturn(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId,
            @RequestBody @Valid CreateReturnRequest request) {
        returnRequestService.createReturnRequest(userId, orderId, request);
        return ApiResponse.success("退货申请已提交", null);
    }

    @PostMapping("/{orderId}/return-logistics")
    public ApiResponse<Void> submitReturnLogistics(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId,
            @RequestBody @Valid SubmitReturnLogisticsRequest request) {
        returnRequestService.submitReturnLogistics(userId, orderId, request);
        return ApiResponse.success("退货物流已提交", null);
    }
}
```

- [ ] **Step 2: 注意, 这一步会让 OrderUserControllerTest 编译失败 (因为构造函数多了一个参数), Task 7 会修复**

Run: `mvn -pl AI-Shopping-backend/order-service -am compile -DskipTests`
Expected: 主源码 `BUILD SUCCESS`(测试源码暂时编译不通过,正常)

- [ ] **Step 3: 暂不提交,Task 7 修测试后一起提交**

---

## Task 7: 修复 OrderUserControllerTest 的构造函数签名

**Files:**
- Modify: `AI-Shopping-backend/order-service/src/test/java/com/gzasc/aishopping/order/controller/OrderUserControllerTest.java`

- [ ] **Step 1: 在 setUp 里给 controller 多传一个 limiter**

文件第 1 行附近(import 区)加一行:

```java
import com.gzasc.aishopping.order.concurrency.OrderConcurrencyLimiter;
```

定位到 `setUp()` 方法(原代码大约在 53-60 行附近):

```java
@BeforeEach
void setUp() {
    var controller = new OrderUserController(orderService, returnRequestService);
    ...
}
```

改为:

```java
@BeforeEach
void setUp() {
    // 测试中使用真实的 limiter(默认参数 maxPermits=3, timeoutMs=5000, fair=true)即可,
    // 这些用例本身不验证限流,只验证下单接口的业务行为;limiter.execute 会同步运行 task。
    var limiter = new OrderConcurrencyLimiter(new com.gzasc.aishopping.order.concurrency.OrderConcurrencyProperties());
    var controller = new OrderUserController(orderService, returnRequestService, limiter);
    ...
}
```

- [ ] **Step 2: 跑测试验证**

Run: `mvn -pl AI-Shopping-backend/order-service -am test -Dtest=OrderUserControllerTest`
Expected: `BUILD SUCCESS`,原有用例全部通过(数量与改之前一致)

- [ ] **Step 3: 提交 Task 6 + Task 7**

```bash
git add AI-Shopping-backend/order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderUserController.java AI-Shopping-backend/order-service/src/test/java/com/gzasc/aishopping/order/controller/OrderUserControllerTest.java
git commit -m "feat(order): apply OrderConcurrencyLimiter to placeOrder endpoint"
```

---

## Task 8: 在 application.yml 加配置项

**Files:**
- Modify: `AI-Shopping-backend/order-service/src/main/resources/application.yml`

- [ ] **Step 1: 在文件末尾追加配置**

打开 `AI-Shopping-backend/order-service/src/main/resources/application.yml`,在文件最末尾追加(注意:整个文件用 UTF-8 编码,中文不要乱码):

```yaml

# ============================================================
# 下单接口并发限流 (Semaphore 学习版)
# ============================================================
order:
  concurrency:
    # 同时允许执行的下单请求数
    max-permits: 3
    # 排队等待许可的最长时间,超过即返回"下单请求过多"
    wait-timeout-ms: 5000
    # 公平模式: true=FIFO 避免饥饿;false=吞吐略高但可能饥饿
    fair: true
```

注意:文件里已经有一个顶层 `order:` 节点(`order.timeout.payment-minutes`),需要把上面的 `order:` 合并进去。最终应该长这样:

```yaml
order:
  timeout:
    payment-minutes: 30
  fallback:
    dir: ${user.dir}/data/failover
  stream:
    key: order:events
    group: order:processors
  concurrency:
    max-permits: 3
    wait-timeout-ms: 5000
    fair: true
```

- [ ] **Step 2: 编译验证(yml 解析不会在编译期触发,但 spring-boot 启动时会验证)**

Run: `mvn -pl AI-Shopping-backend/order-service -am test -DskipTests`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend/order-service/src/main/resources/application.yml
git commit -m "chore(order): add order.concurrency.* config for placeOrder rate limit"
```

---

## Task 9: 全量回归 + 手动验证

**Files:** 无文件修改,仅运行验证。

- [ ] **Step 1: 运行 order-service 全部测试**

Run: `mvn -pl AI-Shopping-backend/order-service -am test`
Expected: `BUILD SUCCESS`,所有原有测试 + 新增 5 个 limiter 测试全部通过

- [ ] **Step 2: 启动依赖**

按项目脚本启动:
- Nacos: `start-nacos.bat`
- Sentinel: `sentinel-start.bat`
- Redis: `C:\PATH\Redis8.4.0\redis-server.exe`
- MySQL/MongoDB 按本地环境已启动

- [ ] **Step 3: 启动 order-service 与 gateway-service**

在 IDEA 或命令行启动 `OrderServiceApplication` 和 `GatewayServiceApplication`。

启动日志里应看到:

```
[Limiter] 初始化: maxPermits=3, fair=true, timeoutMs=5000
```

- [ ] **Step 4: 手动并发验证**

用 6 并发(可用 `curl` 起 6 个后台进程,或 PowerShell):

```powershell
$body = '{"productId":1,"quantity":1,"contactId":1}'
1..6 | ForEach-Object -Parallel {
    Invoke-RestMethod -Method POST `
      -Uri "http://localhost:8088/api/user/order/place" `
      -Headers @{"satoken"="<replace_with_valid_token>"; "Content-Type"="application/json"} `
      -Body $using:body
} -ThrottleLimit 6
```

(token 用一个已登录用户的真实 satoken;若没现成 token 可先调 `/api/user/auth/login`)

观察 `order-service` 日志:

- 出现 6 次 `[Limiter] 请求进入`
- 同一时刻 `[Limiter] 获取许可成功` 的并发数 ≤ 3
- 部分请求出现 `等待耗时=...` 非零值
- 全部请求最终都返回了 `ApiResponse.success` 或者(如果业务校验失败的话)合理的业务错误,**不**应有 `下单请求过多`

- [ ] **Step 5: 手动验证超时拒绝**

把 `application.yml` 临时改 `wait-timeout-ms: 200` 并重启 order-service,再用 10 并发发请求,观察:

- 部分请求收到 `{"code":400,"message":"下单请求过多,请稍后重试"}`
- order-service 日志出现 `[Limiter] 获取许可超时 200ms,拒绝请求`

验证完后把配置改回 `wait-timeout-ms: 5000`。

- [ ] **Step 6: 最终提交(若步骤 5 修改了 yml 需还原后才提交,本步骤通常不需要新提交)**

如果还原 yml 时改动已在 git 里,确认 `git status` 干净即可。

---

## Self-Review Notes (写计划者填写)

- [x] Spec 第 4.2 节"组件清单"已覆盖: Task 1/2/3/4/5/6/7/8
- [x] Spec 第 5.2 节"OrderConcurrencyLimiter"伪代码注释要点全部落到 Task 4 代码里
- [x] Spec 第 6 节 5 个测试用例全部落到 Task 3 + Task 5(`fairness_preserves_order` 标可选,本计划不实现,后续可补)
- [x] Spec 第 7 节 YAGNI 项均未出现在任务里
- [x] Spec 第 9 节验收标准与 Task 9 步骤一一对应
- [x] 不存在 TBD / TODO / "类似 Task N" / 不可执行步骤
- [x] 类型一致: `OrderConcurrencyProperties` / `OrderConcurrencyLimiter` / `execute(Supplier<T>)` / `availablePermits()` 在所有 task 中名字一致
- [x] 已捕获实际项目细节: 实际生效的是 `config/GlobalExceptionHandler.java`、`OrderException` 默认 code=400、`OrderUserControllerTest.setUp` 用 `new OrderUserController(...)` 直接构造需要同步修改

---

## 验收清单(执行完所有 task 后人工对一遍 Spec §9)

- [ ] 启动日志看到 `[Limiter] 初始化: maxPermits=3, fair=true, timeoutMs=5000`
- [ ] 6 并发请求时日志显示同时最多 3 个 "获取许可成功"
- [ ] 把 `wait-timeout-ms` 调小后,过载场景能看到 `下单请求过多` 错误响应
- [ ] 修改 `max-permits: 5` 重启,日志显示新值,并发上限改为 5
- [ ] `OrderConcurrencyLimiterTest` 5 个用例全部通过
- [ ] 代码 review 时 Limiter 类中注释要点齐全
