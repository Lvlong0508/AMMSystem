package com.gzasc.aishopping.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "auth.whitelist")
public class AuthWhitelistProperties {

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
