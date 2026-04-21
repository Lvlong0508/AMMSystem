package com.gzasc.aishopping.auth.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 多数据源配置
 * 
 * 主数据源：eureka_user（用户数据）
 * 第二数据源：eureka_merchant（商家数据）
 */
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String userJdbcUrl;

    @Value("${spring.datasource.username}")
    private String userUsername;

    @Value("${spring.datasource.password}")
    private String userPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String userDriverClassName;

    @Value("${spring.second-datasource.jdbc-url}")
    private String merchantJdbcUrl;

    @Value("${spring.second-datasource.username}")
    private String merchantUsername;

    @Value("${spring.second-datasource.password}")
    private String merchantPassword;

    @Value("${spring.second-datasource.driver-class-name}")
    private String merchantDriverClassName;

    /**
     * 主数据源 - 用户库（eureka_user）
     */
    @Bean(name = "userDataSource")
    @Primary
    public DataSource userDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(userJdbcUrl);
        config.setUsername(userUsername);
        config.setPassword(userPassword);
        config.setDriverClassName(userDriverClassName);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1200000);
        config.setConnectionTimeout(20000);
        return new HikariDataSource(config);
    }

    /**
     * 商家数据源 - 商家库（eureka_merchant）
     */
    @Bean(name = "merchantDataSource")
    public DataSource merchantDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(merchantJdbcUrl);
        config.setUsername(merchantUsername);
        config.setPassword(merchantPassword);
        config.setDriverClassName(merchantDriverClassName);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1200000);
        config.setConnectionTimeout(20000);
        return new HikariDataSource(config);
    }
}
