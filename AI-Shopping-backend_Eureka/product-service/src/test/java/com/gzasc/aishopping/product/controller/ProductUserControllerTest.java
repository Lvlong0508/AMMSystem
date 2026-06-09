package com.gzasc.aishopping.product.controller;

import com.gzasc.aishopping.common.dto.product.ProductCardDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ProductUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ProductUserController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("PR-001 - GET /api/user/product/all - 正常分页")
    void testGetAllSalableProductsWithData() throws Exception {
        when(productService.getSalableProductCards(0)).thenReturn(
                List.of(new ProductCardDTO(), new ProductCardDTO()));

        mockMvc.perform(get("/api/user/product/all").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.products").isArray())
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    @DisplayName("PR-002 - GET /api/user/product/all - 空数据分页")
    void testGetAllSalableProductsEmpty() throws Exception {
        when(productService.getSalableProductCards(0)).thenReturn(List.of());

        mockMvc.perform(get("/api/user/product/all").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.products").isArray())
                .andExpect(jsonPath("$.data.size").value(0));
    }

    @Test
    @DisplayName("PR-004 - GET /api/user/product/{productId} - 查询详情")
    void testGetProductByIdFound() throws Exception {
        ProductWithImageDetailDTO dto = new ProductWithImageDetailDTO();
        dto.setId(1001L);
        dto.setName("测试商品");
        when(productService.getProductById(1001L)).thenReturn(dto);

        mockMvc.perform(get("/api/user/product/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("1001"));
    }

    @Test
    @DisplayName("PR-005 - GET /api/user/product/{productId} - 商品不存在")
    void testGetProductByIdNotFound() throws Exception {
        when(productService.getProductById(99999L)).thenReturn(null);

        mockMvc.perform(get("/api/user/product/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("商品不存在"));
    }

    @Test
    @DisplayName("PR-007 - GET /api/user/product/search - 模糊搜索有结果")
    void testSearchProductsFound() throws Exception {
        when(productService.getProductsByName("手机")).thenReturn(
                List.of(new ProductWithImageDetailDTO(), new ProductWithImageDetailDTO(), new ProductWithImageDetailDTO()));

        mockMvc.perform(get("/api/user/product/search").param("name", "手机"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    @DisplayName("PR-008 - GET /api/user/product/search - 模糊搜索无结果")
    void testSearchProductsNotFound() throws Exception {
        when(productService.getProductsByName("不存在商品xxx")).thenReturn(List.of());

        mockMvc.perform(get("/api/user/product/search").param("name", "不存在商品xxx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("PR-010 - GET /api/user/product/price-range - 正常区间")
    void testPriceRangeWithResults() throws Exception {
        when(productService.getProductCardsByPriceRange(eq(BigDecimal.valueOf(50)), eq(BigDecimal.valueOf(200)), eq(0)))
                .thenReturn(List.of(new ProductCardDTO(), new ProductCardDTO()));

        mockMvc.perform(get("/api/user/product/price-range")
                        .param("minPrice", "50")
                        .param("maxPrice", "200")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.products").isArray());
    }

    @Test
    @DisplayName("PR-011 - GET /api/user/product/price-range - 价格区间无交集")
    void testPriceRangeNoResults() throws Exception {
        when(productService.getProductCardsByPriceRange(eq(BigDecimal.valueOf(1000)), eq(BigDecimal.valueOf(10000)), eq(0)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/user/product/price-range")
                        .param("minPrice", "1000")
                        .param("maxPrice", "10000")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.products").isArray())
                .andExpect(jsonPath("$.data.size").value(0));
    }

    @Test
    @DisplayName("PR-009 - GET /api/user/product/search - 搜索关键词为空")
    void testSearchProductsEmptyKeyword() throws Exception {
        when(productService.getProductsByName("")).thenReturn(List.of());

        mockMvc.perform(get("/api/user/product/search").param("name", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PR-006 - GET /api/user/product/{productId} - 商品已下架无is_sale过滤")
    void testGetProductByIdWhenUnlisted() throws Exception {
        ProductWithImageDetailDTO dto = new ProductWithImageDetailDTO();
        dto.setId(1002L);
        dto.setSale(false);
        when(productService.getProductById(1002L)).thenReturn(dto);

        mockMvc.perform(get("/api/user/product/1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("1002"));
    }

    @Test
    @DisplayName("PR-074 - ProductException - 业务异常返回400")
    void testProductExceptionReturns400() throws Exception {
        when(productService.getProductById(99999L)).thenReturn(null);

        mockMvc.perform(get("/api/user/product/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("PR-076 - Exception - 未预期异常返回500")
    void testUnexpectedExceptionReturns500() throws Exception {
        when(productService.getSalableProductCards(0)).thenThrow(new RuntimeException("数据库连接失败"));

        mockMvc.perform(get("/api/user/product/all").param("page", "0"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
    }
}
