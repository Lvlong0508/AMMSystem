package com.gzasc.aishopping.shop.controller;

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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ShopUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShopService shopService;

    @BeforeEach
    void setUp() {
        var controller = new ShopUserController(shopService);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("SH-030 查询活跃店铺列表 - 有数据")
    void getShopList_success() throws Exception {
        Shop s1 = new Shop(1L, 1001L, 10L, 1, null, null);
        Shop s2 = new Shop(2L, 1002L, 20L, 1, null, null);
        Map<String, Object> listData = Map.of(
                "shops", List.of(s1, s2),
                "total", 2,
                "page", 1,
                "size", 10
        );
        when(shopService.getUserShopList(1, 10)).thenReturn(listData);

        mockMvc.perform(get("/api/user/shop/list")
                        .header("X-User-Id", 2001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.shops.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @DisplayName("SH-031 查询活跃店铺列表 - 分页参数")
    void getShopList_pagination() throws Exception {
        Map<String, Object> listData = Map.of(
                "shops", List.of(),
                "total", 0,
                "page", 2,
                "size", 10
        );
        when(shopService.getUserShopList(2, 10)).thenReturn(listData);

        mockMvc.perform(get("/api/user/shop/list?page=2&size=10")
                        .header("X-User-Id", 2001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("SH-032 查询活跃店铺列表 - 无数据")
    void getShopList_empty() throws Exception {
        Map<String, Object> emptyData = Map.of(
                "shops", List.of(),
                "total", 0,
                "page", 1,
                "size", 10
        );
        when(shopService.getUserShopList(1, 10)).thenReturn(emptyData);

        mockMvc.perform(get("/api/user/shop/list")
                        .header("X-User-Id", 2001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shops").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("SH-033 查询店铺详情 - 活跃店铺")
    void getShopDetail_active() throws Exception {
        Shop shop = new Shop(1L, 1001L, 10L, 1, null, null);
        Map<String, Object> detailData = Map.of("shop", shop, "shopInfo", Map.of(
                "id", 10, "name", "测试店铺", "description", "描述", "logourl", "logo"
        ));
        when(shopService.getActiveShopById(1L)).thenReturn(detailData);

        mockMvc.perform(get("/api/user/shop/1")
                        .header("X-User-Id", 2001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.shop.id").value(1))
                .andExpect(jsonPath("$.data.shopInfo.name").value("测试店铺"));
    }

    @Test
    @DisplayName("SH-034 查询店铺详情 - 已关闭")
    void getShopDetail_closed() throws Exception {
        when(shopService.getActiveShopById(2L))
                .thenThrow(new com.gzasc.aishopping.shop.exception.ShopException("店铺不存在或已关闭"));

        mockMvc.perform(get("/api/user/shop/2")
                        .header("X-User-Id", 2001L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("店铺不存在或已关闭"));
    }

    @Test
    @DisplayName("SH-049 用户端未登录 - 无X-User-Id")
    void getShopList_noAuth() throws Exception {
        mockMvc.perform(get("/api/user/shop/list"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    @DisplayName("用户端查询详情 - 无X-User-Id")
    void getShopDetail_noAuth() throws Exception {
        mockMvc.perform(get("/api/user/shop/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请先登录"));
    }
}
