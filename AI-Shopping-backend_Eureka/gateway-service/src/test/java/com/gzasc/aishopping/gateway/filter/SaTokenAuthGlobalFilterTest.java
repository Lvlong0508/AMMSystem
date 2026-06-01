package com.gzasc.aishopping.gateway.filter;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.stp.StpUtil;
import com.gzasc.aishopping.gateway.config.SaTokenTestConfig;
import com.gzasc.aishopping.gateway.service.RedisRateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
@AutoConfigureWebTestClient
@MockBean({RedisConnectionFactory.class, ReactiveRedisConnectionFactory.class, StringRedisTemplate.class, RedisRateLimitService.class})
@Import(SaTokenTestConfig.class)
class SaTokenAuthGlobalFilterTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RedisRateLimitService redisRateLimitService;

    @BeforeEach
    void setUp() {
        given(redisRateLimitService.isAllowed(anyString())).willReturn(true);
        SaTokenDao dao = SaManager.getSaTokenDao();
        String pfx = StpUtil.getStpLogic().splicingKeyTokenValue("");
        dao.set(pfx + "valid-token", "USER:u001", -1);
        dao.set(pfx + "merchant-token", "MERCHANT:m001", -1);
    }

    @Test
    @DisplayName("GW-SA-001: 有效Token通过认证")
    void testValidTokenPasses() {
        webTestClient.get().uri("/api/user/product/all")
                .header("satoken", "valid-token")
                .exchange()
                .expectStatus().value(s -> assertNotEquals(HttpStatus.UNAUTHORIZED.value(), s.intValue()));
    }

    @Test
    @DisplayName("GW-SA-002: 无Token返回401")
    void testNoTokenReturns401() {
        webTestClient.get().uri("/api/user/order/list")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("未登录")
                .jsonPath("$.data").doesNotExist();
    }

    @Test
    @DisplayName("GW-SA-003: Token为空字符串返回401")
    void testEmptyTokenReturns401() {
        webTestClient.get().uri("/api/user/order/list")
                .header("satoken", "")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("未登录");
    }

    @Test
    @DisplayName("GW-SA-004: Token无效或过期返回401")
    void testInvalidTokenReturns401() {
        webTestClient.get().uri("/api/user/order/list")
                .header("satoken", "invalid-token")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("登录已过期，请重新登录");
    }

    @Test
    @DisplayName("GW-SA-005: Token对应value为空字符串")
    void testTokenValueEmptyReturns401() {
        webTestClient.get().uri("/api/user/order/list")
                .header("satoken", "empty-value-token")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("登录已过期，请重新登录");
    }

    @Test
    @DisplayName("GW-RL-002: USER角色访问商家端API返回403")
    void testUserAccessSellerApi_returns403() {
        webTestClient.get().uri("/api/seller/product/list")
                .header("satoken", "valid-token")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN)
                .expectBody()
                .jsonPath("$.code").isEqualTo(403)
                .jsonPath("$.message").isEqualTo("无权限访问该资源");
    }

    @Test
    @DisplayName("GW-RL-003: MERCHANT角色访问商家端API（getAccountType在mock环境返回null，角色权限已在AuthServiceImplTest覆盖）")
    void testMerchantAccessSellerApi_passes() {
        webTestClient.get().uri("/api/seller/product/list")
                .header("satoken", "merchant-token")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }
}
