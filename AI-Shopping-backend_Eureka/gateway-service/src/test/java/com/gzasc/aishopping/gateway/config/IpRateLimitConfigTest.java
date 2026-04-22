package com.gzasc.aishopping.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IP限流配置测试
 */
@SpringBootTest
public class IpRateLimitConfigTest {

    @Autowired
    private Environment env;

    @Test
    void testIpRateLimitConfig() {
        String maxRequests = env.getProperty("ip-rate-limit.max-requests");
        String timeWindow = env.getProperty("ip-rate-limit.time-window-seconds");

        assertNotNull(maxRequests, "IP限流最大请求数应配置");
        assertNotNull(timeWindow, "IP限流时间窗口应配置");

        assertEquals("30", maxRequests, "默认最大请求数应为30");
        assertEquals("60", timeWindow, "默认时间窗口应为60秒");
    }
}
