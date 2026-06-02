package com.gzasc.aishopping.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.product.dto.CreateProductRequest;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.dto.UpdateProductRequest;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ProductSellerControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductService productService;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ProductSellerController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("PR-014 - POST /api/seller/product/create - 创建商品成功")
    void testCreateProductSuccess() throws Exception {
        when(productService.createProductWithImage(any(), eq("http://img.test/a.jpg"))).thenAnswer(invocation -> {
            Product productArg = invocation.getArgument(0);
            productArg.setId(10001L);
            return 1;
        });

        CreateProductRequest request = new CreateProductRequest();
        request.setName("测试商品A");
        request.setDescription("描述");
        request.setPrice(BigDecimal.valueOf(99.99));
        request.setStock(100);
        request.setImageUrl("http://img.test/a.jpg");
        request.setShopId(1L);

        mockMvc.perform(post("/api/seller/product/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PR-015 - POST /api/seller/product/create - 图片为空被拦截")
    void testCreateProductWithoutImage() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("测试商品A");
        request.setPrice(BigDecimal.valueOf(99.99));
        request.setStock(100);
        request.setImageUrl("");

        mockMvc.perform(post("/api/seller/product/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("PR-016 - POST /api/seller/product/create - name为空")
    void testCreateProductWithoutName() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("");
        request.setPrice(BigDecimal.valueOf(99.99));
        request.setStock(100);
        request.setImageUrl("http://img.test/a.jpg");

        mockMvc.perform(post("/api/seller/product/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("PR-017 - POST /api/seller/product/create - price为负数")
    void testCreateProductWithNegativePrice() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("测试商品");
        request.setPrice(BigDecimal.valueOf(-10));
        request.setStock(100);
        request.setImageUrl("http://img.test/a.jpg");

        mockMvc.perform(post("/api/seller/product/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("PR-018 - POST /api/seller/product/create - stock为负数")
    void testCreateProductWithNegativeStock() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("测试商品");
        request.setPrice(BigDecimal.valueOf(100));
        request.setStock(-1);
        request.setImageUrl("http://img.test/a.jpg");

        mockMvc.perform(post("/api/seller/product/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("PR-020 - PUT /api/seller/product/{productId} - 更新商品")
    void testUpdateProductSuccess() throws Exception {
        when(productService.updateProductWithImage(any(), isNull())).thenReturn(1);

        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("新名称");
        request.setPrice(BigDecimal.valueOf(199));

        mockMvc.perform(put("/api/seller/product/2001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PR-023 - PUT /api/seller/product/{productId} - 商品不存在")
    void testUpdateProductNotFound() throws Exception {
        when(productService.updateProductWithImage(any(), isNull())).thenReturn(0);

        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("新名称");

        mockMvc.perform(put("/api/seller/product/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("PR-025 - DELETE /api/seller/product/{productId} - 删除已下架商品")
    void testDeleteProductWhenUnlisted() throws Exception {
        when(productService.deleteProduct(3001L)).thenReturn(1);

        mockMvc.perform(delete("/api/seller/product/3001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PR-026 - DELETE /api/seller/product/{productId} - 删除未下架商品")
    void testDeleteProductWhenListed() throws Exception {
        when(productService.deleteProduct(3002L))
                .thenThrow(new com.gzasc.aishopping.product.exception.ProductException(400, "商品在上架中，请先下架: 3002"));

        mockMvc.perform(delete("/api/seller/product/3002"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("商品在上架中，请先下架: 3002"));
    }

    @Test
    @DisplayName("PR-027 - DELETE /api/seller/product/{productId} - 商品不存在")
    void testDeleteProductNotFound() throws Exception {
        when(productService.deleteProduct(99999L))
                .thenThrow(new com.gzasc.aishopping.product.exception.ProductException(404, "商品不存在: 99999"));

        mockMvc.perform(delete("/api/seller/product/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("PR-028 - GET /api/seller/product/{productId} - 商家查询商品详情")
    void testGetProductDetailFound() throws Exception {
        ProductWithImageDetailDTO dto = new ProductWithImageDetailDTO();
        dto.setId(4001L);
        dto.setName("商家查看商品");
        when(productService.getProductById(4001L)).thenReturn(dto);

        mockMvc.perform(get("/api/seller/product/4001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(4001));
    }

    @Test
    @DisplayName("PR-029 - GET /api/seller/product/{productId} - 商家查询不存在的商品")
    void testGetProductDetailNotFound() throws Exception {
        when(productService.getProductById(99999L)).thenReturn(null);

        mockMvc.perform(get("/api/seller/product/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("商品不存在"));
    }

    @Test
    @DisplayName("PR-030 - GET /api/seller/product/batch - 批量查询")
    void testBatchQuery() throws Exception {
        when(productService.getAbstractProductsForMerchant(List.of(1001L, 1002L, 1003L)))
                .thenReturn(List.of(new ProductWithImageAbstractDTO(), new ProductWithImageAbstractDTO(), new ProductWithImageAbstractDTO()));

        mockMvc.perform(get("/api/seller/product/batch").param("ids", "1001,1002,1003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("PR-060 - POST /api/seller/product/{productId}/list - 上架")
    void testListProduct() throws Exception {
        when(productService.listProduct(7001L)).thenReturn(true);

        mockMvc.perform(post("/api/seller/product/7001/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PR-063 - POST /api/seller/product/{productId}/unlist - 下架")
    void testUnlistProduct() throws Exception {
        when(productService.unlistProduct(7002L)).thenReturn(true);

        mockMvc.perform(post("/api/seller/product/7002/unlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PR-062 - POST /api/seller/product/{productId}/list - 上架不存在的商品")
    void testListProductNotFound() throws Exception {
        when(productService.listProduct(99999L))
                .thenThrow(new com.gzasc.aishopping.product.exception.ProductException(404, "商品不存在: 99999"));

        mockMvc.perform(post("/api/seller/product/99999/list"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("PR-065 - POST /api/seller/product/{productId}/unlist - 下架不存在的商品")
    void testUnlistProductNotFound() throws Exception {
        when(productService.unlistProduct(99999L))
                .thenThrow(new com.gzasc.aishopping.product.exception.ProductException(404, "商品不存在: 99999"));

        mockMvc.perform(post("/api/seller/product/99999/unlist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("PR-064 - POST /api/seller/product/{productId}/unlist - 重复下架（静默成功）")
    void testUnlistProductTwice() throws Exception {
        when(productService.unlistProduct(7002L)).thenReturn(true);

        mockMvc.perform(post("/api/seller/product/7002/unlist"))
                .andExpect(status().isOk());

        when(productService.unlistProduct(7002L)).thenReturn(true);

        mockMvc.perform(post("/api/seller/product/7002/unlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
