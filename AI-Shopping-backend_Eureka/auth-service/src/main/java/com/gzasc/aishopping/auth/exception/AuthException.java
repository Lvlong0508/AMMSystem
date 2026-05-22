package com.gzasc.aishopping.auth.exception;

public class AuthException extends RuntimeException {

    private int code = 400;

    public AuthException(String message) {
        super(message);
    }

    public AuthException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getCode() {
        return code;
    }
}
