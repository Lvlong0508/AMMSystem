package com.gzasc.aishopping.common.feign.auth;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 认证服务 Feign 客户端
 * 供其他服务调用认证相关接口
 */
@FeignClient(name = "auth-service")
public interface AuthFeignClient {

    /**
     * 注册店员
     */
    @PostMapping("/internal/auth/register-employee")
    Map<String, Object> registerEmployee(@RequestBody Map<String, Object> request);
}