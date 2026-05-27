package com.gzasc.aishopping.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.gateway.config.IpRateLimitProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class IpRateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(IpRateLimitFilter.class);

    private final int maxRequests;
    private final int timeWindowSeconds;
    private final ObjectMapper objectMapper;
    private final Cache<String, AtomicInteger> ipRequestCache;

    public IpRateLimitFilter(IpRateLimitProperties properties, ObjectMapper objectMapper) {
        this.maxRequests = properties.getMaxRequests();
        this.timeWindowSeconds = properties.getTimeWindowSeconds();
        this.objectMapper = objectMapper;
        this.ipRequestCache = Caffeine.newBuilder()
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
            return writeErrorResponse(exchange.getResponse(), HttpStatus.TOO_MANY_REQUESTS,
                    new ApiResponse<>(429, "请求过于频繁，请稍后再试", null));
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

    private Mono<Void> writeErrorResponse(ServerHttpResponse response, HttpStatus status, ApiResponse<?> body) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize rate limit response", e);
            DataBuffer buffer = response.bufferFactory()
                    .wrap("{\"code\":500,\"message\":\"系统错误\"}".getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
