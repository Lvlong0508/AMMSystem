package com.gzasc.aishopping.gateway.loadtest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP限流负载测试
 * 
 * 使用方法：
 * 1. 先启动Eureka Server
 * 2. 启动Gateway Service
 * 3. 启动Auth Service
 * 4. 运行此测试
 */
@Disabled("手动执行，需要服务启动")
public class IpRateLimitLoadTest {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:8080")
            .build();

    /**
     * 测试IP限流 - 连续发送35次请求
     * 预期：前30次返回200，后5次返回429
     */
    @Test
    void testIpRateLimit() {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);

        Flux.range(1, 35)
                .flatMap(i -> {
                    System.out.println("发送第 " + i + " 次请求...");
                    return webClient.get()
                            .uri("/api/user/auth/login")
                            .exchangeToMono(response -> {
                                int status = response.statusCode().value();
                                if (status == 200) {
                                    successCount.incrementAndGet();
                                    System.out.println("第 " + i + " 次: 成功 (200)");
                                } else if (status == 429) {
                                    blockedCount.incrementAndGet();
                                    System.out.println("第 " + i + " 次: 被限流 (429)");
                                } else {
                                    System.out.println("第 " + i + " 次: " + status);
                                }
                                return Mono.just(status);
                            })
                            .onErrorResume(e -> {
                                System.out.println("第 " + i + " 次请求出错: " + e.getMessage());
                                return Mono.just(-1);
                            });
                }, 5) // 并发数5
                .collectList()
                .block();

        System.out.println("\n=== 测试结果 ===");
        System.out.println("成功请求: " + successCount.get());
        System.out.println("被限流请求: " + blockedCount.get());
        System.out.println("预期: 成功30次，被限流5次");

        assert successCount.get() == 30 : "成功请求应为30次，实际" + successCount.get();
        assert blockedCount.get() == 5 : "被限流请求应为5次，实际" + blockedCount.get();
    }

    /**
     * 测试认证拦截 - 未携带Token访问需认证接口
     * 预期：返回401
     */
    @Test
    void testAuthReject() {
        webClient.get()
                .uri("/api/user/order/list")
                .exchangeToMono(response -> {
                    int status = response.statusCode().value();
                    System.out.println("未登录访问订单接口状态: " + status);
                    assert status == 401 : "预期401，实际" + status;
                    return Mono.just(status);
                })
                .block();
    }

    /**
     * 测试白名单放行 - 访问登录接口
     * 预期：返回200（即使未登录）
     */
    @Test
    void testWhiteListPass() {
        webClient.post()
                .uri("/api/user/auth/login")
                .bodyValue("{\"username\":\"test\",\"password\":\"test\"}")
                .header("Content-Type", "application/json")
                .exchangeToMono(response -> {
                    int status = response.statusCode().value();
                    System.out.println("访问登录接口状态: " + status);
                    // 登录可能失败返回400/401，但不会被网关拦截
                    assert status != 401 : "白名单接口不应返回401";
                    return Mono.just(status);
                })
                .block();
    }
}
