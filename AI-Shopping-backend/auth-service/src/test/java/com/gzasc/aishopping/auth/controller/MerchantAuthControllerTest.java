package com.gzasc.aishopping.auth.controller;

import com.gzasc.aishopping.auth.converter.AuthConverter;
import com.gzasc.aishopping.auth.dto.LoginResult;
import com.gzasc.aishopping.auth.exception.AuthException;
import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.service.MerchantAuthService;
import com.gzasc.aishopping.auth.service.MerchantInfoService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class MerchantAuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MerchantAuthService merchantAuthService;

    @Mock
    private MerchantInfoService merchantInfoService;

    @Mock
    private AuthConverter authConverter;

    @BeforeEach
    void setUp() {
        var controller = new MerchantAuthController(merchantAuthService, merchantInfoService, authConverter);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("AU-016 商家注册成功")
    void register_success() throws Exception {
        Merchant merchant = new Merchant();
        merchant.setId(67890L);
        merchant.setUsername("seller1");

        LoginResult result = new LoginResult("merchant-token", merchant, "MERCHANT");
        when(merchantAuthService.register(any())).thenReturn(result);
        when(authConverter.toMerchantInfoMap(any(), any())).thenReturn(Map.of("nickname", "商家一号"));

        mockMvc.perform(post("/api/seller/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"seller1","password":"Seller123","nickname":"商家一号","phone":"13900139001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("merchant-token"))
                .andExpect(jsonPath("$.data.accountType").value("MERCHANT"))
                .andExpect(jsonPath("$.data.merchantInfo.nickname").value("商家一号"));
    }

    @Test
    @DisplayName("AU-017 商家登录成功")
    void login_success() throws Exception {
        Merchant merchant = new Merchant();
        merchant.setId(67890L);
        merchant.setUsername("seller1");

        LoginResult result = new LoginResult("login-token", merchant, "MERCHANT");
        when(merchantAuthService.login(anyString(), anyString())).thenReturn(result);
        when(authConverter.toMerchantInfoMap(any(), any())).thenReturn(Map.of("nickname", "商家一号"));

        mockMvc.perform(post("/api/seller/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"seller1","password":"Seller123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("login-token"))
                .andExpect(jsonPath("$.data.accountType").value("MERCHANT"))
                .andExpect(jsonPath("$.data.merchantInfo.nickname").value("商家一号"));
    }

    @Test
    @DisplayName("AU-018 商家注册-用户名重复(跨表不冲突)")
    void register_usernameNotConflictingWithUser() throws Exception {
        Merchant merchant = new Merchant();
        merchant.setId(67891L);
        merchant.setUsername("testuser");

        LoginResult result = new LoginResult("merchant-token", merchant, "MERCHANT");
        when(merchantAuthService.register(any())).thenReturn(result);
        when(authConverter.toMerchantInfoMap(any(), any())).thenReturn(Map.of("nickname", "商家"));

        mockMvc.perform(post("/api/seller/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"testuser","password":"Seller123","nickname":"商家","phone":"13900139002"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("merchant-token"))
                .andExpect(jsonPath("$.data.accountType").value("MERCHANT"));
    }

    @Test
    @DisplayName("AU-019 商家登录-返回 merchantInfo")
    void login_returnsMerchantInfo() throws Exception {
        Merchant merchant = new Merchant();
        merchant.setId(67890L);
        merchant.setUsername("seller1");

        LoginResult result = new LoginResult("login-token", merchant, "MERCHANT");
        when(merchantAuthService.login(anyString(), anyString())).thenReturn(result);
        when(authConverter.toMerchantInfoMap(any(), any()))
                .thenReturn(Map.of("nickname", "商家一号", "avatar", "avatar.jpg"));

        mockMvc.perform(post("/api/seller/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"seller1","password":"Seller123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchantInfo.nickname").value("商家一号"))
                .andExpect(jsonPath("$.data.merchantInfo.avatar").value("avatar.jpg"));
    }
}
