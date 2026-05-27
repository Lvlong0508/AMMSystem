package com.gzasc.aishopping.gateway.service.impl;

import com.gzasc.aishopping.gateway.config.AuthWhitelistProperties;
import com.gzasc.aishopping.gateway.exception.GatewayAuthException;
import com.gzasc.aishopping.gateway.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final StringRedisTemplate stringRedisTemplate;
    private final AuthWhitelistProperties whitelistProperties;

    public AuthServiceImpl(StringRedisTemplate stringRedisTemplate,
                           AuthWhitelistProperties whitelistProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
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
        String tokenKey = "satoken:login:token:" + token;
        String loginId = stringRedisTemplate.opsForValue().get(tokenKey);
        if (loginId == null || loginId.isEmpty()) {
            log.warn("Token无效或已过期: {}", token);
            throw new GatewayAuthException(401, "登录已过期，请重新登录");
        }
        log.debug("用户 {} 认证通过", loginId);
        return loginId;
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
    public boolean hasPermission(String loginId, String path, ServerHttpRequest request) {
        if (path.startsWith("/api/user/")) {
            return loginId.startsWith("USER:");
        }
        if (path.startsWith("/api/seller/")) {
            if (!loginId.startsWith("MERCHANT:")) {
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
