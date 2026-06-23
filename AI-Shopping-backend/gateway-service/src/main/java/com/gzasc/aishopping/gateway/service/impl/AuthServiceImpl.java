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

/**
 * AuthService 实现: 基于 Sa-Token 框架的认证与鉴权。
 *
 * 核心流程:
 *   1. 预检请求识别(OPTIONS → 放行)
 *   2. 白名单匹配(登录/注册 → 跳过认证)
 *   3. Token 校验(StpUtil.getLoginIdByToken → 提取用户 ID)
 *   4. 账户类型识别(从 SaSession 读取 accountType)
 *   5. 权限判断(USER → /api/user/**, MERCHANT → /api/seller/**)
 *   6. 店长专属 API 额外校验(预留)
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    /** Spring Ant 路径匹配器,支持通配符匹配白名单路径 */
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
        // 遍历白名单列表,使用 AntPathMatcher 做通配匹配
        return whitelistProperties.getPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    public String validateToken(String token) {
        // Token 不存在 → 未登录
        if (token == null || token.isEmpty()) {
            throw new GatewayAuthException(401, "未登录");
        }
        try {
            // 由 Sa-Token 根据 Token 解析出登录用户 ID
            String loginId = (String) StpUtil.getLoginIdByToken(token);
            if (loginId == null || loginId.isEmpty()) {
                log.warn("Token无效或已过期: {}", token);
                throw new GatewayAuthException(401, "登录已过期，请重新登录");
            }
            log.debug("用户 {} 认证通过", loginId);
            return loginId;
        } catch (SaTokenException e) {
            // Sa-Token 内部异常(Token 过期/伪造/签名错误)统一包装
            log.warn("Token无效或已过期: {}", token);
            throw new GatewayAuthException(401, "登录已过期，请重新登录");
        }
    }

    @Override
    public String getAccountType(String token) {
        try {
            // 从 Sa-Token 的 Session 中获取登录时写入的 accountType 属性
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
        // 用户端 API: 要求账户类型为 USER
        if (path.startsWith("/api/user/")) {
            return "USER".equals(accountType);
        }
        // 商家端 API: 要求账户类型为 MERCHANT,店长专属 API 额外校验
        if (path.startsWith("/api/seller/")) {
            if (!"MERCHANT".equals(accountType)) {
                return false;
            }
            if (isShopOwnerOnlyApi(path)) {
                return checkShopOwnerPermission(request);
            }
            return true;
        }
        // 其他路径(如公共接口)默认放行
        return true;
    }

    /** 判断是否为店长专属 API(预留扩展点,当前始终返回 false) */
    private boolean isShopOwnerOnlyApi(String path) {
        return false;
    }

    /**
     * 校验店长权限: 检查请求头中的店长角色标识和店铺 ID。
     * X-Merchant-Role=1 表示店长, X-Shop-Id 指定操作的店铺。
     */
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
