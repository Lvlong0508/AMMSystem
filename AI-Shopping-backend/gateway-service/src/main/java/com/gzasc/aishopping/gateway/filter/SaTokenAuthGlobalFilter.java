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
public class SaTokenAuthGlobalFilter implements GlobalFilter, Ordered {



    private final AuthService authService;

    public SaTokenAuthGlobalFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (authService.isPreFlightRequest(request)) {
            return chain.filter(exchange);
        }

        if (authService.isWhiteList(path)) {
            log.debug("WhiteList path: {}, skip auth", path);
            return chain.filter(exchange);
        }

        String token = request.getHeaders().getFirst("satoken");
        String loginId = authService.validateToken(token);

        String accountType = authService.getAccountType(token);
        if (accountType == null) {
            log.warn("Token session 已过期: {}", loginId);
            throw new GatewayAuthException(401, "登录已过期，请重新登录");
        }
        if (!authService.hasPermission(accountType, path, request)) {
            log.warn("{} 无权限访问路径 {}", loginId, path);
            throw new GatewayAuthException(403, "无权限访问该资源");
        }

        ServerHttpRequest newRequest = request.mutate()
                .header("X-User-Id", loginId)
                .header("satoken", token)
                .build();

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
