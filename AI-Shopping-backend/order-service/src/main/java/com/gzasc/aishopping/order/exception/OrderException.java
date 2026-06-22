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

    /**
     * 把 CompletableFuture.join() 抛出的 CompletionException 解包为业务异常。
     * - cause 是 OrderException -> 原样返回（保留原始错误代码和文案）
     * - 其它情况（含 cause 为 null） -> 返回通用 "系统繁忙，请稍后重试"
     *
     * 调用方使用： throw OrderException.unwrap(e);
     */
    public static OrderException unwrap(Throwable e) {
        Throwable cause = e == null ? null : e.getCause();
        if (cause instanceof OrderException oe) {
            return oe;
        }
        return new OrderException("系统繁忙，请稍后重试");
    }
}
