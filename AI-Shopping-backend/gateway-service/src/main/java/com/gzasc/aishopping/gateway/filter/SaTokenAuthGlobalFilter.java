package com.gzasc.aishopping.gateway.filter;

import com.gzasc.aishopping.gateway.exception.GatewayAuthException;
import com.gzasc.aishopping.gateway.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
/**
 * Sa-Token 认证与鉴权全局过滤器。
 *
 * 在限流之后执行(Order=-100),对每个非白名单、非预检请求依次做:
 *   1. Token 有效性校验(是否存在、是否过期、是否伪造)
 *   2. 账户类型识别(USER / MERCHANT)
 *   3. 请求路径的权限检查(用户只能访问 /api/user/**,商家只能访问 /api/seller/**)
 * 认证通过后在请求头注入 X-User-Id(用户 ID)和 satoken(Token),
 * 下游微服务通过请求头直接获取当前用户信息。
 */
public class SaTokenAuthGlobalFilter implements GlobalFilter, Ordered {

    private final AuthService authService;

    public SaTokenAuthGlobalFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. OPTIONS 预检请求直接放行(浏览器跨域时需要)
        if (authService.isPreFlightRequest(request)) {
            return chain.filter(exchange);
        }

        // 2. 白名单路径(登录/注册等)跳过认证
        if (authService.isWhiteList(path)) {
            log.debug("WhiteList path: {}, skip auth", path);
            return chain.filter(exchange);
        }

        // 3. 从请求头提取 Token 并校验有效性
        String token = request.getHeaders().getFirst("satoken");
        String loginId = authService.validateToken(token);

        // 4. 获取账户类型(USER/MERCHANT),判断会话是否存在
        String accountType = authService.getAccountType(token);
        if (accountType == null) {
            log.warn("Token session 已过期: {}", loginId);
            throw new GatewayAuthException(401, "登录已过期，请重新登录");
        }

        // 5. 按账户类型 + 请求路径做权限检查
        if (!authService.hasPermission(accountType, path, request)) {
            log.warn("{} 无权限访问路径 {}", loginId, path);
            throw new GatewayAuthException(403, "无权限访问该资源");
        }

        // 6. 认证通过,将用户 ID 和 Token 注入请求头,下游服务可直接取用
        ServerHttpRequest newRequest = request.mutate()
                .header("X-User-Id", loginId)
                .header("satoken", token)
                .build();

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    @Override
    public int getOrder() {
        // 在限流之后(限流 -200)、在路由转发之前执行
        return -100;
    }
}
