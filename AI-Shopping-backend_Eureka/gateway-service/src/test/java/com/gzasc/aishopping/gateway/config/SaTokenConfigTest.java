package com.gzasc.aishopping.gateway.config;

import cn.dev33.satoken.config.SaTokenConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SaToken配置测试
 */
@SpringBootTest
public class SaTokenConfigTest {

    @Autowired
    private SaTokenConfig saTokenConfig;

    @Test
    void testSaTokenConfiguration() {
        assertNotNull(saTokenConfig);

        // 验证token名称
        assertEquals("satoken", saTokenConfig.getTokenName(),
                "Token名称应为satoken");

        // 验证超时时间 (30天 = 2592000秒)
        assertEquals(2592000L, saTokenConfig.getTimeout(),
                "Token超时时间应为30天");

        // 验证header模式
        assertFalse(saTokenConfig.getIsCookie(),
                "应禁用Cookie模式");
        assertTrue(saTokenConfig.getIsHeader(),
                "应启用Header模式");
    }
}
