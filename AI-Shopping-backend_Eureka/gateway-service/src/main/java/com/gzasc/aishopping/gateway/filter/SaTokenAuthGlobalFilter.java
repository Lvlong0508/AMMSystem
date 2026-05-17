package com.gzasc.aishopping.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * SaToken认证全局过滤器
 * 使用Spring Cloud Gateway的GlobalFilter实现
 */
@Slf4j
@Component
public class SaTokenAuthGlobalFilter implements GlobalFilter, Ordered {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${auth.whitelist.paths:}")
    private List<String> whitelistPaths;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 放行 CORS 预检请求
        if (request.getMethod() == org.springframework.http.HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // 检查是否是白名单路径
        if (isWhiteList(path)) {
            log.debug("路径 {} 在白名单中，放行", path);
            return chain.filter(exchange);
        }

        // 获取Token
        String token = request.getHeaders().getFirst("satoken");
        if (token == null) {
            log.warn("请求 {} 未携带satoken", path);
            return unauthorized(exchange.getResponse(), "未登录，请重新登录");
        }

        // 验证Token - 直接查询Redis，绕过Sa-Token上下文问题
        try {
            // Sa-Token存储的key格式: satoken:login:token:{token值}
            String tokenKey = "satoken:login:token:" + token;
            
            // 使用StringRedisTemplate直接读取字符串
            String loginId = stringRedisTemplate.opsForValue().get(tokenKey);
            
            if (loginId == null || loginId.isEmpty()) {
                log.warn("Token无效或已过期: {}", token);
                return unauthorized(exchange.getResponse(), "登录已过期，请重新登录");
            }
            
            log.debug("用户 {} 认证通过", loginId);

            // 角色校验：检查用户是否有权限访问该路径
            if (!hasPermission(loginId, path, request)) {
                log.warn("用户 {} 无权限访问路径 {}", loginId, path);
                return forbidden(exchange.getResponse(), "无权限访问该资源");
            }

            // 将userId和satoken添加到请求头，传递给下游服务
            ServerHttpRequest newRequest = request.mutate()
                    .header("userId", loginId)
                    .header("satoken", token)
                    .build();

            return chain.filter(exchange.mutate().request(newRequest).build());

        } catch (Exception e) {
            System.out.println("[GATEWAY DEBUG] 认证异常: " + e.getMessage());
            e.printStackTrace();
            log.error("认证异常: {}", e.getMessage(), e);
            return unauthorized(exchange.getResponse(), "认证失败: " + e.getMessage());
        }
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhiteList(String path) {
        List<String> whiteList = getWhiteList();
        for (String pattern : whiteList) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取白名单配置
     */
    private List<String> getWhiteList() {
        if (whitelistPaths == null || whitelistPaths.isEmpty()) {
            return Arrays.asList(
                    "/api/user/auth/login",
                    "/api/user/auth/register",
                    "/api/seller/auth/login",
                    "/api/seller/auth/register",
                    "/api/user/auth/check-username",
                    "/api/user/auth/check-phone",
                    "/api/seller/auth/check-username",
                    "/api/seller/auth/check-phone"
            );
        }
        return whitelistPaths;
    }

    /**
     * 检查用户是否有权限访问路径
     * loginId格式: USER:xxx 或 MERCHANT:xxx
     */
    private boolean hasPermission(String loginId, String path, ServerHttpRequest request) {
        // 用户只能访问 /api/user/** 路径
        if (path.startsWith("/api/user/")) {
            return loginId.startsWith("USER:");
        }
        // 商家只能访问 /api/seller/** 路径
        if (path.startsWith("/api/seller/")) {
            if (!loginId.startsWith("MERCHANT:")) {
                return false;
            }
            // 检查是否是店长专属API
            if (isShopOwnerOnlyApi(path)) {
                return checkShopOwnerPermission(request);
            }
            return true;
        }
        // 其他路径默认允许（如果有其他类型接口）
        return true;
    }

    /**
     * 判断是否是店长专属API
     * 查询类接口：店铺成员可访问
     * 管理类接口：仅店长可访问，但发货接口店员可访问
     */
    private boolean isShopOwnerOnlyApi(String path) {
        // 查询类接口：店铺成员（店员/店长）可访问
        if (pathMatcher.match("/api/seller/shop/query/**", path)) {
            return false;
        }
        // 管理类接口中的发货接口：店铺成员可访问
        if (pathMatcher.match("/api/seller/shop/manage/**/ship", path)) {
            return false;
        }
        // 其他管理类接口：仅店长可访问
        return pathMatcher.match("/api/seller/shop/manage/**", path) ||
               pathMatcher.match("/api/seller/shop/register", path) ||
               pathMatcher.match("/api/seller/shop/*/products/*", path) ||
               pathMatcher.match("/api/seller/shop/*/employees/register", path) ||
               pathMatcher.match("/api/seller/shop/*/employees/*", path);
    }

    /**
     * 检查店长权限
     */
    private boolean checkShopOwnerPermission(ServerHttpRequest request) {
        String role = request.getHeaders().getFirst("X-Merchant-Role");
        String shopId = request.getHeaders().getFirst("X-Shop-Id");

        // 需要role=1（店长）和shopId
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

    /**
     * 返回401未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":401,\"message\":\"%s\"}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 返回403禁止访问响应
     */
    private Mono<Void> forbidden(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":403,\"message\":\"%s\"}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 在IP限流之后执行，在路由之前执行
        return -100;
    }
}
