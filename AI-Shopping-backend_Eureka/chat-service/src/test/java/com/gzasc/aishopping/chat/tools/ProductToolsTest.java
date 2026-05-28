package com.gzasc.aishopping.chat.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.chat.exception.AiToolException;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductToolsTest {

    @Mock
    private ProductFeignClient productFeignClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProductTools productTools;

    @Test
    @DisplayName("CH-012 getAllProducts - 正常分页")
    void getAllProducts_normal() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "查询成功");

        Map<String, Object> item1 = new HashMap<>();
        item1.put("id", 1);
        item1.put("name", "手机");
        item1.put("price", 2999.0);
        item1.put("stock", 100);
        Map<String, Object> shop1 = new HashMap<>();
        shop1.put("name", "shopA");
        shop1.put("id", 1L);
        item1.put("shop", shop1);

        Map<String, Object> item2 = new HashMap<>();
        item2.put("id", 2);
        item2.put("name", "耳机");
        item2.put("price", 199.0);
        item2.put("stock", 200);
        Map<String, Object> shop2 = new HashMap<>();
        shop2.put("name", "shopB");
        shop2.put("id", 2L);
        item2.put("shop", shop2);

        response.put("data", List.of(item1, item2));

        when(productFeignClient.getAllProducts(0)).thenReturn(response);

        List<Map<String, Object>> result = productTools.getAllProducts(0);

        assertEquals(2, result.size());
        assertEquals("手机", result.get(0).get("name"));
        assertEquals("shopA", result.get(0).get("shopName"));
        assertFalse(result.get(0).containsKey("shop"));
        assertEquals("耳机", result.get(1).get("name"));
        assertEquals("shopB", result.get(1).get("shopName"));
        assertFalse(result.get(1).containsKey("shop"));
    }

    @Test
    @DisplayName("CH-013 getAllProducts - 空数据页")
    void getAllProducts_emptyData() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "查询成功");
        response.put("data", List.of());

        when(productFeignClient.getAllProducts(0)).thenReturn(response);

        List<Map<String, Object>> result = productTools.getAllProducts(0);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("CH-014 getAllProducts - Feign 返回 null")
    void getAllProducts_feignNull() {
        when(productFeignClient.getAllProducts(0)).thenReturn(null);

        List<Map<String, Object>> result = productTools.getAllProducts(0);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("CH-015 getAllProducts - Feign 返回 error")
    void getAllProducts_feignError() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "系统错误");
        response.put("data", null);

        when(productFeignClient.getAllProducts(0)).thenReturn(response);

        List<Map<String, Object>> result = productTools.getAllProducts(0);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("CH-016 getAllProducts - data 非 List")
    void getAllProducts_dataNotList() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "查询成功");
        response.put("data", "error_string");

        when(productFeignClient.getAllProducts(0)).thenReturn(response);

        List<Map<String, Object>> result = productTools.getAllProducts(0);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("CH-017 getAllProducts - 商品无 shop 字段")
    void getAllProducts_noShopField() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "查询成功");

        Map<String, Object> item = new HashMap<>();
        item.put("id", 1);
        item.put("name", "手机");

        response.put("data", List.of(item));

        when(productFeignClient.getAllProducts(0)).thenReturn(response);

        List<Map<String, Object>> result = productTools.getAllProducts(0);
        assertEquals(1, result.size());
        assertNull(result.get(0).get("shopName"));
        assertFalse(result.get(0).containsKey("shop"));
    }

    @Test
    @DisplayName("CH-018 getAllProducts - shop 字段非 Map")
    void getAllProducts_shopNotMap() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "查询成功");

        Map<String, Object> item = new HashMap<>();
        item.put("id", 1);
        item.put("name", "手机");
        item.put("shop", "invalid");

        response.put("data", List.of(item));

        when(productFeignClient.getAllProducts(0)).thenReturn(response);

        List<Map<String, Object>> result = productTools.getAllProducts(0);
        assertEquals(1, result.size());
        assertNull(result.get(0).get("shopName"));
        assertFalse(result.get(0).containsKey("shop"));
    }

    @Test
    @DisplayName("CH-019 getProductDetails - 正常查询")
    void getProductDetails_normal() {
        Map<String, Object> feignResponse = new HashMap<>();
        feignResponse.put("message", "查询成功");

        Map<String, Object> data = new HashMap<>();
        data.put("id", 1L);
        data.put("name", "手机");
        data.put("price", 2999.0);
        Map<String, Object> shop = new HashMap<>();
        shop.put("name", "shopA");
        shop.put("id", 1L);
        data.put("shop", shop);

        feignResponse.put("data", data);

        when(productFeignClient.getProductByIdExternal(1L)).thenReturn(feignResponse);

        Map<String, Object> result = productTools.getProductDetails("1");

        assertEquals("手机", result.get("name"));
        assertEquals("shopA", result.get("shopName"));
        assertFalse(result.containsKey("shop"));
    }

    @Test
    @DisplayName("CH-020 getProductDetails - 商品 ID 格式错误")
    void getProductDetails_invalidId() {
        AiToolException ex = assertThrows(AiToolException.class,
                () -> productTools.getProductDetails("abc"));
        assertEquals("商品ID格式不正确", ex.getMessage());
    }

    @Test
    @DisplayName("CH-021 getProductDetails - 商品 ID 为负数字符串")
    void getProductDetails_negativeId() {
        Map<String, Object> feignResponse = new HashMap<>();
        feignResponse.put("message", "查询成功");
        feignResponse.put("data", new HashMap<>());

        when(productFeignClient.getProductByIdExternal(-1L)).thenReturn(feignResponse);

        Map<String, Object> result = productTools.getProductDetails("-1");
        assertNotNull(result);
    }

    @Test
    @DisplayName("CH-022 getProductDetails - Feign 返回 null")
    void getProductDetails_feignNull() {
        when(productFeignClient.getProductByIdExternal(999L)).thenReturn(null);

        AiToolException ex = assertThrows(AiToolException.class,
                () -> productTools.getProductDetails("999"));
        assertEquals("id不存在或商品已下架", ex.getMessage());
    }

    @Test
    @DisplayName("CH-023 getProductDetails - Feign 返回 error")
    void getProductDetails_feignError() {
        Map<String, Object> feignResponse = new HashMap<>();
        feignResponse.put("message", "商品不存在");
        feignResponse.put("data", null);

        when(productFeignClient.getProductByIdExternal(999L)).thenReturn(feignResponse);

        AiToolException ex = assertThrows(AiToolException.class,
                () -> productTools.getProductDetails("999"));
        assertEquals("id不存在或商品已下架", ex.getMessage());
    }

    @Test
    @DisplayName("CH-024 getProductDetails - data 非 Map")
    void getProductDetails_dataNotMap() {
        Map<String, Object> feignResponse = new HashMap<>();
        feignResponse.put("message", "查询成功");
        feignResponse.put("data", "error");

        when(productFeignClient.getProductByIdExternal(1L)).thenReturn(feignResponse);

        AiToolException ex = assertThrows(AiToolException.class,
                () -> productTools.getProductDetails("1"));
        assertEquals("id不存在或商品已下架", ex.getMessage());
    }

    @Test
    @DisplayName("CH-025 getProductDetails - 商品无 shop 字段")
    void getProductDetails_noShop() {
        Map<String, Object> feignResponse = new HashMap<>();
        feignResponse.put("message", "查询成功");

        Map<String, Object> data = new HashMap<>();
        data.put("id", 1L);
        data.put("name", "手机");
        feignResponse.put("data", data);

        when(productFeignClient.getProductByIdExternal(1L)).thenReturn(feignResponse);

        Map<String, Object> result = productTools.getProductDetails("1");
        assertEquals("手机", result.get("name"));
        assertNull(result.get("shopName"));
    }
}
