package com.gzasc.aishopping.auth.controller;

import com.gzasc.aishopping.auth.model.User;
import com.gzasc.aishopping.auth.model.dto.LoginRequest;
import com.gzasc.aishopping.auth.model.dto.LoginResult;
import com.gzasc.aishopping.auth.model.dto.RegisterRequest;
import com.gzasc.aishopping.auth.service.UserAuthService;
import com.gzasc.aishopping.auth.service.UserInfoService;
import com.gzasc.aishopping.auth.service.impl.AuthException;
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
            return Map.of("message", "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        try {
            LoginResult result = userAuthService.register(request);
            return Map.of(
                    "message", "注册成功",
                    "token", result.getToken(),
                    "userInfo", buildUserInfo(result.getUser())
            );
        } catch (AuthException e) {
            return Map.of("message", "注册失败：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("message", "注册错误：" + e.getMessage());
        }
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody @Valid LoginRequest request,
                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Map.of("message", "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        try {
            LoginResult result = userAuthService.login(request.getUsername(), request.getPassword());
            return Map.of(
                    "message", "登录成功",
                    "token", result.getToken(),
                    "userInfo", buildUserInfo(result.getUser())
            );
        } catch (AuthException e) {
            return Map.of("message", "登录失败：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return Map.of("message", "登录错误：" + errorMsg);
        }
    }

    @PostMapping("/logout")
    public Map<String, String> logout() {
        try {
            userAuthService.logout();
            return Map.of("message", "登出成功");
        } catch (Exception e) {
            return Map.of("message", "登出错误：" + e.getMessage());
        }
    }

    @GetMapping("/check-username")
    public Map<String, Object> checkUsername(@RequestParam String username) {
        boolean exists = userAuthService.existsByUsername(username);
        return Map.of(
                "available", !exists,
                "message", exists ? "用户名已被使用" : "用户名可用"
        );
    }

    @GetMapping("/check-phone")
    public Map<String, Object> checkPhone(@RequestParam String phone) {
        boolean exists = userAuthService.existsByPhone(phone);
        return Map.of(
                "available", !exists,
                "message", exists ? "手机号已被注册" : "手机号可用"
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