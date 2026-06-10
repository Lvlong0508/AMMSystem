package com.gzasc.aishopping.shop.controller.internal;

import com.gzasc.aishopping.common.dto.shop.CreateShopForMerchantRequest;
import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.controller.GlobalExceptionHandler;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.ShopService;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class InternalShopControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShopService shopService;

    @BeforeEach
    void setUp() {
        var controller = new InternalShopController(shopService);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("内部接口 - 为商家创建店铺成功")
    void createShopForMerchant_success() throws Exception {
        Shop shop = new Shop();
        shop.setId(10001L);
        when(shopService.createShop(any(), eq(1001L))).thenReturn(shop);

        mockMvc.perform(post("/internal/shop/create-for-merchant")
                        .header("X-Internal-Source", "auth-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"merchantId":1001,"name":"我的小店","description":"描述","logoUrl":"http://logo"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(10001));
    }

    @Test
    @DisplayName("内部接口 - 创建店铺时shop-service异常")
    void createShopForMerchant_serviceError() throws Exception {
        when(shopService.createShop(any(), anyLong())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/internal/shop/create-for-merchant")
                        .header("X-Internal-Source", "auth-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"merchantId":1001,"name":"我的小店"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("创建店铺失败"));
    }

    @Test
    @DisplayName("内部接口 - 店铺名称为空")
    void createShopForMerchant_nameBlank() throws Exception {
        mockMvc.perform(post("/internal/shop/create-for-merchant")
                        .header("X-Internal-Source", "auth-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"merchantId":1001,"name":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("查询店铺信息")
    void getShopInfo() throws Exception {
        ShopInfoDTO dto = new ShopInfoDTO(10L, "测试店铺", "测试描述", "logo");
        when(shopService.getShopInfoById(1L)).thenReturn(dto);

        mockMvc.perform(get("/internal/shop/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("测试店铺"))
                .andExpect(jsonPath("$.data.description").value("测试描述"));
    }

    @Test
    @DisplayName("查询店铺信息 - 无关联info")
    void getShopInfo_null() throws Exception {
        when(shopService.getShopInfoById(1L)).thenReturn(null);

        mockMvc.perform(get("/internal/shop/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("批量查询店铺信息")
    void batchGetShopInfo() throws Exception {
        ShopInfoDTO dto1 = new ShopInfoDTO(10L, "店铺A", "描述A", "logoA");
        ShopInfoDTO dto2 = new ShopInfoDTO(20L, "店铺B", "描述B", "logoB");
        when(shopService.batchGetShopInfo(Set.of(1L, 2L, 3L)))
                .thenReturn(Map.of(1L, dto1, 2L, dto2));

        mockMvc.perform(post("/internal/shop/info/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.['1'].name").value("店铺A"))
                .andExpect(jsonPath("$.data.['2'].name").value("店铺B"))
                .andExpect(jsonPath("$.data.['3']").doesNotExist());
    }

    @Test
    @DisplayName("批量查询 - 空集合")
    void batchGetShopInfo_empty() throws Exception {
        when(shopService.batchGetShopInfo(Set.of())).thenReturn(Map.of());

        mockMvc.perform(post("/internal/shop/info/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
