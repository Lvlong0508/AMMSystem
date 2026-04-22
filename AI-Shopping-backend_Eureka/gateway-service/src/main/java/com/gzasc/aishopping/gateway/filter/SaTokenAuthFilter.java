package com.gzasc.aishopping.gateway.filter;

import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.reactor.context.SaHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.List;

@Slf4j
@Configuration
public class SaTokenAuthFilter {

    @Value("${auth.whitelist.paths:}")
    private List<String> whitelistPaths;

    @Bean
    @Order(-100)
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
                // 拦截所有路径
                .addInclude("/**")
                // 放行白名单路径
                .addExclude(getExcludePaths())
                // 认证函数
                .setAuth(obj -> {
                    // 检查登录状态
                    StpUtil.checkLogin();
                    
                    // 获取用户ID并添加到请求头
                    Object loginId = StpUtil.getLoginId();
                    SaHolder.getRequest().setAttribute("userId", loginId);
                    
                    log.debug("用户认证通过，userId: {}", loginId);
                })
                // 异常处理
                .setError(e -> {
                    log.warn("认证失败: {}", e.getMessage());
                    if (e instanceof SaTokenException) {
                        return SaResult.error("未登录，请重新登录").setCode(401);
                    }
                    return SaResult.error("认证失败").setCode(401);
                });
    }

    private String[] getExcludePaths() {
        if (whitelistPaths == null || whitelistPaths.isEmpty()) {
            return new String[]{
                    "/api/user/auth/login",
                    "/api/user/auth/register",
                    "/api/seller/auth/login",
                    "/api/seller/auth/register"
            };
        }
        return whitelistPaths.toArray(new String[0]);
    }
}
