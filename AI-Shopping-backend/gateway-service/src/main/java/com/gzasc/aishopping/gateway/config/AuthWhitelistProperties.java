package com.gzasc.aishopping.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 认证白名单配置。
 *
 * 配置哪些请求路径不需要登录认证即可访问(如登录、注册接口)。
 * 对应 application.yml 的 auth.whitelist.xxx 前缀。
 * 默认值在构造时初始化,也可通过 Nacos/yml 覆盖。
 */
@ConfigurationProperties(prefix = "auth.whitelist")
public class AuthWhitelistProperties {

    // 不需要登录即可访问的路径列表,默认支持用户端和商家端的登录/注册/校验接口
    private List<String> paths = new ArrayList<>(Arrays.asList(
            "/api/user/auth/login",
            "/api/user/auth/register",
            "/api/user/auth/check-username",
            "/api/user/auth/check-phone",
            "/api/seller/auth/login",
            "/api/seller/auth/register",
            "/api/seller/auth/check-username",
            "/api/seller/auth/check-phone"
    ));

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }
}
