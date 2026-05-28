package com.gzasc.aishopping.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.gateway.config.IpRateLimitProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "ip-rate-limit.max-requests=5",
        "ip-rate-limit.time-window-seconds=60"
})
@AutoConfigureWebTestClient
@MockBean({RedisConnectionFactory.class, StringRedisTemplate.class})
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class IpRateLimitFilterTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("GW-IP-001/GW-IP-002: 正常请求与超限拦截")
    void testRateLimitNormalAndThreshold() {
        for (int i = 0; i < 5; i++) {
            webTestClient.get().uri("/api/user/auth/login")
                    .exchange()
                    .expectStatus().value(s -> assertNotEquals(HttpStatus.TOO_MANY_REQUESTS.value(), s.intValue()));
        }
        webTestClient.get().uri("/api/user/auth/login")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectBody()
                .jsonPath("$.code").isEqualTo(429)
                .jsonPath("$.message").isEqualTo("请求过于频繁，请稍后再试")
                .jsonPath("$.data").doesNotExist();
    }

    @Test
    @DisplayName("GW-IP-003: 不同IP独立计数")
    void testRateLimitDifferentIps() {
        for (int i = 0; i < 5; i++) {
            webTestClient.get().uri("/api/user/auth/login")
                    .header("X-Forwarded-For", "10.0.0.1")
                    .exchange()
                    .expectStatus().value(s -> assertNotEquals(HttpStatus.TOO_MANY_REQUESTS.value(), s.intValue()));
        }
        webTestClient.get().uri("/api/user/auth/login")
                .header("X-Forwarded-For", "10.0.0.2")
                .exchange()
                .expectStatus().value(s -> assertNotEquals(HttpStatus.TOO_MANY_REQUESTS.value(), s.intValue()));
        webTestClient.get().uri("/api/user/auth/login")
                .header("X-Forwarded-For", "10.0.0.1")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("GW-IP-005: X-Forwarded-For优先获取IP")
    void testRateLimitXForwardedForPriority() {
        for (int i = 0; i < 5; i++) {
            webTestClient.get().uri("/api/user/auth/login")
                    .header("X-Forwarded-For", "10.0.0.1")
                    .header("X-Real-IP", "10.0.0.2")
                    .exchange()
                    .expectStatus().value(s -> assertNotEquals(HttpStatus.TOO_MANY_REQUESTS.value(), s.intValue()));
        }
        webTestClient.get().uri("/api/user/auth/login")
                .header("X-Forwarded-For", "10.0.0.1")
                .header("X-Real-IP", "10.0.0.2")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("GW-IP-008: X-Forwarded-For多IP取第一个")
    void testRateLimitXForwardedForMultiple() {
        for (int i = 0; i < 5; i++) {
            webTestClient.get().uri("/api/user/auth/login")
                    .header("X-Forwarded-For", "10.0.0.1, 10.0.0.2, 10.0.0.3")
                    .exchange()
                    .expectStatus().value(s -> assertNotEquals(HttpStatus.TOO_MANY_REQUESTS.value(), s.intValue()));
        }
        webTestClient.get().uri("/api/user/auth/login")
                .header("X-Forwarded-For", "10.0.0.1, 10.0.0.2, 10.0.0.3")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("GW-IP-006: X-Real-IP作为第二IP来源")
    void testRateLimitXRealIp() {
        for (int i = 0; i < 5; i++) {
            webTestClient.get().uri("/api/user/auth/login")
                    .header("X-Real-IP", "10.0.0.3")
                    .exchange()
                    .expectStatus().value(s -> assertNotEquals(HttpStatus.TOO_MANY_REQUESTS.value(), s.intValue()));
        }
        webTestClient.get().uri("/api/user/auth/login")
                .header("X-Real-IP", "10.0.0.3")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("GW-IP-007: 无代理Header时使用RemoteAddress")
    void testGetClientIp_noProxyHeader_usesRemoteAddress() {
        IpRateLimitProperties properties = new IpRateLimitProperties();
        properties.setMaxRequests(5);
        properties.setTimeWindowSeconds(60);
        IpRateLimitFilter filter = new IpRateLimitFilter(properties, new ObjectMapper());

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new InetSocketAddress("192.168.1.1", 8080))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    @DisplayName("GW-IP-009: 自定义maxRequests=1生效")
    void testRateLimitCustomMaxRequests() {
        IpRateLimitProperties properties = new IpRateLimitProperties();
        properties.setMaxRequests(1);
        properties.setTimeWindowSeconds(60);
        IpRateLimitFilter filter = new IpRateLimitFilter(properties, new ObjectMapper());

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new InetSocketAddress("10.0.0.99", 8080))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ServerWebExchange exchange2 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("10.0.0.99", 8080))
                        .build()
        );

        StepVerifier.create(filter.filter(exchange2, chain))
                .expectNextCount(0)
                .verifyComplete();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange2.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("GW-IP-010: JSON序列化异常回退")
    void testWriteErrorResponse_jsonProcessingFallback() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("mock error") {});

        IpRateLimitProperties properties = new IpRateLimitProperties();
        properties.setMaxRequests(0);
        properties.setTimeWindowSeconds(60);
        IpRateLimitFilter filter = new IpRateLimitFilter(properties, mockMapper);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new InetSocketAddress("10.0.0.100", 8080))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .expectNextCount(0)
                .verifyComplete();

        ServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
    }
}
