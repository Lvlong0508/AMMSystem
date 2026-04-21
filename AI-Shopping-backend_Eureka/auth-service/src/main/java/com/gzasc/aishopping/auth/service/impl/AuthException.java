package com.gzasc.aishopping.auth.service.impl;

/**
 * 认证业务异常
 */
public class AuthException extends RuntimeException {
    
    public AuthException(String message) {
        super(message);
    }
    
    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
