package com.gzasc.aishopping.product.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.product.exception.ProductException;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(ProductException.class)
    public ApiResponse<Void> handleProductException(ProductException e, HttpServletResponse response) {
        log.warn("业务异常: {}", e.getMessage());
        int httpStatus = e.getCode() >= 500 ? 500 : (e.getCode() >= 400 ? e.getCode() : 400);
        response.setStatus(httpStatus);
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数验证失败";
        log.warn("参数验证失败: {}", message);
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统错误", e);
        return ApiResponse.error("系统错误，请稍后重试");
    }
}