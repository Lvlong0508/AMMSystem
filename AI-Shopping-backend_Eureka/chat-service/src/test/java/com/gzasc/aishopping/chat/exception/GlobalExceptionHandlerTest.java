package com.gzasc.aishopping.chat.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import com.gzasc.aishopping.chat.controller.GlobalExceptionHandler;
import com.gzasc.aishopping.common.response.ApiResponse;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("CH-056 ChatException 处理 - 无参构造（默认 code=500）")
    void handleChatException_defaultCode() {
        ChatException ex = new ChatException("商品推荐失败");
        ApiResponse<Void> response = handler.handleChatException(ex);

        assertEquals(500, response.getCode());
        assertEquals("商品推荐失败", response.getMessage());
    }

    @Test
    @DisplayName("CH-057 ChatException 处理 - 带 code 构造")
    void handleChatException_withCode() {
        ChatException ex = new ChatException(4001, "业务错误");
        ApiResponse<Void> response = handler.handleChatException(ex);

        assertEquals(4001, response.getCode());
        assertEquals("业务错误", response.getMessage());
    }

    @Test
    @DisplayName("CH-058 MethodArgumentNotValidException 处理")
    void handleValidationException() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "test");
        bindingResult.addError(new FieldError("test", "message", "消息内容不能为空"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ApiResponse<Void> response = handler.handleValidationException(ex);

        assertEquals(400, response.getCode());
        assertEquals("消息内容不能为空", response.getMessage());
    }

    @Test
    @DisplayName("CH-059 未知 Exception 处理")
    void handleUnknownException() {
        Exception ex = new ArithmeticException("division by zero");
        ApiResponse<Void> response = handler.handleException(ex);

        assertEquals(500, response.getCode());
        assertEquals("系统错误，请稍后重试", response.getMessage());
    }

    @Test
    @DisplayName("CH-060 AiToolException 传递（继承 RuntimeException，走默认 Exception 路径）")
    void handleAiToolException() {
        AiToolException ex = new AiToolException("商品ID格式不正确");
        ApiResponse<Void> response = handler.handleException(ex);

        assertEquals(500, response.getCode());
        assertEquals("系统错误，请稍后重试", response.getMessage());
    }
}
