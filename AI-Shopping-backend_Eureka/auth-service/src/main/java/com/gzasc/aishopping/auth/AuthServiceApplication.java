package com.gzasc.aishopping.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.gzasc.aishopping.common.feign")
@MapperScan("com.gzasc.aishopping.auth.mapper")
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
        System.out.println("认证服务启动成功：http://localhost:8086");
        System.out.println("功能：用户/商家注册登录（Sa-Token + BCrypt加盐加密）");
    }
}
