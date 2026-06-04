package com.gzasc.aishopping.product.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.product.exception.ProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ProductException - code=400 返回400")
    void testProductExceptionWith400() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ApiResponse<Void> result = handler.handleProductException(new ProductException(400, "业务异常"), response);

        assertEquals(400, response.getStatus());
        assertEquals(400, result.getCode());
        assertEquals("业务异常", result.getMessage());
    }

    @Test
    @DisplayName("ProductException - code=500 返回500")
    void testProductExceptionWith500() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ApiResponse<Void> result = handler.handleProductException(new ProductException(500, "内部错误"), response);

        assertEquals(500, response.getStatus());
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("ProductException - code<400 使用默认400")
    void testProductExceptionWithLowCode() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ApiResponse<Void> result = handler.handleProductException(new ProductException(200, "业务异常"), response);

        assertEquals(400, response.getStatus());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException - 返回400和字段错误消息")
    void testMethodArgumentNotValidException() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new FieldError("obj", "name", "商品名称不能为空"));
        MethodArgumentNotValidException e = new MethodArgumentNotValidException(null, bindingResult);

        ApiResponse<Void> result = handler.handleValidationException(e);

        assertEquals(400, result.getCode());
        assertEquals("商品名称不能为空", result.getMessage());
    }

    @Test
    @DisplayName("MissingServletRequestPartException - 返回400")
    void testMissingServletRequestPartException() {
        MissingServletRequestPartException e = new MissingServletRequestPartException("image");

        ApiResponse<Void> result = handler.handleMissingPart(e);

        assertEquals(400, result.getCode());
        assertThat(result.getMessage()).contains("image");
    }

    @Test
    @DisplayName("MultipartException - 返回400")
    void testMultipartException() {
        MultipartException e = new MultipartException("文件大小超出限制");

        ApiResponse<Void> result = handler.handleMultipartException(e);

        assertEquals(400, result.getCode());
        assertEquals("文件大小超出限制（最大 10MB）", result.getMessage());
    }

    @Test
    @DisplayName("HttpMessageNotReadableException - 返回400")
    void testHttpMessageNotReadableException() {
        HttpMessageNotReadableException e = new HttpMessageNotReadableException("JSON解析失败");

        ApiResponse<Void> result = handler.handleMessageNotReadable(e);

        assertEquals(400, result.getCode());
        assertEquals("请求参数格式错误，请检查 JSON 格式", result.getMessage());
    }

    @Test
    @DisplayName("Exception - 兜底返回500")
    void testGenericException() {
        Exception e = new RuntimeException("未知错误");

        ApiResponse<Void> result = handler.handleException(e);

        assertEquals(500, result.getCode());
        assertEquals("系统错误，请稍后重试", result.getMessage());
    }

    @Test
    @DisplayName("MissingServletRequestParameterException → 400 缺少必要参数")
    void testMissingServletRequestParameterException() {
        MissingServletRequestParameterException e = new MissingServletRequestParameterException("name", "String");
        ApiResponse<Void> result = handler.handleMissingParam(e);
        assertEquals(400, result.getCode());
        assertThat(result.getMessage()).contains("name");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException → 400 参数格式错误")
    void testMethodArgumentTypeMismatchException() {
        MethodArgumentTypeMismatchException e = new MethodArgumentTypeMismatchException("abc", int.class, "page", null, null);
        ApiResponse<Void> result = handler.handleTypeMismatch(e);
        assertEquals(400, result.getCode());
        assertThat(result.getMessage()).contains("page");
    }
}
