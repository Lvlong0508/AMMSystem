package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.shop.exception.ShopException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    @Validated
    static class TestController {
        @GetMapping("/test/missing-header")
        public void throwMissingHeader(@RequestHeader("X-Required") String header) {
        }

        @GetMapping("/test/type-mismatch")
        public void throwTypeMismatch(@RequestParam("page") int page) {
        }

        @GetMapping("/test/constraint-violation")
        public void throwConstraintViolation(@RequestParam("id") @jakarta.validation.constraints.Min(1) Long id) {
            throw new jakarta.validation.ConstraintViolationException(
                    "参数校验失败: id 必须大于等于1", java.util.Collections.<jakarta.validation.ConstraintViolation<?>>emptySet());
        }

        @GetMapping("/test/shop-exception")
        public void throwShopException() {
            throw new ShopException("店铺业务异常");
        }

        @PostMapping("/test/unreadable-body")
        public void throwUnreadableBody(@RequestBody TestDto body) {
        }
    }

    static class TestDto {
        public String name;
    }

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GEH-001: MissingRequestHeaderException → 400 缺少必要请求头")
    void handleMissingHeader() throws Exception {
        mockMvc.perform(get("/test/missing-header")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("缺少必要请求头: X-Required"));
    }

    @Test
    @DisplayName("GEH-002: HttpMessageNotReadableException → 400 请求体格式错误")
    void handleMessageNotReadable() throws Exception {
        mockMvc.perform(post("/test/unreadable-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }

    @Test
    @DisplayName("GEH-003: MethodArgumentTypeMismatchException → 400 参数格式错误")
    void handleTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/type-mismatch?page=abc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数格式错误: page"));
    }

    @Test
    @DisplayName("GEH-004: 已有 ShopException 仍正常工作（回归）")
    void handleShopException() throws Exception {
        mockMvc.perform(get("/test/shop-exception")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("店铺业务异常"));
    }

    @Test
    @DisplayName("GEH-005: ConstraintViolationException → 400")
    void handleConstraintViolation() throws Exception {
        mockMvc.perform(get("/test/constraint-violation?id=0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
