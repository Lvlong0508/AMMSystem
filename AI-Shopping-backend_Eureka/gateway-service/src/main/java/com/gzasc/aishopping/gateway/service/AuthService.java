package com.gzasc.aishopping.gateway.service;

import org.springframework.http.server.reactive.ServerHttpRequest;

public interface AuthService {

    boolean isPreFlightRequest(ServerHttpRequest request);

    boolean isWhiteList(String path);

    String validateToken(String token);

    String extractRole(String loginId);

    boolean hasPermission(String loginId, String path, ServerHttpRequest request);
}
