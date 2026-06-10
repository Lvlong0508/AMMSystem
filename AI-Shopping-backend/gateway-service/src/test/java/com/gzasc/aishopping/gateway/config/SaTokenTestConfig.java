package com.gzasc.aishopping.gateway.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class SaTokenTestConfig {

    @Bean
    @Primary
    public SaTokenDao saTokenDao() {
        return new SaTokenDaoDefaultImpl();
    }

    @Bean
    public Object fixSaTokenDao(SaTokenDao saTokenDao) {
        SaManager.setSaTokenDao(saTokenDao);
        return new Object();
    }
}
