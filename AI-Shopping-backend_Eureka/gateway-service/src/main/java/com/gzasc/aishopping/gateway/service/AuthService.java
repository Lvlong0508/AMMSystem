package com.gzasc.aishopping.gateway.service;

import org.springframework.http.server.reactive.ServerHttpRequest;

public interface AuthService {

    boolean isPreFlightRequest(ServerHttpRequest request);

    boolean isWhiteList(String path);

    String validateToken(String token);

    String getAccountType(String token);

    boolean hasPermission(String accountType, String path, ServerHttpRequest request);
}
