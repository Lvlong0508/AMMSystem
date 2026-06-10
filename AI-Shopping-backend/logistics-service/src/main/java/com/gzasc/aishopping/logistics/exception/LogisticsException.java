package com.gzasc.aishopping.logistics.exception;

public class LogisticsException extends RuntimeException {
    private final int code;

    public LogisticsException(String message) {
        super(message);
        this.code = 400;
    }

    public LogisticsException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
