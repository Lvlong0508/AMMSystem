package com.gzasc.aishopping.product.converter;

import com.gzasc.aishopping.product.dto.ProductDTO;
import com.gzasc.aishopping.product.model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProductConverterTest {

    private final ProductConverter converter = new ProductConverter();

    private Product createProduct() {
        Product p = new Product();
        p.setId(1L);
        p.setName("测试商品");
        p.setPrice(BigDecimal.valueOf(99.99));
        p.setTags("手机,数码");
        p.setDescription("描述");
        p.setStock(100);
        p.setSale(true);
        p.setImageId(10);
        p.setShopId(100L);
        return p;
    }

    @Test
    @DisplayName("toDTO(Product) - 基础转换")
    void testToDTO() {
        ProductDTO dto = converter.toDTO(createProduct());

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("测试商品", dto.getName());
        assertEquals(BigDecimal.valueOf(99.99), dto.getPrice());
    }

    @Test
    @DisplayName("toDTO(Product) - 传入null返回null")
    void testToDTOWithNull() {
        assertNull(converter.toDTO((Product) null));
    }

    @Test
    @DisplayName("toDTO(Product, imageUrl) - 带图片URL")
    void testToDTOWithImageUrl() {
        ProductDTO dto = converter.toDTO(createProduct(), "http://img.test/1.jpg");

        assertEquals("http://img.test/1.jpg", dto.getImageUrl());
    }

    @Test
    @DisplayName("toDTOList - 正常转换列表")
    void testToDTOList() {
        List<ProductDTO> list = converter.toDTOList(
                List.of(createProduct(), createProduct()),
                Map.of(10, "http://img.test/1.jpg"));

        assertThat(list).hasSize(2);
        assertEquals("http://img.test/1.jpg", list.get(0).getImageUrl());
    }

    @Test
    @DisplayName("toDTOList - 传入null返回空列表")
    void testToDTOListWithNull() {
        assertThat(converter.toDTOList(null, null)).isEmpty();
    }
}
