package com.gzasc.aishopping.auth.controller;

import com.gzasc.aishopping.auth.converter.AuthConverter;
import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.model.MerchantInfo;
import com.gzasc.aishopping.auth.dto.LoginRequest;
import com.gzasc.aishopping.auth.dto.LoginResult;
import com.gzasc.aishopping.auth.dto.RegisterRequest;
import com.gzasc.aishopping.auth.service.MerchantAuthService;
import com.gzasc.aishopping.auth.service.MerchantInfoService;
import com.gzasc.aishopping.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/seller/auth")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final MerchantAuthService merchantAuthService;
    private final MerchantInfoService merchantInfoService;
    private final AuthConverter authConverter;

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody @Valid RegisterRequest request) {
        LoginResult result = merchantAuthService.register(request);
        Merchant merchant = (Merchant) result.getAccount();
        MerchantInfo merchantInfo = merchant.getInfoId() != null ? merchantInfoService.getMerchantInfoById(merchant.getInfoId()) : null;
        return ApiResponse.success("注册成功", Map.of(
                "token", result.getToken(),
                "accountType", result.getAccountType(),
                "merchantInfo", authConverter.toMerchantInfoMap(merchant, merchantInfo)
        ));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody @Valid LoginRequest request) {
        LoginResult result = merchantAuthService.login(request.getUsername(), request.getPassword());
        Merchant merchant = (Merchant) result.getAccount();
        MerchantInfo merchantInfo = merchant.getInfoId() != null ? merchantInfoService.getMerchantInfoById(merchant.getInfoId()) : null;
        return ApiResponse.success("登录成功", Map.of(
                "token", result.getToken(),
                "accountType", result.getAccountType(),
                "merchantInfo", authConverter.toMerchantInfoMap(merchant, merchantInfo)
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        merchantAuthService.logout();
        return ApiResponse.success("登出成功", null);
    }

    @GetMapping("/check-username")
    public ApiResponse<Map<String, Object>> checkUsername(@RequestParam String username) {
        boolean exists = merchantAuthService.existsByUsername(username);
        return ApiResponse.success(
                exists ? "用户名已被使用" : "用户名可用",
                Map.of("available", !exists)
        );
    }

    @GetMapping("/check-phone")
    public ApiResponse<Map<String, Object>> checkPhone(@RequestParam String phone) {
        boolean exists = merchantAuthService.existsByPhone(phone);
        return ApiResponse.success(
                exists ? "手机号已被注册" : "手机号可用",
                Map.of("available", !exists)
        );
    }
}