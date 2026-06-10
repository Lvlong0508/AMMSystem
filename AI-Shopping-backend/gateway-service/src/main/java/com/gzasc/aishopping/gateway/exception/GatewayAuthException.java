package com.gzasc.aishopping.gateway.exception;

public class GatewayAuthException extends RuntimeException {

    private int code;

    public GatewayAuthException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
