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

    @Test
    @Timeout(10)
    @DisplayName("同时执行中的任务数永远不超过 maxPermits")
    void concurrent_running_never_exceeds_max_permits() throws Exception {
        OrderConcurrencyLimiter limiter = new OrderConcurrencyLimiter(3, 2000, true);

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

    @Test
    @Timeout(10)
    @DisplayName("第 4 个请求等待到有许可释放后才执行")
    void fourth_request_waits_until_release() throws Exception {
        OrderConcurrencyLimiter limiter = new OrderConcurrencyLimiter(3, 5000, true);

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
        OrderConcurrencyLimiter limiter = new OrderConcurrencyLimiter(3, 300, true);

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
        OrderConcurrencyLimiter limiter = new OrderConcurrencyLimiter(2, 500, true);

        assertThatThrownBy(() -> limiter.execute(() -> {
            throw new RuntimeException("boom");
        })).isInstanceOf(RuntimeException.class).hasMessage("boom");

        assertThat(limiter.availablePermits()).isEqualTo(2);

        String result = limiter.execute(() -> "ok");
        assertThat(result).isEqualTo("ok");
        assertThat(limiter.availablePermits()).isEqualTo(2);
    }

    @Test
    @Timeout(5)
    @DisplayName("等待期间被中断抛 OrderException,中断标志保留")
    void interrupt_during_wait() throws Exception {
        OrderConcurrencyLimiter limiter = new OrderConcurrencyLimiter(1, 5000, true);

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
}
