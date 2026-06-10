package com.gzasc.aishopping.shop.exception;

public class ShopException extends RuntimeException {

    private int code = 400;

    public ShopException(String message) {
        super(message);
    }

    public ShopException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ShopException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getCode() {
        return code;
    }
}
