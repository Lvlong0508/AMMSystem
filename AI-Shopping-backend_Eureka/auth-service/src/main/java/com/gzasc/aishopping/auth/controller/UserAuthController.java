package com.gzasc.aishopping.auth.controller;

import com.gzasc.aishopping.auth.model.User;
import com.gzasc.aishopping.auth.model.dto.LoginRequest;
import com.gzasc.aishopping.auth.model.dto.LoginResult;
import com.gzasc.aishopping.auth.model.dto.RegisterRequest;
import com.gzasc.aishopping.auth.service.UserAuthService;
import com.gzasc.aishopping.auth.service.UserInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;
    private final UserInfoService userInfoService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody @Valid RegisterRequest request,
                                        BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Map.of("code", 400, "message", "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        LoginResult result = userAuthService.register(request);
        return Map.of(
                "code", 200,
                "message", "注册成功",
                "data", Map.of(
                        "token", result.getToken(),
                        "accountType", result.getAccountType(),
                        "userInfo", buildUserInfo((User) result.getAccount())
                )
        );
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody @Valid LoginRequest request,
                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Map.of("code", 400, "message", "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        LoginResult result = userAuthService.login(request.getUsername(), request.getPassword());
        return Map.of(
                "code", 200,
                "message", "登录成功",
                "data", Map.of(
                        "token", result.getToken(),
                        "accountType", result.getAccountType(),
                        "userInfo", buildUserInfo((User) result.getAccount())
                )
        );
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        userAuthService.logout();
        return Map.of("code", 200, "message", "登出成功");
    }

    @GetMapping("/check-username")
    public Map<String, Object> checkUsername(@RequestParam String username) {
        boolean exists = userAuthService.existsByUsername(username);
        return Map.of(
                "code", 200,
                "message", exists ? "用户名已被使用" : "用户名可用",
                "data", Map.of("available", !exists)
        );
    }

    @GetMapping("/check-phone")
    public Map<String, Object> checkPhone(@RequestParam String phone) {
        boolean exists = userAuthService.existsByPhone(phone);
        return Map.of(
                "code", 200,
                "message", exists ? "手机号已被注册" : "手机号可用",
                "data", Map.of("available", !exists)
        );
    }

    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("phone", user.getPhone());
        info.put("email", user.getEmail());
        info.put("infoId", user.getInfoId());
        info.put("status", user.getStatus());
        if (user.getInfoId() != null) {
            var userInfo = userInfoService.getUserInfoById(user.getInfoId());
            if (userInfo != null) {
                info.put("nickname", userInfo.getNickname());
                info.put("avatar", userInfo.getAvatar());
            }
        }
        return info;
    }
}