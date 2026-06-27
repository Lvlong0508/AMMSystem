package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.shop.dto.SimpleShopDTO;
import com.gzasc.aishopping.shop.dto.UpdateShopRequest;
import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.ShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ShopMerchantControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShopService shopService;

    @BeforeEach
    void setUp() {
        var controller = new ShopMerchantController(shopService);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ========== 查询我的店铺（一对一） ==========

    @Test
    @DisplayName("查询我的店铺 - 有店铺")
    void getMyShop_found() throws Exception {
        SimpleShopDTO shop = new SimpleShopDTO(1L, "我的小店", 1);
        when(shopService.getMyShop(1001L)).thenReturn(shop);

        mockMvc.perform(get("/api/seller/shop/my-shop")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.shop.id").value(1))
                .andExpect(jsonPath("$.data.shop.name").value("我的小店"))
                .andExpect(jsonPath("$.data.shop.status").value(1));
    }

    @Test
    @DisplayName("查询我的店铺 - 无店铺")
    void getMyShop_notFound() throws Exception {
        when(shopService.getMyShop(1001L)).thenReturn(null);

        mockMvc.perform(get("/api/seller/shop/my-shop")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.shop").isEmpty());
    }

    // ========== 查询店铺详情 ==========

    @Test
    @DisplayName("查询店铺详情 - 有权限")
    void getShop_success() throws Exception {
        Shop shop = new Shop(1L, 1001L, 10L, 1, null, null);
        ShopInfoDTO shopInfo = new ShopInfoDTO(10L, "测试店铺", "测试描述", "logo");
        when(shopService.getShopWithAccessCheck(1L, 1001L)).thenReturn(shop);
        when(shopService.getShopInfoById(1L)).thenReturn(shopInfo);

        mockMvc.perform(get("/api/seller/shop/1")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.shop.id").value(1))
                .andExpect(jsonPath("$.data.shop.merchantId").value(1001))
                .andExpect(jsonPath("$.data.shop.shopInfoId").value(10))
                .andExpect(jsonPath("$.data.shopInfo.name").value("测试店铺"));
    }

    @Test
    @DisplayName("查询店铺详情 - 无权限")
    void getShop_noAccess() throws Exception {
        when(shopService.getShopWithAccessCheck(1L, 9999L))
                .thenThrow(new ShopException("无权限访问该店铺"));

        mockMvc.perform(get("/api/seller/shop/1")
                        .header("X-User-Id", 9999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("无权限访问该店铺"));
    }

    @Test
    @DisplayName("查询店铺详情 - 店铺不存在")
    void getShop_notFound() throws Exception {
        when(shopService.getShopWithAccessCheck(999L, 1001L))
                .thenThrow(new ShopException("店铺不存在"));

        mockMvc.perform(get("/api/seller/shop/999")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("店铺不存在"));
    }

    // ========== 更新店铺 ==========

    @Test
    @DisplayName("更新店铺成功")
    void updateShop_success() throws Exception {
        MockMultipartFile shopPart = new MockMultipartFile(
                "shop", "", "application/json",
                "{\"name\":\"新店铺\",\"description\":\"新描述\",\"logoId\":\"new-logo\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/seller/shop/1")
                        .file(shopPart)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新店铺成功"));

        verify(shopService).updateShop(eq(1L), any(UpdateShopRequest.class), eq(1001L), isNull());
    }

    // ========== 关闭/开启店铺 ==========

    @Test
    @DisplayName("关闭店铺成功")
    void closeShop_success() throws Exception {
        mockMvc.perform(patch("/api/seller/shop/1/close")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("关闭店铺成功"));

        verify(shopService).closeShop(1L, 1001L);
    }

    @Test
    @DisplayName("开启店铺成功")
    void openShop_success() throws Exception {
        mockMvc.perform(patch("/api/seller/shop/1/open")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("开启店铺成功"));

        verify(shopService).openShop(1L, 1001L);
    }

    // ========== 异常处理 ==========

    @Test
    @DisplayName("未知异常返回500")
    void unknownException() throws Exception {
        when(shopService.getMyShop(any()))
                .thenThrow(new RuntimeException("未知错误"));

        mockMvc.perform(get("/api/seller/shop/my-shop")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }
}