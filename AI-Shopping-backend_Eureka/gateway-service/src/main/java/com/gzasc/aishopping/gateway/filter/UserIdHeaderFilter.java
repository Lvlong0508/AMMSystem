package com.gzasc.aishopping.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
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
public class UserIdHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        // 白名单路径不需要添加userId
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        try {
            // 检查是否已登录
            if (StpUtil.isLogin()) {
                String userId = StpUtil.getLoginIdAsString();
                
                // 添加userId到请求头
                ServerHttpRequest newRequest = request.mutate()
                        .header("userId", userId)
                        .build();
                
                log.debug("添加userId到请求头: {}", userId);
                
                return chain.filter(exchange.mutate().request(newRequest).build());
            }
        } catch (Exception e) {
            log.warn("获取userId失败: {}", e.getMessage());
        }

        return chain.filter(exchange);
    }

    private boolean isWhiteList(String path) {
        return path.contains("/auth/login") || path.contains("/auth/register");
    }

    @Override
    public int getOrder() {
        // 在SaTokenAuthFilter之后执行
        return 0;
    }
}
