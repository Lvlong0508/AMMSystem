package com.gzasc.aishopping.auth.controller;

import com.gzasc.aishopping.auth.converter.AuthConverter;
import com.gzasc.aishopping.auth.dto.LoginResult;
import com.gzasc.aishopping.auth.exception.AuthException;
import com.gzasc.aishopping.auth.model.User;
import com.gzasc.aishopping.auth.service.UserAuthService;
import com.gzasc.aishopping.auth.service.UserInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserAuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserAuthService userAuthService;

    @Mock
    private UserInfoService userInfoService;

    @Mock
    private AuthConverter authConverter;

    @BeforeEach
    void setUp() {
        var controller = new UserAuthController(userAuthService, userInfoService, authConverter);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("AU-001 用户注册成功")
    void register_success() throws Exception {
        User user = new User();
        user.setId(12345L);
        user.setUsername("testuser");

        LoginResult result = new LoginResult("test-token", user, "USER");
        when(userAuthService.register(any())).thenReturn(result);
        when(authConverter.toUserInfoMap(any(), any())).thenReturn(Map.of("nickname", "测试"));

        mockMvc.perform(post("/api/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"testuser","password":"Abc123","nickname":"测试","phone":"13800138001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("test-token"))
                .andExpect(jsonPath("$.data.accountType").value("USER"))
                .andExpect(jsonPath("$.data.userInfo.nickname").value("测试"));
    }

    @Test
    @DisplayName("AU-002 注册-用户名重复")
    void register_duplicateUsername() throws Exception {
        when(userAuthService.register(any())).thenThrow(new AuthException("用户名已存在"));

        mockMvc.perform(post("/api/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"testuser","password":"Def456","nickname":"重复","phone":"13800138002"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    @DisplayName("AU-003 注册-手机号重复")
    void register_duplicatePhone() throws Exception {
        when(userAuthService.register(any())).thenThrow(new AuthException("手机号已被注册"));

        mockMvc.perform(post("/api/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newuser","password":"Abc123","nickname":"重复","phone":"13800138001"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("手机号已被注册"));
    }

    @Test
    @DisplayName("AU-009 用户登录成功")
    void login_success() throws Exception {
        User user = new User();
        user.setId(12345L);
        user.setUsername("testuser");

        LoginResult result = new LoginResult("login-token", user, "USER");
        when(userAuthService.login(anyString(), anyString())).thenReturn(result);
        when(authConverter.toUserInfoMap(any(), any())).thenReturn(Map.of("nickname", "测试"));

        mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"testuser","password":"Abc123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("login-token"))
                .andExpect(jsonPath("$.data.accountType").value("USER"))
                .andExpect(jsonPath("$.data.userInfo.nickname").value("测试"));
    }

    @Test
    @DisplayName("AU-010 登录-密码错误")
    void login_wrongPassword() throws Exception {
        when(userAuthService.login(anyString(), anyString()))
                .thenThrow(new AuthException("用户名或密码错误"));

        mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"testuser","password":"WrongPass1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("AU-011 登录-用户名不存在")
    void login_userNotFound() throws Exception {
        when(userAuthService.login(anyString(), anyString()))
                .thenThrow(new AuthException("用户名或密码错误"));

        mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"nonexistent","password":"Abc123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("AU-012 登录-账户已禁用")
    void login_accountDisabled() throws Exception {
        when(userAuthService.login(anyString(), anyString()))
                .thenThrow(new AuthException("账号已被禁用"));

        mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"testuser","password":"Abc123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("账号已被禁用"));
    }

    @Test
    @DisplayName("AU-020 用户登出成功")
    void logout_success() throws Exception {
        doNothing().when(userAuthService).logout();

        mockMvc.perform(post("/api/user/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登出成功"));
    }

    @Test
    @DisplayName("AU-022 用户名可用")
    void checkUsername_available() throws Exception {
        when(userAuthService.existsByUsername("newuser")).thenReturn(false);

        mockMvc.perform(get("/api/user/auth/check-username")
                        .param("username", "newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    @DisplayName("AU-023 用户名不可用")
    void checkUsername_unavailable() throws Exception {
        when(userAuthService.existsByUsername("testuser")).thenReturn(true);

        mockMvc.perform(get("/api/user/auth/check-username")
                        .param("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    @DisplayName("AU-024 手机号可用")
    void checkPhone_available() throws Exception {
        when(userAuthService.existsByPhone("13700137000")).thenReturn(false);

        mockMvc.perform(get("/api/user/auth/check-phone")
                        .param("phone", "13700137000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    @DisplayName("AU-025 手机号不可用")
    void checkPhone_unavailable() throws Exception {
        when(userAuthService.existsByPhone("13800138001")).thenReturn(true);

        mockMvc.perform(get("/api/user/auth/check-phone")
                        .param("phone", "13800138001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    @DisplayName("AU-034 RegisterRequest 用户名为 null")
    void register_validation_usernameNull() throws Exception {
        mockMvc.perform(post("/api/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"Abc123","phone":"13800138001"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("AU-035 RegisterRequest 密码为 null")
    void register_validation_passwordNull() throws Exception {
        mockMvc.perform(post("/api/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"testuser","phone":"13800138001"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("AU-036 LoginRequest 用户名为 null")
    void login_validation_usernameNull() throws Exception {
        mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"Abc123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("AU-037 LoginRequest 密码为 null")
    void login_validation_passwordNull() throws Exception {
        mockMvc.perform(post("/api/user/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"testuser"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("AU-038 RegisterRequest 手机号格式校验不通过")
    void register_validation_invalidPhone() throws Exception {
        mockMvc.perform(post("/api/user/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"testuser","password":"Abc123","phone":"138001380"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
