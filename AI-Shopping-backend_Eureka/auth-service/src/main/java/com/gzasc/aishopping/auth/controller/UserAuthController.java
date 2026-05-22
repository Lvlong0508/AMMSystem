package com.gzasc.aishopping.auth.controller;

import com.gzasc.aishopping.auth.converter.AuthConverter;
import com.gzasc.aishopping.auth.model.User;
import com.gzasc.aishopping.auth.model.UserInfo;
import com.gzasc.aishopping.auth.dto.LoginRequest;
import com.gzasc.aishopping.auth.dto.LoginResult;
import com.gzasc.aishopping.auth.dto.RegisterRequest;
import com.gzasc.aishopping.auth.service.UserAuthService;
import com.gzasc.aishopping.auth.service.UserInfoService;
import com.gzasc.aishopping.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;
    private final UserInfoService userInfoService;
    private final AuthConverter authConverter;

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody @Valid RegisterRequest request) {
        LoginResult result = userAuthService.register(request);
        User user = (User) result.getAccount();
        UserInfo userInfo = user.getInfoId() != null ? userInfoService.getUserInfoById(user.getInfoId()) : null;
        return ApiResponse.success("注册成功", Map.of(
                "token", result.getToken(),
                "accountType", result.getAccountType(),
                "userInfo", authConverter.toUserInfoMap(user, userInfo)
        ));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody @Valid LoginRequest request) {
        LoginResult result = userAuthService.login(request.getUsername(), request.getPassword());
        User user = (User) result.getAccount();
        UserInfo userInfo = user.getInfoId() != null ? userInfoService.getUserInfoById(user.getInfoId()) : null;
        return ApiResponse.success("登录成功", Map.of(
                "token", result.getToken(),
                "accountType", result.getAccountType(),
                "userInfo", authConverter.toUserInfoMap(user, userInfo)
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        userAuthService.logout();
        return ApiResponse.success("登出成功", null);
    }

    @GetMapping("/check-username")
    public ApiResponse<Map<String, Object>> checkUsername(@RequestParam String username) {
        boolean exists = userAuthService.existsByUsername(username);
        return ApiResponse.success(
                exists ? "用户名已被使用" : "用户名可用",
                Map.of("available", !exists)
        );
    }

    @GetMapping("/check-phone")
    public ApiResponse<Map<String, Object>> checkPhone(@RequestParam String phone) {
        boolean exists = userAuthService.existsByPhone(phone);
        return ApiResponse.success(
                exists ? "手机号已被注册" : "手机号可用",
                Map.of("available", !exists)
        );
    }
}