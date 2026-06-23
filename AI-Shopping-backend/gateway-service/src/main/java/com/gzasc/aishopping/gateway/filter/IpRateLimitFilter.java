package com.gzasc.aishopping.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.gateway.service.RedisRateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

@Slf4j
@Component
/**
 * IP 级别请求限流全局过滤器。
 *
 * 过滤器链中最先执行(Order=-200),对每个请求提取客户端 IP,
 * 通过 Redis 计数(默认 300 次/60s)判断是否超限。
 * 超限则直接返回 429 JSON 响应,不再继续后续过滤器和路由转发。
 */
public class IpRateLimitFilter implements GlobalFilter, Ordered {

    @Autowired
    private RedisRateLimitService redisRateLimitService;

    private final ObjectMapper objectMapper;

    public IpRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 从请求中提取客户端真实 IP
        String ip = getClientIp(exchange.getRequest());

        // 检查是否超过限流阈值,超限则直接拦截
        if (!redisRateLimitService.isAllowed(ip)) {
            log.warn("IP: {} 请求过于频繁，已拦截", ip);
            return writeErrorResponse(exchange.getResponse(), HttpStatus.TOO_MANY_REQUESTS,
                    new ApiResponse<>(429, "请求过于频繁，请稍后再试", null));
        }

        return chain.filter(exchange);
    }

    /**
     * 从请求头中提取客户端真实 IP。
     * 优先取 X-Forwarded-For(经过代理时携带原始 IP),
     * 其次 X-Real-IP,最后从 RemoteAddress 直取。
     * X-Forwarded-For 可能包含逗号分隔的多个 IP,只取第一个(原始客户端)。
     */
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
        // X-Forwarded-For 格式: "client, proxy1, proxy2" 取第一个
        return ip.split(",")[0].trim();
    }

    /**
     * 将限流拒绝的 ApiResponse 序列化为 JSON 写回客户端。
     * 序列化失败时使用硬编码的 fallback JSON,保证兜底可用。
     */
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
        // 数值越小优先级越高,确保限流在认证之前执行
        return -200;
    }
}
