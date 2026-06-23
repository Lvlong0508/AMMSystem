package com.gzasc.aishopping.gateway.service;

import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * 认证与鉴权服务接口。
 *
 * 由 SaTokenAuthGlobalFilter 调用,定义了完整的认证鉴权流水线:
 * 预检请求识别 → 白名单过滤 → Token 校验 → 账户类型识别 → 权限检查。
 * 实现类 AuthServiceImpl 基于 Sa-Token 框架实现。
 */
public interface AuthService {

    /** 判断是否为 OPTIONS 预检请求(浏览器跨域用,直接放行) */
    boolean isPreFlightRequest(ServerHttpRequest request);

    /** 判断请求路径是否在认证白名单中(登录/注册等无需 Token) */
    boolean isWhiteList(String path);

    /** 校验 Token 有效性,返回登录用户 ID;无效则抛 GatewayAuthException */
    String validateToken(String token);

    /** 获取 Token 对应的账户类型(USER / MERCHANT) */
    String getAccountType(String token);

    /** 按账户类型 + 请求路径判断是否有访问权限 */
    boolean hasPermission(String accountType, String path, ServerHttpRequest request);
}
