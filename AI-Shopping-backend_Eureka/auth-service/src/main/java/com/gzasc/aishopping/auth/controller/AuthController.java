package com.gzasc.aishopping.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.model.User;
import com.gzasc.aishopping.auth.model.dto.LoginRequest;
import com.gzasc.aishopping.auth.model.dto.LoginResult;
import com.gzasc.aishopping.auth.model.dto.RegisterRequest;
import com.gzasc.aishopping.auth.service.AuthService;
import com.gzasc.aishopping.auth.service.impl.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 * 
 * 接口设计原则：用户端和商家端接口完全分开，便于权限管理和维护
 * 
 * 用户端接口: /api/auth/user/**
 * 商家端接口: /api/auth/merchant/**
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ==================== 用户端接口 ====================

    /**
     * 用户注册
     * POST /api/user/auth/register
     */
    @PostMapping("/api/user/auth/register")
    public Map<String, Object> userRegister(@RequestBody @Valid RegisterRequest request, 
                                               BindingResult bindingResult) {
        // 参数校验
        if (bindingResult.hasErrors()) {
            return Map.of("message", "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        try {
            LoginResult result = authService.userRegister(request);
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

    /**
     * 用户登录
     * POST /api/user/auth/login
     */
    @PostMapping("/api/user/auth/login")
    public Map<String, Object> userLogin(@RequestBody @Valid LoginRequest request,
                                          BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Map.of("message", "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        try {
            LoginResult result = authService.userLogin(request.getUsername(), request.getPassword());
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

    /**
     * 用户登出
     * POST /api/user/auth/logout
     */
    @PostMapping("/api/user/auth/logout")
    public Map<String, String> userLogout() {
        try {
            authService.logout();
            return Map.of("message", "登出成功");
        } catch (Exception e) {
            return Map.of("message", "登出错误：" + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     * GET /api/user/auth/info
     */
    @GetMapping("/api/user/auth/info")
    public Map<String, Object> getUserInfo() {
        try {
            // 检查是否登录且为用户类型
            if (!StpUtil.isLogin()) {
                return Map.of("message", "未登录", "code", 401);
            }

            String loginId = (String) StpUtil.getLoginId();
            if (!loginId.startsWith("USER:")) {
                return Map.of("message", "非用户账号", "code", 403);
            }

            Integer userId = Integer.parseInt(loginId.replace("USER:", ""));
            User user = authService.getUserById(userId);

            if (user == null) {
                return Map.of("message", "用户不存在", "code", 404);
            }

            return Map.of(
                "message", "查询成功",
                "userInfo", buildUserInfo(user)
            );
        } catch (Exception e) {
            // Sa-Token 上下文异常视为未登录
            if (e.getMessage() != null && e.getMessage().contains("上下文")) {
                return Map.of("message", "未登录", "code", 401);
            }
            return Map.of("message", "查询错误：" + e.getMessage());
        }
    }

    /**
     * 检查用户用户名是否存在
     * GET /api/user/auth/check-username?username=xxx
     */
    @GetMapping("/api/user/auth/check-username")
    public Map<String, Object> checkUserUsername(@RequestParam String username) {
        boolean exists = authService.userExistsByUsername(username);
        return Map.of(
            "available", !exists,
            "message", exists ? "用户名已被使用" : "用户名可用"
        );
    }

    /**
     * 检查用户手机号是否存在
     * GET /api/user/auth/check-phone?phone=xxx
     */
    @GetMapping("/api/user/auth/check-phone")
    public Map<String, Object> checkUserPhone(@RequestParam String phone) {
        boolean exists = authService.userExistsByPhone(phone);
        return Map.of(
            "available", !exists,
            "message", exists ? "手机号已被注册" : "手机号可用"
        );
    }

    // ==================== 商家端接口 ====================

    /**
     * 商家注册
     * POST /api/seller/auth/register
     */
    @PostMapping("/api/seller/auth/register")
    public Map<String, Object> merchantRegister(@RequestBody @Valid RegisterRequest request,
                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Map.of("message", "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        try {
            LoginResult result = authService.merchantRegister(request);
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

    /**
     * 商家登录
     * POST /api/seller/auth/login
     */
    @PostMapping("/api/seller/auth/login")
    public Map<String, Object> merchantLogin(@RequestBody @Valid LoginRequest request,
                                              BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Map.of("message", "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        try {
            LoginResult result = authService.merchantLogin(request.getUsername(), request.getPassword());
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

    /**
     * 商家登出
     * POST /api/seller/auth/logout
     */
    @PostMapping("/api/seller/auth/logout")
    public Map<String, String> merchantLogout() {
        try {
            authService.logout();
            return Map.of("message", "登出成功");
        } catch (Exception e) {
            return Map.of("message", "登出错误：" + e.getMessage());
        }
    }

    /**
     * 获取当前登录商家信息
     * GET /api/seller/auth/info
     */
    @GetMapping("/api/seller/auth/info")
    public Map<String, Object> getMerchantInfo() {
        try {
            // 检查是否登录且为商家类型
            if (!StpUtil.isLogin()) {
                return Map.of("message", "未登录", "code", 401);
            }

            String loginId = (String) StpUtil.getLoginId();
            if (!loginId.startsWith("MERCHANT:")) {
                return Map.of("message", "非商家账号", "code", 403);
            }

            Integer merchantId = Integer.parseInt(loginId.replace("MERCHANT:", ""));
            Merchant merchant = authService.getMerchantById(merchantId);

            if (merchant == null) {
                return Map.of("message", "商家不存在", "code", 404);
            }

            return Map.of(
                "message", "查询成功",
                "merchantInfo", buildMerchantInfo(merchant)
            );
        } catch (Exception e) {
            // Sa-Token 上下文异常视为未登录
            if (e.getMessage() != null && e.getMessage().contains("上下文")) {
                return Map.of("message", "未登录", "code", 401);
            }
            return Map.of("message", "查询错误：" + e.getMessage());
        }
    }

    /**
     * 检查商家用户名是否存在
     * GET /api/seller/auth/check-username?username=xxx
     */
    @GetMapping("/api/seller/auth/check-username")
    public Map<String, Object> checkMerchantUsername(@RequestParam String username) {
        boolean exists = authService.merchantExistsByUsername(username);
        return Map.of(
            "available", !exists,
            "message", exists ? "用户名已被使用" : "用户名可用"
        );
    }

    /**
     * 检查商家手机号是否存在
     * GET /api/seller/auth/check-phone?phone=xxx
     */
    @GetMapping("/api/seller/auth/check-phone")
    public Map<String, Object> checkMerchantPhone(@RequestParam String phone) {
        boolean exists = authService.merchantExistsByPhone(phone);
        return Map.of(
            "available", !exists,
            "message", exists ? "手机号已被注册" : "手机号可用"
        );
    }

    // ==================== 工具方法 ====================

    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("phone", user.getPhone());
        info.put("email", user.getEmail());
        info.put("status", user.getStatus());
        return info;
    }

    private Map<String, Object> buildMerchantInfo(Merchant merchant) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", merchant.getId());
        info.put("username", merchant.getUsername());
        info.put("shopName", merchant.getShopName());
        info.put("phone", merchant.getPhone());
        info.put("email", merchant.getEmail());
        info.put("status", merchant.getStatus());
        return info;
    }
}
