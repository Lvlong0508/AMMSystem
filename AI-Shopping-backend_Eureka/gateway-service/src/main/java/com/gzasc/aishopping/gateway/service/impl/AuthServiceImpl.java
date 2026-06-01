package com.gzasc.aishopping.gateway.service.impl;

import com.gzasc.aishopping.gateway.config.AuthWhitelistProperties;
import com.gzasc.aishopping.gateway.exception.GatewayAuthException;
import com.gzasc.aishopping.gateway.service.AuthService;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {



    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final AuthWhitelistProperties whitelistProperties;

    public AuthServiceImpl(AuthWhitelistProperties whitelistProperties) {
        this.whitelistProperties = whitelistProperties;
    }

    @Override
    public boolean isPreFlightRequest(ServerHttpRequest request) {
        return request.getMethod() == HttpMethod.OPTIONS;
    }

    @Override
    public boolean isWhiteList(String path) {
        return whitelistProperties.getPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    public String validateToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new GatewayAuthException(401, "未登录");
        }
        try {
            String loginId = (String) StpUtil.getLoginIdByToken(token);
            if (loginId == null || loginId.isEmpty()) {
                log.warn("Token无效或已过期: {}", token);
                throw new GatewayAuthException(401, "登录已过期，请重新登录");
            }
            log.debug("用户 {} 认证通过", loginId);
            return loginId;
        } catch (SaTokenException e) {
            log.warn("Token无效或已过期: {}", token);
            throw new GatewayAuthException(401, "登录已过期，请重新登录");
        }
    }

    @Override
    public String extractRole(String loginId) {
        if (loginId.startsWith("USER:")) {
            return "USER";
        } else if (loginId.startsWith("MERCHANT:")) {
            return "MERCHANT";
        }
        return "UNKNOWN";
    }

    @Override
    public String getAccountType(String token) {
        try {
            SaSession session = StpUtil.getStpLogic().getTokenSessionByToken(token, false);
            if (session == null) {
                log.warn("Token session不存在: {}", token);
                return null;
            }
            return (String) session.get("accountType");
        } catch (Exception e) {
            log.error("读取token session失败", e);
            return null;
        }
    }

    @Override
    public boolean hasPermission(String accountType, String path, ServerHttpRequest request) {
        if (path.startsWith("/api/user/")) {
            return "USER".equals(accountType);
        }
        if (path.startsWith("/api/seller/")) {
            if (!"MERCHANT".equals(accountType)) {
                return false;
            }
            if (isShopOwnerOnlyApi(path)) {
                return checkShopOwnerPermission(request);
            }
            return true;
        }
        return true;
    }

    private boolean isShopOwnerOnlyApi(String path) {
        if (pathMatcher.match("/api/seller/shop/query/**", path)
                || pathMatcher.match("/api/seller/shop/manage/**/ship", path)) {
            return false;
        }
        return pathMatcher.match("/api/seller/shop/manage/**", path)
                || pathMatcher.match("/api/seller/shop/register", path)
                || pathMatcher.match("/api/seller/shop/*/products/*", path)
                || pathMatcher.match("/api/seller/shop/*/employees/register", path)
                || pathMatcher.match("/api/seller/shop/*/employees/*", path);
    }

    private boolean checkShopOwnerPermission(ServerHttpRequest request) {
        String role = request.getHeaders().getFirst("X-Merchant-Role");
        String shopId = request.getHeaders().getFirst("X-Shop-Id");
        if (role == null || !role.equals("1")) {
            log.warn("非店长无法访问此API");
            return false;
        }
        if (shopId == null || shopId.isEmpty()) {
            log.warn("未指定店铺ID");
            return false;
        }
        return true;
    }
}
