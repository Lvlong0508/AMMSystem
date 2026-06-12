package com.gzasc.aishopping.chat.config.web;

public class UserContext {
    private static final ThreadLocal<Long> userIdHolder = new InheritableThreadLocal<>();

    public static void setUserId(Long userId) {
        userIdHolder.set(userId);
    }

    public static Long getUserId() {
        Long userId = userIdHolder.get();
        if (userId == null) {
            throw new IllegalStateException("No user context available");
        }
        return userId;
    }

    public static void clear() {
        userIdHolder.remove();
    }
}
