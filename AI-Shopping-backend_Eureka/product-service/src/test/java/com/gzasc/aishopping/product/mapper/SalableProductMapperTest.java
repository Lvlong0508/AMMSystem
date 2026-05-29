package com.gzasc.aishopping.product.mapper;

import com.gzasc.aishopping.product.model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/eureka_product?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.datasource.username=root",
    "spring.datasource.password=123456",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("SalableProductMapper 集成测试")
class SalableProductMapperTest {

    @Autowired
    private SalableProductMapper salableProductMapper;

    @Autowired
    private ProductMapper productMapper;

    private Long uniqueId() {
        return System.nanoTime();
    }

    private String insertProduct() {
        Long id = uniqueId();
        Product p = new Product();
        p.setId(id);
        p.setName("salable-product-" + id);
        p.setPrice(BigDecimal.valueOf(99.99));
        p.setTags("test");
        p.setDescription("test");
        p.setStock(100);
        p.setSale(true);
        p.setShopId(100L);
        productMapper.insertProduct(p);
        return String.valueOf(id);
    }

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("添加可售商品")
        void addSalable_shouldInsert() {
            String pid = insertProduct();
            int affected = salableProductMapper.addSalable(pid);
            assertThat(affected).isEqualTo(1);
        }

        @Test
        @DisplayName("重复添加可售商品应抛异常")
        void addSalable_duplicate_shouldThrow() {
            String pid = insertProduct();
            salableProductMapper.addSalable(pid);
            org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> salableProductMapper.addSalable(pid));
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("判断商品是否可售-存在")
        void isSalable_shouldReturnTrue() {
            String pid = insertProduct();
            salableProductMapper.addSalable(pid);
            assertThat(salableProductMapper.isSalable(pid)).isTrue();
        }

        @Test
        @DisplayName("判断商品是否可售-不存在")
        void isSalable_notFound_shouldReturnFalse() {
            assertThat(salableProductMapper.isSalable("999999")).isFalse();
        }

        @Test
        @DisplayName("查询所有可售商品ID")
        void selectAll_shouldReturnIds() {
            String pid = insertProduct();
            salableProductMapper.addSalable(pid);
            List<String> list = salableProductMapper.selectAll(0);
            assertThat(list).isNotEmpty();
            assertThat(list).contains(pid);
        }

        @Test
        @DisplayName("查询可售商品超出偏移量返回空列表")
        void selectAll_offsetTooLarge_shouldReturnEmpty() {
            List<String> list = salableProductMapper.selectAll(99999);
            assertThat(list).isEmpty();
        }
    }

    @Nested
    @DisplayName("删除操作")
    class DeleteTests {

        @Test
        @DisplayName("移除可售商品")
        void removeSalable_shouldDelete() {
            String pid = insertProduct();
            salableProductMapper.addSalable(pid);
            int affected = salableProductMapper.removeSalable(pid);
            assertThat(affected).isEqualTo(1);
            assertThat(salableProductMapper.isSalable(pid)).isFalse();
        }

        @Test
        @DisplayName("移除不存在的可售商品返回0")
        void removeSalable_notFound_shouldReturnZero() {
            int affected = salableProductMapper.removeSalable("999999");
            assertThat(affected).isEqualTo(0);
        }
    }
}
