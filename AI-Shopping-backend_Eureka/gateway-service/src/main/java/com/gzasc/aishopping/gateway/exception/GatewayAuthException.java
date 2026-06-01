package com.gzasc.aishopping.gateway.exception;

import reactor.core.publisher.Mono;

public class GatewayAuthException extends RuntimeException {

    private int code;

    public GatewayAuthException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static <T> Mono<T> monoError(int code, String message) {
        return Mono.error(new GatewayAuthException(code, message));
    }
}
