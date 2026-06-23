package com.gzasc.aishopping.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.gateway.exception.GatewayAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Order(-1)
@Component
/**
 * 网关层全局异常处理器。
 *
 * 当过滤器(SaTokenAuthGlobalFilter 等)抛出异常或路由转发失败时,
 * 统一将异常转为标准的 JSON 错误响应。
 *
 * 处理优先级:
 *   1. GatewayAuthException     → 401/403 + 自定义 message
 *   2. ResponseStatusException  → 透传 HTTP 状态码
 *   3. 其他未预期异常           → 500 + 通用错误信息
 */
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 认证/鉴权异常: 用异常自身的 code 作为 HTTP 状态码和业务码
        if (ex instanceof GatewayAuthException e) {
            return writeResponse(response,
                    HttpStatus.resolve(e.getCode()) != null
                            ? HttpStatus.valueOf(e.getCode())
                            : HttpStatus.BAD_REQUEST,
                    new ApiResponse<>(e.getCode(), e.getMessage(), null));
        }

        // Spring 框架抛出的状态异常(如路由 404)
        if (ex instanceof ResponseStatusException e) {
            return writeResponse(response, HttpStatus.valueOf(e.getStatusCode().value()),
                    new ApiResponse<>(e.getStatusCode().value(), e.getReason(), null));
        }

        // 未预期异常: 记录完整堆栈,返回通用 500
        log.error("Unhandled gateway error", ex);
        return writeResponse(response, HttpStatus.INTERNAL_SERVER_ERROR,
                new ApiResponse<>(500, "系统错误，请稍后重试", null));
    }

    /**
     * 将 ApiResponse 序列化为 JSON 写入响应。
     * ObjectMapper 序列化失败时使用硬编码 fallback,确保兜底可用。
     */
    private Mono<Void> writeResponse(ServerHttpResponse response, HttpStatus status, ApiResponse<?> body) {
        response.setStatusCode(status);
        try {
            byte[] bytes = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response", e);
            String fallback = "{\"code\":500,\"message\":\"系统错误\"}";
            DataBuffer buffer = response.bufferFactory().wrap(fallback.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }
}
