package com.gzasc.aishopping.chat.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.chat.exception.AiToolException;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
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

    @SuppressWarnings("unchecked")
    private ApiResponse<Map<String, Object>> apiData(Map<String, Object> data) {
        return ApiResponse.success(data);
    }

    @Test
    @DisplayName("CH-012 getAllProducts - 正常分页")
    void getAllProducts_normal() {
        Map<String, Object> data = new HashMap<>();

        Map<String, Object> item1 = new HashMap<>();
        item1.put("id", 1);
        item1.put("name", "手机");
        item1.put("price", 2999.0);
        item1.put("stock", 100);
        item1.put("imageUrl", "http://img.test/phone.jpg");

        Map<String, Object> item2 = new HashMap<>();
        item2.put("id", 2);
        item2.put("name", "耳机");
        item2.put("price", 199.0);
        item2.put("stock", 200);
        item2.put("imageUrl", "http://img.test/earphone.jpg");

        data.put("products", List.of(item1, item2));

        when(productFeignClient.getAllProducts(0)).thenReturn(apiData(data));

        List<Map<String, Object>> result = productTools.getAllProducts(0);

        assertEquals(2, result.size());
        assertEquals("手机", result.get(0).get("name"));
        assertEquals(100, result.get(0).get("stock"));
        assertEquals("耳机", result.get(1).get("name"));
        assertEquals(200, result.get(1).get("stock"));
    }

    @Test
    @DisplayName("CH-013 getAllProducts - 空数据页")
    void getAllProducts_emptyData() {
        Map<String, Object> data = new HashMap<>();
        data.put("products", List.of());

        when(productFeignClient.getAllProducts(0)).thenReturn(apiData(data));

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
        ApiResponse<Map<String, Object>> response = ApiResponse.error("系统错误");

        when(productFeignClient.getAllProducts(0)).thenReturn(response);

        List<Map<String, Object>> result = productTools.getAllProducts(0);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("CH-016 getAllProducts - data 非 List")
    void getAllProducts_dataNotList() {
        Map<String, Object> data = new HashMap<>();
        data.put("products", "error_string");

        when(productFeignClient.getAllProducts(0)).thenReturn(apiData(data));

        List<Map<String, Object>> result = productTools.getAllProducts(0);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("CH-017 getAllProducts - 商品无额外字段")
    void getAllProducts_noExtraFields() {
        Map<String, Object> data = new HashMap<>();

        Map<String, Object> item = new HashMap<>();
        item.put("id", 1);
        item.put("name", "手机");
        item.put("stock", 50);

        data.put("products", List.of(item));

        when(productFeignClient.getAllProducts(0)).thenReturn(apiData(data));

        List<Map<String, Object>> result = productTools.getAllProducts(0);
        assertEquals(1, result.size());
        assertEquals(50, result.get(0).get("stock"));
    }

    @Test
    @DisplayName("CH-018 getAllProducts - stock 为 0 的商品")
    void getAllProducts_zeroStock() {
        Map<String, Object> data = new HashMap<>();

        Map<String, Object> item = new HashMap<>();
        item.put("id", 1);
        item.put("name", "手机");
        item.put("stock", 0);

        data.put("products", List.of(item));

        when(productFeignClient.getAllProducts(0)).thenReturn(apiData(data));

        List<Map<String, Object>> result = productTools.getAllProducts(0);
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).get("stock"));
    }

    @Test
    @DisplayName("CH-019 getProductDetails - 正常查询")
    void getProductDetails_normal() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 1L);
        data.put("name", "手机");
        data.put("price", 2999.0);
        data.put("stock", 50);

        when(productFeignClient.getProductByIdExternal(1L)).thenReturn(apiData(data));

        Map<String, Object> result = productTools.getProductDetails("1");

        assertEquals("手机", result.get("name"));
        assertEquals(50, result.get("stock"));
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
        when(productFeignClient.getProductByIdExternal(-1L)).thenReturn(apiData(new HashMap<>()));

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
        when(productFeignClient.getProductByIdExternal(999L)).thenReturn(ApiResponse.error("商品不存在"));

        AiToolException ex = assertThrows(AiToolException.class,
                () -> productTools.getProductDetails("999"));
        assertEquals("id不存在或商品已下架", ex.getMessage());
    }

    @Test
    @DisplayName("CH-024 getProductDetails - data 基本字段")
    void getProductDetails_dataNotMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 1L);
        data.put("name", "手机");
        data.put("stock", 30);

        when(productFeignClient.getProductByIdExternal(1L)).thenReturn(apiData(data));

        Map<String, Object> result = productTools.getProductDetails("1");
        assertEquals("手机", result.get("name"));
        assertEquals(30, result.get("stock"));
    }

    @Test
    @DisplayName("CH-025 getProductDetails - 商品无额外字段")
    void getProductDetails_noShop() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 1L);
        data.put("name", "手机");

        when(productFeignClient.getProductByIdExternal(1L)).thenReturn(apiData(data));

        Map<String, Object> result = productTools.getProductDetails("1");
        assertEquals("手机", result.get("name"));
    }
}
