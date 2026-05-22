package com.gzasc.aishopping.chat.exception;

public class ChatException extends RuntimeException {
    private int code = 500;

    public ChatException(String message) {
        super(message);
    }

    public ChatException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
