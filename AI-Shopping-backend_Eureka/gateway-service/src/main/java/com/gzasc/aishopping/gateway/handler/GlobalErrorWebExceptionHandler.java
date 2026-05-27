package com.gzasc.aishopping.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.gateway.exception.GatewayAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Order(-1)
@Component
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        if (ex instanceof GatewayAuthException e) {
            return writeResponse(response,
                    HttpStatus.resolve(e.getCode()) != null
                            ? HttpStatus.valueOf(e.getCode())
                            : HttpStatus.BAD_REQUEST,
                    new ApiResponse<>(e.getCode(), e.getMessage(), null));
        }

        if (ex instanceof ResponseStatusException e) {
            return writeResponse(response, (HttpStatus) e.getStatusCode(),
                    new ApiResponse<>(e.getStatusCode().value(), e.getReason(), null));
        }

        log.error("Unhandled gateway error", ex);
        return writeResponse(response, HttpStatus.INTERNAL_SERVER_ERROR,
                new ApiResponse<>(500, "系统错误，请稍后重试", null));
    }

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
