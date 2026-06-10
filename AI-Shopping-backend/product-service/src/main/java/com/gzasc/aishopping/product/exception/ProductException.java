package com.gzasc.aishopping.product.exception;

public class ProductException extends RuntimeException {

    private int code = 400;

    public ProductException(String message) {
        super(message);
    }

    public ProductException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}