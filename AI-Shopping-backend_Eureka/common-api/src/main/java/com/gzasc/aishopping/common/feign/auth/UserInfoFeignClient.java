package com.gzasc.aishopping.common.feign.auth;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "auth-service", contextId = "userInfoFeignClient", path = "/internal/userinfo")
public interface UserInfoFeignClient {

    @PostMapping("/create")
    Map<String, Object> createUserInfo(@RequestBody Map<String, String> request);
}