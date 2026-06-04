package com.gzasc.aishopping.product.converter;

import com.gzasc.aishopping.product.model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

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
}
