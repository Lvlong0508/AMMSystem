package com.gzasc.aishopping.chat.exception;

public class FileException extends RuntimeException {
    private int code = 415;

    public FileException(String message) {
        super(message);
    }

    public FileException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode(){return code;}
}
