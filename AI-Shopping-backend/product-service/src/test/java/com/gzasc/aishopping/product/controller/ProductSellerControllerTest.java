package com.gzasc.aishopping.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.product.dto.CreateProductRequest;
import com.gzasc.aishopping.product.dto.SellerProductAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.dto.UpdateProductRequest;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.service.ImageStorageService;
import com.gzasc.aishopping.product.service.ProductCommandService;
import com.gzasc.aishopping.product.service.SellerProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
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
    private SellerProductService sellerProductService;

    @Mock
    private ProductCommandService productCommandService;

    @Mock
    private ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ProductSellerController(sellerProductService, productCommandService, imageStorageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("创建商品成功 - multipart/form-data")
    void testCreateProductSuccess() throws Exception {
        when(productCommandService.createProductWithImage(any(Product.class), any(MultipartFile.class))).thenAnswer(invocation -> {
            Product productArg = invocation.getArgument(0);
            productArg.setId(10001L);
            return 1;
        });

        CreateProductRequest request = new CreateProductRequest();
        request.setName("测试商品A");
        request.setDescription("描述");
        request.setPrice(BigDecimal.valueOf(99.99));
        request.setStock(100);
        request.setShopId(1L);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "fake-image-content".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/seller/product/create")
                        .file(productPart)
                        .file(imageFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("创建商品 - name为空")
    void testCreateProductWithoutName() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("");
        request.setPrice(BigDecimal.valueOf(99.99));
        request.setStock(100);
        request.setShopId(1L);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "fake-image-content".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/seller/product/create")
                        .file(productPart)
                        .file(imageFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("创建商品 - price为负数")
    void testCreateProductWithNegativePrice() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("测试商品");
        request.setPrice(BigDecimal.valueOf(-10));
        request.setStock(100);
        request.setShopId(1L);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "fake-image-content".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/seller/product/create")
                        .file(productPart)
                        .file(imageFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("创建商品 - stock为负数")
    void testCreateProductWithNegativeStock() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("测试商品");
        request.setPrice(BigDecimal.valueOf(100));
        request.setStock(-1);
        request.setShopId(1L);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "fake-image-content".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/seller/product/create")
                        .file(productPart)
                        .file(imageFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("创建商品 - shopId为空")
    void testCreateProductWithoutShopId() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("测试商品");
        request.setPrice(BigDecimal.valueOf(100));
        request.setStock(10);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "fake-image-content".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/seller/product/create")
                        .file(productPart)
                        .file(imageFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("PR-020 - PUT /api/seller/product/{productId} - 更新商品（含图片）")
    void testUpdateProductSuccess() throws Exception {
        when(productCommandService.updateProductWithImage(any(Product.class), any(MultipartFile.class))).thenReturn(1);

        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("新名称");
        request.setPrice(BigDecimal.valueOf(199));

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "fake-image-content".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/seller/product/2001")
                        .file(productPart)
                        .file(imageFile)
                        .with(requestPut -> {
                            requestPut.setMethod("PUT");
                            return requestPut;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PR-023 - PUT /api/seller/product/{productId} - 商品不存在")
    void testUpdateProductNotFound() throws Exception {
        when(productCommandService.updateProductWithImage(any(Product.class), isNull())).thenReturn(0);

        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("新名称");

        MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/seller/product/99999")
                        .file(productPart)
                        .with(requestPut -> {
                            requestPut.setMethod("PUT");
                            return requestPut;
                        }))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("updateProduct - 更新商品不传图片（纯文本）")
    void testUpdateProductWithoutImage() throws Exception {
        when(productCommandService.updateProductWithImage(any(Product.class), isNull())).thenReturn(1);

        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("纯文本更新");

        MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/seller/product/2001")
                        .file(productPart)
                        .with(requestPut -> {
                            requestPut.setMethod("PUT");
                            return requestPut;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("updateProduct - 图片格式不支持")
    void testUpdateProductWithInvalidImageFormat() throws Exception {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("新名称");
        request.setPrice(BigDecimal.valueOf(199));

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.gif", "image/gif", "fake-gif-content".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));
        doThrow(new com.gzasc.aishopping.product.exception.ProductException(400, "不支持的图片格式"))
                .when(imageStorageService).validateImage(any(MultipartFile.class));

        mockMvc.perform(multipart("/api/seller/product/2001")
                        .file(productPart)
                        .file(imageFile)
                        .with(requestPut -> {
                            requestPut.setMethod("PUT");
                            return requestPut;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("PR-025 - DELETE /api/seller/product/{productId} - 删除已下架商品")
    void testDeleteProductWhenUnlisted() throws Exception {
        when(sellerProductService.deleteProduct(3001L)).thenReturn(1);

        mockMvc.perform(delete("/api/seller/product/3001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PR-026 - DELETE /api/seller/product/{productId} - 删除未下架商品")
    void testDeleteProductWhenListed() throws Exception {
        when(sellerProductService.deleteProduct(3002L))
                .thenThrow(new com.gzasc.aishopping.product.exception.ProductException(400, "商品在上架中，请先下架: 3002"));

        mockMvc.perform(delete("/api/seller/product/3002"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("商品在上架中，请先下架: 3002"));
    }

    @Test
    @DisplayName("PR-027 - DELETE /api/seller/product/{productId} - 商品不存在")
    void testDeleteProductNotFound() throws Exception {
        when(sellerProductService.deleteProduct(99999L))
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
        when(sellerProductService.getSellerProductDetail(4001L)).thenReturn(dto);

        mockMvc.perform(get("/api/seller/product/4001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("4001"));
    }

    @Test
    @DisplayName("PR-029 - GET /api/seller/product/{productId} - 商家查询不存在的商品")
    void testGetProductDetailNotFound() throws Exception {
        when(sellerProductService.getSellerProductDetail(99999L)).thenReturn(null);

        mockMvc.perform(get("/api/seller/product/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("商品不存在"));
    }

    @Test
    @DisplayName("PR-030 - GET /api/seller/product/batch - 批量查询")
    void testBatchQuery() throws Exception {
        when(sellerProductService.getSellerProductsAbstract(List.of(1001L, 1002L, 1003L)))
                .thenReturn(List.of(new SellerProductAbstractDTO(), new SellerProductAbstractDTO(), new SellerProductAbstractDTO()));

        mockMvc.perform(get("/api/seller/product/batch").param("ids", "1001,1002,1003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("PR-060 - POST /api/seller/product/{productId}/list - 上架")
    void testListProduct() throws Exception {
        when(sellerProductService.listProduct(7001L)).thenReturn(true);

        mockMvc.perform(post("/api/seller/product/7001/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PR-063 - POST /api/seller/product/{productId}/unlist - 下架")
    void testUnlistProduct() throws Exception {
        when(sellerProductService.unlistProduct(7002L)).thenReturn(true);

        mockMvc.perform(post("/api/seller/product/7002/unlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("PR-062 - POST /api/seller/product/{productId}/list - 上架不存在的商品")
    void testListProductNotFound() throws Exception {
        when(sellerProductService.listProduct(99999L))
                .thenThrow(new com.gzasc.aishopping.product.exception.ProductException(404, "商品不存在: 99999"));

        mockMvc.perform(post("/api/seller/product/99999/list"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("PR-065 - POST /api/seller/product/{productId}/unlist - 下架不存在的商品")
    void testUnlistProductNotFound() throws Exception {
        when(sellerProductService.unlistProduct(99999L))
                .thenThrow(new com.gzasc.aishopping.product.exception.ProductException(404, "商品不存在: 99999"));

        mockMvc.perform(post("/api/seller/product/99999/unlist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("PR-064 - POST /api/seller/product/{productId}/unlist - 重复下架（静默成功）")
    void testUnlistProductTwice() throws Exception {
        when(sellerProductService.unlistProduct(7002L)).thenReturn(true);

        mockMvc.perform(post("/api/seller/product/7002/unlist"))
                .andExpect(status().isOk());

        when(sellerProductService.unlistProduct(7002L)).thenReturn(true);

        mockMvc.perform(post("/api/seller/product/7002/unlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
