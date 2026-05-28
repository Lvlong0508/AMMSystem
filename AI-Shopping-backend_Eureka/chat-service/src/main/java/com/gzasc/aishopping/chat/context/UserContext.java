package com.gzasc.aishopping.chat.context;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class UserContext {

    private UserContext() {}

    public static Long getUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new RuntimeException("No request context available");
        }
        String userId = attrs.getRequest().getHeader("X-User-Id");
        if (userId == null || userId.isEmpty()) {
            throw new RuntimeException("X-User-Id header is missing");
        }
        return Long.parseLong(userId);
    }
}
