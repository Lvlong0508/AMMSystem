package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.common.dto.product.ProductCardDTO;
import com.gzasc.aishopping.common.feign.shop.ShopFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.product.converter.ProductConverter;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.mapper.ProductImageInfoMapper;
import com.gzasc.aishopping.product.mapper.ProductMapper;
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
    @DisplayName("PR-CARD-001 - getSalableProductCards 有数据")
    void testGetSalableProductCardsWithData() {
        Product p1 = new Product(); p1.setId(1L); p1.setImageId(1); p1.setStock(10);
        Product p2 = new Product(); p2.setId(2L); p2.setImageId(2); p2.setStock(5);
        when(productMapper.selectCardProductsPage(0, 20)).thenReturn(List.of(p1, p2));
        when(productImageInfoMapper.selectByIds(anyList())).thenReturn(List.of());
        when(productConverter.toCardDTOList(anyList(), anyMap())).thenReturn(List.of(new ProductCardDTO(), new ProductCardDTO()));

        List<ProductCardDTO> result = productService.getSalableProductCards(0);

        assertEquals(2, result.size());
        verify(productMapper).selectCardProductsPage(0, 20);
    }

    @Test
    @DisplayName("PR-CARD-002 - getSalableProductCards 无数据")
    void testGetSalableProductCardsEmpty() {
        when(productMapper.selectCardProductsPage(0, 20)).thenReturn(List.of());
        List<ProductCardDTO> result = productService.getSalableProductCards(0);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("PR-CARD-003 - getSalableProductCards page为负数抛出异常")
    void testGetSalableProductCardsNegativePage() {
        ProductException ex = assertThrows(ProductException.class,
                () -> productService.getSalableProductCards(-1));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("负数"));
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
    @DisplayName("PR-020 - 更新商品名称和价格（含新图片）")
    void testUpdateProductWithImageSuccess() {
        Product existing = new Product();
        existing.setId(2001L);
        existing.setName("旧名称");
        existing.setImageId(1);
        when(productMapper.selectProductById(2001L)).thenReturn(existing);
        when(imageStorageService.saveImage(anyLong(), any(MultipartFile.class)))
                .thenReturn("/image/goods/main/2001/2001_abc.jpg");
        when(productImageInfoMapper.selectURLById(1)).thenReturn(new ProductImageInfo(1, "http://localhost:8081/image/goods/main/2001/old.jpg"));
        when(productImageInfoMapper.updateUrl(any(ProductImageInfo.class))).thenReturn(1);
        when(productMapper.updateProduct(any(Product.class))).thenReturn(1);

        Product update = new Product();
        update.setId(2001L);
        update.setName("新名称");
        update.setPrice(BigDecimal.valueOf(199));
        MockMultipartFile imageFile = new MockMultipartFile("image", "new.jpg", "image/jpeg", "new-image-content".getBytes());

        int result = productService.updateProductWithImage(update, imageFile);

        assertEquals(1, result);
        verify(imageStorageService).saveImage(eq(2001L), any(MultipartFile.class));
        verify(productImageInfoMapper).updateUrl(any(ProductImageInfo.class));
        verify(productMapper).updateProduct(any(Product.class));
        verify(imageStorageService).deleteImage("http://localhost:8081/image/goods/main/2001/old.jpg");
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

        boolean result = productService.listProduct(7001L);

        assertTrue(result);
        verify(productMapper).updateSaleStatus(7001L, true);
    }

    @Test
    @DisplayName("PR-063 - 下架商品")
    void testUnlistProduct() {
        Product product = new Product();
        product.setId(7002L);
        product.setSale(true);
        when(productMapper.selectProductById(7002L)).thenReturn(product);
        when(productMapper.updateSaleStatus(7002L, false)).thenReturn(1);

        boolean result = productService.unlistProduct(7002L);

        assertTrue(result);
        verify(productMapper).updateSaleStatus(7002L, false);
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
    @DisplayName("B1 - updateProductWithImage 传入新 image 且原商品无图片时，imageId 应正确关联")
    void testUpdateProductWithNewImage_ExistingProductWithoutImage() {
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setShopId(100L);
        existingProduct.setName("测试商品");
        existingProduct.setPrice(BigDecimal.valueOf(99.00));
        existingProduct.setStock(10);

        when(productMapper.selectProductById(1L)).thenReturn(existingProduct);
        when(imageStorageService.saveImage(anyLong(), any(MultipartFile.class)))
                .thenReturn("/image/goods/main/1/1_new.jpg");

        doAnswer(invocation -> {
            ProductImageInfo arg = invocation.getArgument(0);
            arg.setId(999);
            return 1;
        }).when(productImageInfoMapper).insert(any());

        when(productMapper.updateProduct(any())).thenReturn(1);

        Product updateData = new Product();
        updateData.setId(1L);
        updateData.setName("更新后");
        MockMultipartFile imageFile = new MockMultipartFile("image", "new.jpg", "image/jpeg", "content".getBytes());

        int result = productService.updateProductWithImage(updateData, imageFile);

        ArgumentCaptor<ProductImageInfo> imageCaptor = ArgumentCaptor.forClass(ProductImageInfo.class);
        verify(productImageInfoMapper).insert(imageCaptor.capture());
        assertEquals("http://localhost:8081/image/goods/main/1/1_new.jpg", imageCaptor.getValue().getUrl());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateProduct(productCaptor.capture());
        assertNotNull(productCaptor.getValue().getImageId());
        assertEquals(Integer.valueOf(999), productCaptor.getValue().getImageId());

        verify(imageStorageService, never()).deleteImage(anyString());

        assertEquals(1, result);
    }

    @Test
    @DisplayName("updateProduct - 纯文本更新不传图片，保留原图")
    void testUpdateProductWithoutImage() {
        Product existing = new Product();
        existing.setId(3001L);
        existing.setName("旧名称");
        existing.setImageId(5);
        when(productMapper.selectProductById(3001L)).thenReturn(existing);
        when(productMapper.updateProduct(any(Product.class))).thenReturn(1);

        Product update = new Product();
        update.setId(3001L);
        update.setName("新名称");
        update.setPrice(BigDecimal.valueOf(299));

        int result = productService.updateProductWithImage(update, null);

        assertEquals(1, result);
        verify(productMapper).updateProduct(productCaptor.capture());
        assertEquals(Integer.valueOf(5), productCaptor.getValue().getImageId());
        verify(imageStorageService, never()).saveImage(anyLong(), any(MultipartFile.class));
        verify(imageStorageService, never()).deleteImage(anyString());
    }
}
