package com.gzasc.aishopping.gateway.exception;

/**
 * 网关认证/鉴权异常。
 *
 * 在 SaTokenAuthGlobalFilter 中认证失败时抛出,
 * 由 GlobalErrorWebExceptionHandler 统一捕获并转为标准的 JSON 错误响应(code + message)。
 * code 通常为 401(未登录/过期)或 403(无权限)。
 */
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
