package com.gzasc.aishopping.auth.controller.internal;

import com.gzasc.aishopping.auth.controller.GlobalExceptionHandler;
import com.gzasc.aishopping.auth.exception.AuthException;
import com.gzasc.aishopping.auth.service.MerchantAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class InternalControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MerchantAuthService merchantAuthService;

    @BeforeEach
    void setUp() {
        var controller = new InternalController(merchantAuthService);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("AU-027 店员注册成功")
    void registerEmployee_success() throws Exception {
        when(merchantAuthService.registerEmployee(any())).thenReturn(10001L);

        mockMvc.perform(post("/internal/auth/register-employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"emp01","password":"Emp123","nickname":"店员A","phone":"13200132001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.merchantId").value(10001));
    }

    @Test
    @DisplayName("AU-028 店员注册-用户名重复")
    void registerEmployee_duplicateUsername() throws Exception {
        when(merchantAuthService.registerEmployee(any()))
                .thenThrow(new AuthException("用户名已存在"));

        mockMvc.perform(post("/internal/auth/register-employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"emp01","password":"Emp456","phone":"13200132002"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    @DisplayName("AU-029 店员注册-默认密码场景")
    void registerEmployee_defaultPassword() throws Exception {
        when(merchantAuthService.registerEmployee(any())).thenReturn(10002L);

        mockMvc.perform(post("/internal/auth/register-employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"emp02","nickname":"店员B","phone":"13200132003"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.merchantId").value(10002));
    }
}
