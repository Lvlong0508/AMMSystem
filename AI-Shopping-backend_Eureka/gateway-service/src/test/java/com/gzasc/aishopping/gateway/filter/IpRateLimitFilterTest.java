package com.gzasc.aishopping.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * IP限流过滤器测试
 */
public class IpRateLimitFilterTest {

    private IpRateLimitFilter filter;
    private ServerWebExchange exchange;
    private GatewayFilterChain chain;
    private ServerHttpRequest request;
    private ServerHttpResponse response;

    @BeforeEach
    void setUp() {
        filter = new IpRateLimitFilter();
        filter.init();

        exchange = mock(ServerWebExchange.class);
        chain = mock(GatewayFilterChain.class);
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void testIpRateLimit_AllowRequest() {
        // 模拟IP地址
        when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 12345));
        when(request.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(chain, times(1)).filter(any());
    }

    @Test
    void testIpRateLimit_BlockAfterLimit() {
        // 模拟同一IP频繁请求
        String testIp = "192.168.1.2";
        when(request.getRemoteAddress()).thenReturn(new InetSocketAddress(testIp, 12345));
        when(request.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
        when(response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS)).thenReturn(true);
        when(response.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
        when(response.bufferFactory()).thenReturn(new org.springframework.core.io.buffer.DefaultDataBufferFactory());
        when(response.writeWith(any())).thenReturn(Mono.empty());

        // 发送超过30次请求
        for (int i = 0; i < 30; i++) {
            filter.filter(exchange, chain).block();
        }

        // 第31次请求应该被拦截
        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void testGetOrder() {
        assert filter.getOrder() == -200;
    }
}
