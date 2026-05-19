package com.gzasc.aishopping.contact.service.impl;

/**
 * 联系人业务异常类
 * 用于抛出业务逻辑相关的错误信息
 */
public class ContactException extends RuntimeException {

    public ContactException(String message) {
        super(message);
    }

    public ContactException(String message, Throwable cause) {
        super(message, cause);
    }
}