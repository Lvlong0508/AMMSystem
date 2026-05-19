package com.gzasc.aishopping.auth.controller;

import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.model.dto.LoginRequest;
import com.gzasc.aishopping.auth.model.dto.LoginResult;
import com.gzasc.aishopping.auth.model.dto.RegisterRequest;
import com.gzasc.aishopping.auth.service.MerchantAuthService;
import com.gzasc.aishopping.auth.service.MerchantInfoService;
import com.gzasc.aishopping.auth.service.impl.AuthException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/seller/auth")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final MerchantAuthService merchantAuthService;
    private final MerchantInfoService merchantInfoService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody @Valid RegisterRequest request,
                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Map.of("message", "参数错误：" + Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage());
        }

        try {
            LoginResult result = merchantAuthService.register(request);
            return Map.of(
                    "message", "注册成功",
                    "token", result.getToken(),
                    "merchantInfo", buildMerchantInfo(result.getMerchant())
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
            return Map.of("message", "参数错误：" + Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage());
        }

        try {
            LoginResult result = merchantAuthService.login(request.getUsername(), request.getPassword());
            return Map.of(
                    "message", "登录成功",
                    "token", result.getToken(),
                    "merchantInfo", buildMerchantInfo(result.getMerchant())
            );
        } catch (AuthException e) {
            return Map.of("message", "登录失败：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("message", "登录错误：" + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Map<String, String> logout() {
        try {
            merchantAuthService.logout();
            return Map.of("message", "登出成功");
        } catch (Exception e) {
            return Map.of("message", "登出错误：" + e.getMessage());
        }
    }

    @GetMapping("/check-username")
    public Map<String, Object> checkUsername(@RequestParam String username) {
        boolean exists = merchantAuthService.existsByUsername(username);
        return Map.of(
                "available", !exists,
                "message", exists ? "用户名已被使用" : "用户名可用"
        );
    }

    @GetMapping("/check-phone")
    public Map<String, Object> checkPhone(@RequestParam String phone) {
        boolean exists = merchantAuthService.existsByPhone(phone);
        return Map.of(
                "available", !exists,
                "message", exists ? "手机号已被注册" : "手机号可用"
        );
    }

    private Map<String, Object> buildMerchantInfo(Merchant merchant) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", merchant.getId());
        info.put("username", merchant.getUsername());
        info.put("phone", merchant.getPhone());
        info.put("email", merchant.getEmail());
        info.put("infoId", merchant.getInfoId());
        info.put("status", merchant.getStatus());
        if (merchant.getInfoId() != null) {
            var merchantInfo = merchantInfoService.getMerchantInfoById(merchant.getInfoId());
            if (merchantInfo != null) {
                info.put("nickname", merchantInfo.getNickname());
                info.put("avatar", merchantInfo.getAvatar());
            }
        }
        return info;
    }
}