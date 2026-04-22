package com.gzasc.aishopping.gateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class IpRateLimitFilter implements GlobalFilter, Ordered {

    @Value("${ip-rate-limit.max-requests:30}")
    private int maxRequests;

    @Value("${ip-rate-limit.time-window-seconds:60}")
    private int timeWindowSeconds;

    private Cache<String, AtomicInteger> ipRequestCache;

    @jakarta.annotation.PostConstruct
    public void init() {
        ipRequestCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(timeWindowSeconds))
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = getClientIp(exchange.getRequest());
        AtomicInteger count = ipRequestCache.get(ip, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();

        log.debug("IP: {}, 当前请求次数: {}/{} (窗口: {}秒)", ip, currentCount, maxRequests, timeWindowSeconds);

        if (currentCount > maxRequests) {
            log.warn("IP: {} 请求过于频繁，已拦截", ip);
            return returnErrorResponse(exchange, HttpStatus.TOO_MANY_REQUESTS,
                    "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
        }

        return chain.filter(exchange);
    }

    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddress() != null
                    ? request.getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
        }
        return ip.split(",")[0].trim();
    }

    private Mono<Void> returnErrorResponse(ServerWebExchange exchange, HttpStatus status, String body) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
