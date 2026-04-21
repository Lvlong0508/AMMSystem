package com.gzasc.aishopping.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.auth.model.dto.LoginRequest;
import com.gzasc.aishopping.auth.model.dto.RegisterRequest;
import com.gzasc.aishopping.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证控制器测试类
 *
 * 核心测试点：
 * 1. 用户/商家注册接口
 * 2. 用户/商家登录接口
 * 3. 参数校验
 * 4. 用户名/手机号检查接口
 */
@WebMvcTest(AuthController.class)
@DisplayName("AuthController API 接口测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // ==================== 用户注册接口测试 ====================

    @Test
    @DisplayName("POST /api/auth/user/register - 参数校验失败（空用户名）")
    void testUserRegisterValidationFail() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("");  // 空用户名
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())  // 业务层返回 200，但包含错误信息
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("参数错误")));
    }

    @Test
    @DisplayName("GET /api/auth/user/check-username - 检查用户名可用")
    void testCheckUserUsernameAvailable() throws Exception {
        when(authService.userExistsByUsername("newuser")).thenReturn(false);

        mockMvc.perform(get("/api/auth/user/check-username")
                .param("username", "newuser"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(true))
            .andExpect(jsonPath("$.message").value("用户名可用"));
    }

    @Test
    @DisplayName("GET /api/auth/user/check-username - 检查用户名已存在")
    void testCheckUserUsernameExists() throws Exception {
        when(authService.userExistsByUsername("existing")).thenReturn(true);

        mockMvc.perform(get("/api/auth/user/check-username")
                .param("username", "existing"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(false))
            .andExpect(jsonPath("$.message").value("用户名已被使用"));
    }

    @Test
    @DisplayName("GET /api/auth/user/check-phone - 检查手机号可用")
    void testCheckUserPhoneAvailable() throws Exception {
        when(authService.userExistsByPhone("13800138000")).thenReturn(false);

        mockMvc.perform(get("/api/auth/user/check-phone")
                .param("phone", "13800138000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("GET /api/auth/user/check-phone - 检查手机号已注册")
    void testCheckUserPhoneExists() throws Exception {
        when(authService.userExistsByPhone("13800138000")).thenReturn(true);

        mockMvc.perform(get("/api/auth/user/check-phone")
                .param("phone", "13800138000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(false));
    }

    // ==================== 商家接口测试 ====================

    @Test
    @DisplayName("GET /api/auth/merchant/check-username - 检查商家用户名")
    void testCheckMerchantUsername() throws Exception {
        when(authService.merchantExistsByUsername("merchant1")).thenReturn(true);

        mockMvc.perform(get("/api/auth/merchant/check-username")
                .param("username", "merchant1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/merchant/login - 登录参数校验")
    void testMerchantLoginValidation() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("merchant1");
        request.setPassword("pass123");

        mockMvc.perform(post("/api/auth/merchant/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
        // 实际登录需要 Sa-Token，会返回错误，但接口层正常
    }

    // ==================== 登出接口测试 ====================

    @Test
    @DisplayName("POST /api/auth/user/logout - 用户登出接口")
    void testUserLogout() throws Exception {
        mockMvc.perform(post("/api/auth/user/logout"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/merchant/logout - 商家登出接口")
    void testMerchantLogout() throws Exception {
        mockMvc.perform(post("/api/auth/merchant/logout"))
            .andExpect(status().isOk());
    }

    // ==================== 用户信息接口测试 ====================

    @Test
    @DisplayName("GET /api/auth/user/info - 未登录时应返回未登录")
    void testGetUserInfoNotLogin() throws Exception {
        mockMvc.perform(get("/api/auth/user/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("未登录"))
            .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("GET /api/auth/merchant/info - 未登录时应返回未登录")
    void testGetMerchantInfoNotLogin() throws Exception {
        mockMvc.perform(get("/api/auth/merchant/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("未登录"))
            .andExpect(jsonPath("$.code").value(401));
    }
}
