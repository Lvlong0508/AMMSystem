package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.common.feign.shop.ShopFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.product.converter.ProductConverter;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.mapper.ProductImageInfoMapper;
import com.gzasc.aishopping.product.mapper.ProductMapper;
import com.gzasc.aishopping.product.mapper.SalableProductMapper;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;
import com.gzasc.aishopping.product.service.ImageStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductImageInfoMapper productImageInfoMapper;
    @Mock
    private SalableProductMapper salableProductMapper;
    @Mock
    private ProductConverter productConverter;
    @Mock
    private ShopFeignClient shopFeignClient;
    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private ProductServiceImpl productService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productService, "imageBaseUrl", "http://localhost:8081");
    }

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    @Test
    @DisplayName("PR-004 - 根据ID查询商品详情 - 存在")
    void testGetProductByIdFound() {
        Product product = new Product();
        product.setId(1001L);
        product.setName("测试商品");
        product.setImageId(1);
        product.setShopId(10L);
        when(productMapper.selectProductById(1001L)).thenReturn(product);
        when(productImageInfoMapper.selectURLById(1)).thenReturn(new ProductImageInfo(1, "http://img.test/a.jpg"));
        when(shopFeignClient.getShopInfo(10L)).thenReturn(ApiResponse.success(null));
        ProductWithImageDetailDTO dto = new ProductWithImageDetailDTO();
        when(productConverter.toDetailWithImageDTO(eq(product), anyString(), any())).thenReturn(dto);

        ProductWithImageDetailDTO result = productService.getProductById(1001L);

        assertNotNull(result);
        verify(productMapper).selectProductById(1001L);
    }

    @Test
    @DisplayName("PR-005 - 根据ID查询商品详情 - 不存在返回null")
    void testGetProductByIdNotFound() {
        when(productMapper.selectProductById(99999L)).thenReturn(null);

        ProductWithImageDetailDTO result = productService.getProductById(99999L);

        assertNull(result);
    }

    @Test
    @DisplayName("PR-007 - 按名称模糊搜索有结果")
    void testGetProductsByNameFound() {
        Product product = new Product();
        product.setId(1L);
        product.setName("测试手机");
        product.setImageId(1);
        product.setSale(true);
        product.setShopId(10L);
        when(productMapper.selectProductsByName("手机")).thenReturn(List.of(product));
        when(productImageInfoMapper.selectByIds(anyList())).thenReturn(List.of(new ProductImageInfo(1, "http://img.test/phone.jpg")));
        when(productConverter.toDetailWithImageDTOList(anyList(), anyMap(), anyMap())).thenReturn(List.of(new ProductWithImageDetailDTO()));

        List<ProductWithImageDetailDTO> result = productService.getProductsByName("手机");

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("PR-008 - 按名称模糊搜索无结果")
    void testGetProductsByNameNotFound() {
        when(productMapper.selectProductsByName("不存在商品xxx")).thenReturn(List.of());

        List<ProductWithImageDetailDTO> result = productService.getProductsByName("不存在商品xxx");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("PR-001 - 分页查询可售商品 - 有数据")
    void testGetSalableProductsAbstractWithData() {
        when(salableProductMapper.selectAll(0, 20)).thenReturn(List.of(1L, 2L));
        Product p1 = new Product(); p1.setId(1L); p1.setImageId(1); p1.setShopId(10L);
        Product p2 = new Product(); p2.setId(2L); p2.setImageId(2); p2.setShopId(10L);
        when(productMapper.selectAbstractProductsByIds(List.of(1L, 2L))).thenReturn(List.of(p1, p2));
        when(productImageInfoMapper.selectByIds(anyList())).thenReturn(List.of());
        when(productConverter.toAbstractWithImageDTOList(anyList(), anyMap(), anyMap())).thenReturn(List.of(new ProductWithImageAbstractDTO(), new ProductWithImageAbstractDTO()));

        List<ProductWithImageAbstractDTO> result = productService.getSalableProductsAbstract(0);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("PR-002-b - shop-service不可用时 分页查询应降级返回空店铺信息")
    void testGetSalableProductsAbstractWhenShopServiceDown() {
        when(salableProductMapper.selectAll(0, 20)).thenReturn(List.of(1L));
        Product p = new Product();
        p.setId(1L);
        p.setImageId(1);
        p.setShopId(10L);
        when(productMapper.selectAbstractProductsByIds(List.of(1L))).thenReturn(List.of(p));
        when(productImageInfoMapper.selectByIds(anyList())).thenReturn(List.of());
        when(shopFeignClient.batchGetShopInfo(any())).thenThrow(new RuntimeException("shop-service unavailable"));
        when(productConverter.toAbstractWithImageDTOList(anyList(), anyMap(), anyMap())).thenReturn(List.of(new ProductWithImageAbstractDTO()));

        List<ProductWithImageAbstractDTO> result = productService.getSalableProductsAbstract(0);

        assertEquals(1, result.size());
        verify(productConverter).toAbstractWithImageDTOList(anyList(), anyMap(), anyMap());
    }

    @Test
    @DisplayName("PR-002 - 分页查询可售商品 - 无数据")
    void testGetSalableProductsAbstractEmpty() {
        when(salableProductMapper.selectAll(0, 20)).thenReturn(List.of());

        List<ProductWithImageAbstractDTO> result = productService.getSalableProductsAbstract(0);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("PR-010 - 价格区间查询 - 有结果")
    void testGetProductsByPriceRangeWithResults() {
        Product p = new Product(); p.setId(1L); p.setImageId(1); p.setSale(true); p.setShopId(10L);
        when(productMapper.selectByPriceRangeWithPage(eq(BigDecimal.valueOf(50)), eq(BigDecimal.valueOf(200)), eq(0))).thenReturn(List.of(p));
        when(productImageInfoMapper.selectByIds(anyList())).thenReturn(List.of());
        when(productConverter.toAbstractWithImageDTOList(anyList(), anyMap(), anyMap())).thenReturn(List.of(new ProductWithImageAbstractDTO()));

        List<ProductWithImageAbstractDTO> result = productService.getProductsByPriceRange(BigDecimal.valueOf(50), BigDecimal.valueOf(200), 0);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("PR-011 - 价格区间查询 - 无结果")
    void testGetProductsByPriceRangeEmpty() {
        when(productMapper.selectByPriceRangeWithPage(eq(BigDecimal.valueOf(1000)), eq(BigDecimal.valueOf(10000)), eq(0))).thenReturn(List.of());

        List<ProductWithImageAbstractDTO> result = productService.getProductsByPriceRange(BigDecimal.valueOf(1000), BigDecimal.valueOf(10000), 0);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("创建商品（含图片上传）")
    void testCreateProductWithImage() {
        Product product = new Product();
        product.setName("测试商品A");
        product.setDescription("描述");
        product.setPrice(BigDecimal.valueOf(99.99));
        product.setStock(100);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "fake-image-content".getBytes());
        when(imageStorageService.saveImage(anyLong(), any(MultipartFile.class))).thenReturn("/image/goods/main/123/123_abc123.jpg");
        when(productImageInfoMapper.insert(any(ProductImageInfo.class))).thenReturn(1);
        when(productMapper.insertProduct(any(Product.class))).thenReturn(1);

        int result = productService.createProductWithImage(product, imageFile);

        assertEquals(1, result);
        verify(imageStorageService).saveImage(anyLong(), any(MultipartFile.class));
        verify(productImageInfoMapper).insert(any(ProductImageInfo.class));
        verify(productMapper).insertProduct(productCaptor.capture());
        Product captured = productCaptor.getValue();
        assertNotNull(captured.getId());
        assertEquals("测试商品A", captured.getName());
    }

    @Test
    @DisplayName("PR-020 - 更新商品名称和价格")
    void testUpdateProductWithImageSuccess() {
        Product existing = new Product();
        existing.setId(2001L);
        existing.setName("旧名称");
        existing.setImageId(1);
        when(productMapper.selectProductById(2001L)).thenReturn(existing);
        when(productImageInfoMapper.updateUrl(any(ProductImageInfo.class))).thenReturn(1);
        when(productMapper.updateProduct(any(Product.class))).thenReturn(1);

        Product update = new Product();
        update.setId(2001L);
        update.setName("新名称");
        update.setPrice(BigDecimal.valueOf(199));
        int result = productService.updateProductWithImage(update, "http://img.test/new.jpg");

        assertEquals(1, result);
        verify(productImageInfoMapper).updateUrl(any(ProductImageInfo.class));
        verify(productMapper).updateProduct(any(Product.class));
    }

    @Test
    @DisplayName("PR-023 - 更新不存在的商品")
    void testUpdateProductNotFound() {
        when(productMapper.selectProductById(99999L)).thenReturn(null);

        Product update = new Product();
        update.setId(99999L);
        update.setName("新名称");
        ProductException exception = assertThrows(ProductException.class,
                () -> productService.updateProductWithImage(update, null));
        assertTrue(exception.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("PR-025 - 删除已下架商品")
    void testDeleteProductWhenUnlisted() {
        Product product = new Product();
        product.setId(3001L);
        product.setSale(false);
        when(productMapper.selectProductById(3001L)).thenReturn(product);
        when(productMapper.deleteProduct(3001L)).thenReturn(1);

        int result = productService.deleteProduct(3001L);

        assertEquals(1, result);
    }

    @Test
    @DisplayName("PR-026 - 删除未下架商品（在售中）抛出异常")
    void testDeleteProductWhenListedThrows() {
        Product product = new Product();
        product.setId(3002L);
        product.setSale(true);
        when(productMapper.selectProductById(3002L)).thenReturn(product);

        ProductException exception = assertThrows(ProductException.class,
                () -> productService.deleteProduct(3002L));
        assertTrue(exception.getMessage().contains("上架中"));
    }

    @Test
    @DisplayName("PR-027 - 删除不存在的商品")
    void testDeleteProductNotFound() {
        when(productMapper.selectProductById(99999L)).thenReturn(null);

        ProductException exception = assertThrows(ProductException.class,
                () -> productService.deleteProduct(99999L));
        assertTrue(exception.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("PR-055 - 直接扣减库存成功")
    void testDeductStockSuccess() {
        when(productMapper.deductStock(6001L, 10)).thenReturn(1);

        boolean result = productService.deductStock(6001L, 10);

        assertTrue(result);
    }

    @Test
    @DisplayName("PR-056 - 库存不足扣减失败")
    void testDeductStockInsufficient() {
        when(productMapper.deductStock(6001L, 999)).thenReturn(0);

        boolean result = productService.deductStock(6001L, 999);

        assertFalse(result);
    }

    @Test
    @DisplayName("PR-058 - 恢复库存成功")
    void testRestoreStockSuccess() {
        when(productMapper.restoreStock(6001L, 5)).thenReturn(1);

        boolean result = productService.restoreStock(6001L, 5);

        assertTrue(result);
    }

    @Test
    @DisplayName("PR-060 - 上架商品")
    void testListProduct() {
        Product product = new Product();
        product.setId(7001L);
        product.setSale(false);
        when(productMapper.selectProductById(7001L)).thenReturn(product);
        when(productMapper.updateSaleStatus(7001L, true)).thenReturn(1);
        when(salableProductMapper.addSalable(7001L)).thenReturn(1);

        boolean result = productService.listProduct(7001L);

        assertTrue(result);
        verify(productMapper).updateSaleStatus(7001L, true);
        verify(salableProductMapper).addSalable(7001L);
    }

    @Test
    @DisplayName("PR-063 - 下架商品")
    void testUnlistProduct() {
        Product product = new Product();
        product.setId(7002L);
        product.setSale(true);
        when(productMapper.selectProductById(7002L)).thenReturn(product);
        when(productMapper.updateSaleStatus(7002L, false)).thenReturn(1);
        when(salableProductMapper.removeSalable(7002L)).thenReturn(1);

        boolean result = productService.unlistProduct(7002L);

        assertTrue(result);
        verify(productMapper).updateSaleStatus(7002L, false);
        verify(salableProductMapper).removeSalable(7002L);
    }

    @Test
    @DisplayName("PR-062 - 上架不存在的商品")
    void testListProductNotFound() {
        when(productMapper.selectProductById(99999L)).thenReturn(null);

        ProductException exception = assertThrows(ProductException.class,
                () -> productService.listProduct(99999L));
        assertTrue(exception.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("PR-065 - 下架不存在的商品")
    void testUnlistProductNotFound() {
        when(productMapper.selectProductById(99999L)).thenReturn(null);

        ProductException exception = assertThrows(ProductException.class,
                () -> productService.unlistProduct(99999L));
        assertTrue(exception.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("PR-030 - 商家批量查询商品")
    void testGetAbstractProductsForMerchant() {
        Product p = new Product(); p.setId(1L); p.setImageId(1); p.setShopId(10L);
        when(productMapper.selectAbstractProductsByIdsJustMerchant(List.of(1001L, 1002L))).thenReturn(List.of(p));
        when(productImageInfoMapper.selectByIds(anyList())).thenReturn(List.of(new ProductImageInfo(1, "http://img.test/a.jpg")));
        when(productConverter.toAbstractWithImageDTOList(anyList(), anyMap(), anyMap())).thenReturn(List.of(new ProductWithImageAbstractDTO()));

        List<ProductWithImageAbstractDTO> result = productService.getAbstractProductsForMerchant(List.of(1001L, 1002L));

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("PR-031 - 批量查询含无效ID（空列表返回）")
    void testGetAbstractProductsForMerchantWithInvalidIds() {
        when(productMapper.selectAbstractProductsByIdsJustMerchant(List.of(1001L, 99999L))).thenReturn(List.of());

        List<ProductWithImageAbstractDTO> result = productService.getAbstractProductsForMerchant(List.of(1001L, 99999L));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("PR-080 - 雪花ID生成后不为空")
    void testCreateProductGeneratesSnowflakeId() {
        when(productMapper.insertProduct(any(Product.class))).thenReturn(1);

        Product product = new Product();
        product.setName("ID测试");
        int result = productService.createProduct(product);

        assertEquals(1, result);
        verify(productMapper).insertProduct(productCaptor.capture());
        assertNotNull(productCaptor.getValue().getId());
        assertTrue(productCaptor.getValue().getId() > 0);
    }
}
