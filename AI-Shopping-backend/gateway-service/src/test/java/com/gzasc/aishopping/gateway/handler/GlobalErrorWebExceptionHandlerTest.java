package com.gzasc.aishopping.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.gateway.exception.GatewayAuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalErrorWebExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GlobalErrorWebExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalErrorWebExceptionHandler(objectMapper);
    }

    @Test
    @DisplayName("GW-EX-001: GatewayAuthException 401 返回对应JSON")
    void handleGatewayAuthException_401() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/user/order/list").build()
        );
        GatewayAuthException ex = new GatewayAuthException(401, "未登录");

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        ServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getContentType().includes(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GW-EX-002: GatewayAuthException 403 返回对应JSON")
    void handleGatewayAuthException_403() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/seller/product/list").build()
        );
        GatewayAuthException ex = new GatewayAuthException(403, "无权限访问该资源");

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("GW-EX-003: ResponseStatusException 404")
    void handleResponseStatusException_404() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/user/nonexistent").build()
        );
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("GW-EX-004: 未知异常返回500")
    void handleUnknownException_returns500() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test").build()
        );
        RuntimeException ex = new NullPointerException("模拟空指针");

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("GW-EX-005: GatewayAuthException自定义code=429")
    void handleGatewayAuthException_customCode_429() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test").build()
        );
        GatewayAuthException ex = new GatewayAuthException(429, "自定义消息");

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertEquals(429, exchange.getResponse().getStatusCode().value());
    }

    @Test
    @DisplayName("GW-EX-006: JSON序列化异常回退")
    void handleException_jsonProcessingFallback() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("mock error") {});

        GlobalErrorWebExceptionHandler handlerWithMockMapper =
                new GlobalErrorWebExceptionHandler(mockMapper);

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test").build()
        );
        GatewayAuthException ex = new GatewayAuthException(401, "未登录");

        StepVerifier.create(handlerWithMockMapper.handle(exchange, ex))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
}
