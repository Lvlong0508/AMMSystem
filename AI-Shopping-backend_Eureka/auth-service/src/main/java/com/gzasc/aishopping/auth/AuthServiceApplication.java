package com.gzasc.aishopping.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
        System.out.println("认证服务启动成功：http://localhost:8086");
        System.out.println("功能：用户/商家注册登录（Sa-Token + BCrypt加盐加密）");
    }
}
