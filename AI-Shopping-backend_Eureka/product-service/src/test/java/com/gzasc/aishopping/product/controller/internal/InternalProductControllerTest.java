package com.gzasc.aishopping.product.controller.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.common.dto.product.StockDeductRequest;
import com.gzasc.aishopping.common.dto.product.StockReserveRequest;
import com.gzasc.aishopping.product.controller.GlobalExceptionHandler;
import com.gzasc.aishopping.product.dto.InternalCreateProductRequest;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.mapper.ProductImageInfoMapper;
import com.gzasc.aishopping.product.mapper.ProductMapper;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;
import com.gzasc.aishopping.product.service.ProductReservationService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class InternalProductControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductService productService;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductImageInfoMapper productImageInfoMapper;

    @Mock
    private ProductReservationService reservationService;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new InternalProductController(productService, productMapper, productImageInfoMapper, reservationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /internal/product/{productId} - 查询商品详情成功")
    void testGetProductByIdFound() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("测试商品");
        product.setPrice(BigDecimal.valueOf(99.99));
        product.setStock(10);
        product.setShopId(100L);
        product.setImageId(1);
        when(productMapper.selectProductById(1L)).thenReturn(product);
        when(productImageInfoMapper.selectURLById(1)).thenReturn(new ProductImageInfo(1, "http://img.test/a.jpg"));

        mockMvc.perform(get("/internal/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("测试商品"))
                .andExpect(jsonPath("$.data.imageUrl").value("http://img.test/a.jpg"));
    }

    @Test
    @DisplayName("GET /internal/product/{productId} - 商品不存在")
    void testGetProductByIdNotFound() throws Exception {
        when(productMapper.selectProductById(99999L)).thenReturn(null);

        mockMvc.perform(get("/internal/product/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("商品不存在"));
    }

    @Test
    @DisplayName("GET /internal/product/{productId} - 商品无图片时返回空imageUrl")
    void testGetProductByIdNoImage() throws Exception {
        Product product = new Product();
        product.setId(2L);
        product.setName("无图商品");
        product.setImageId(null);
        when(productMapper.selectProductById(2L)).thenReturn(product);

        mockMvc.perform(get("/internal/product/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.imageUrl").doesNotExist());
    }

    @Test
    @DisplayName("GET /internal/product/batch - 批量查询成功")
    void testGetProductsByIds() throws Exception {
        when(productService.getAbstractProductsForBuyer(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(new ProductWithImageAbstractDTO(), new ProductWithImageAbstractDTO()));

        mockMvc.perform(get("/internal/product/batch").param("ids", "1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /internal/product/deduct-stock - 扣减库存成功")
    void testDeductStockSuccess() throws Exception {
        when(productService.deductStock(1L, 10)).thenReturn(true);
        StockDeductRequest request = new StockDeductRequest(1L, 10);

        mockMvc.perform(post("/internal/product/deduct-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /internal/product/deduct-stock - 库存不足")
    void testDeductStockInsufficient() throws Exception {
        when(productService.deductStock(1L, 999999)).thenReturn(false);
        StockDeductRequest request = new StockDeductRequest(1L, 999999);

        mockMvc.perform(post("/internal/product/deduct-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("扣减失败：库存不足"));
    }

    @Test
    @DisplayName("POST /internal/product/restore-stock - 恢复库存成功")
    void testRestoreStockSuccess() throws Exception {
        when(productService.restoreStock(1L, 5)).thenReturn(true);
        StockDeductRequest request = new StockDeductRequest(1L, 5);

        mockMvc.perform(post("/internal/product/restore-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /internal/product/restore-stock - 恢复库存失败")
    void testRestoreStockFailed() throws Exception {
        when(productService.restoreStock(1L, 5)).thenReturn(false);
        StockDeductRequest request = new StockDeductRequest(1L, 5);

        mockMvc.perform(post("/internal/product/restore-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("POST /internal/product/reserve-stock - 预占库存成功")
    void testReserveStockSuccess() throws Exception {
        StockReserveRequest request = new StockReserveRequest("order-001", 1L, 3);

        mockMvc.perform(post("/internal/product/reserve-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /internal/product/reserve-stock - 预占库存失败")
    void testReserveStockFailed() throws Exception {
        doThrow(new RuntimeException("库存不足")).when(reservationService).reserve("order-002", "1", 100);
        StockReserveRequest request = new StockReserveRequest("order-002", 1L, 100);

        mockMvc.perform(post("/internal/product/reserve-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("库存不足"));
    }

    @Test
    @DisplayName("POST /internal/product/confirm-reservation - 确认预占成功")
    void testConfirmReservationSuccess() throws Exception {
        mockMvc.perform(post("/internal/product/confirm-reservation").param("orderId", "order-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /internal/product/confirm-reservation - 确认预占失败")
    void testConfirmReservationFailed() throws Exception {
        doThrow(new RuntimeException("预占不存在")).when(reservationService).confirm("order-002");

        mockMvc.perform(post("/internal/product/confirm-reservation").param("orderId", "order-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("预占不存在"));
    }

    @Test
    @DisplayName("POST /internal/product/create - 创建商品成功")
    void testCreateProductSuccess() throws Exception {
        when(productService.createProduct(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(100L);
            return 1;
        });

        InternalCreateProductRequest request = new InternalCreateProductRequest();
        request.setName("内部创建商品");
        request.setPrice(BigDecimal.valueOf(199.99));
        request.setStock(50);

        mockMvc.perform(post("/internal/product/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    @DisplayName("POST /internal/product/create - 创建商品失败")
    void testCreateProductFailed() throws Exception {
        when(productService.createProduct(any(Product.class))).thenReturn(0);

        InternalCreateProductRequest request = new InternalCreateProductRequest();
        request.setName("失败商品");
        request.setPrice(BigDecimal.valueOf(99));

        mockMvc.perform(post("/internal/product/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("GET /internal/product/by-shop/{shopId} - 按店铺分页查询成功")
    void testGetProductsByShopId() throws Exception {
        when(productService.getProductsByShopId(100L, 0, 10))
                .thenReturn(List.of(new ProductWithImageAbstractDTO(), new ProductWithImageAbstractDTO()));

        mockMvc.perform(get("/internal/product/by-shop/100")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /internal/product/release-reservation - 释放预占成功")
    void testReleaseReservationSuccess() throws Exception {
        mockMvc.perform(post("/internal/product/release-reservation").param("orderId", "order-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /internal/product/release-reservation - 释放预占失败")
    void testReleaseReservationFailed() throws Exception {
        doThrow(new RuntimeException("预占记录不存在")).when(reservationService).release("order-002");

        mockMvc.perform(post("/internal/product/release-reservation").param("orderId", "order-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("预占记录不存在"));
    }

    @Test
    @DisplayName("POST /internal/product/deduct-stock - 参数校验失败（商品ID为空）")
    void testDeductStockValidationFailed() throws Exception {
        StockDeductRequest request = new StockDeductRequest(null, 10);

        mockMvc.perform(post("/internal/product/deduct-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /internal/product/create - 参数校验失败（名称为空）")
    void testCreateProductValidationFailed() throws Exception {
        InternalCreateProductRequest request = new InternalCreateProductRequest();
        request.setName("");
        request.setPrice(BigDecimal.valueOf(99));

        mockMvc.perform(post("/internal/product/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
