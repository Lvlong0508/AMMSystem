package com.gzasc.aishopping.order.exception;

public class OrderException extends RuntimeException {
    private int code = 400;

    public OrderException(String message) {
        super(message);
    }

    public OrderException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
