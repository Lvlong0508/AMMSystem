package com.gzasc.aishopping.product.mapper;

import com.gzasc.aishopping.product.model.ProductImageInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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
@DisplayName("ProductImageInfoMapper 集成测试")
class ProductImageInfoMapperTest {

    @Autowired
    private ProductImageInfoMapper productImageInfoMapper;

    private ProductImageInfo buildImage(String url) {
        ProductImageInfo img = new ProductImageInfo();
        img.setUrl(url);
        return img;
    }

    private ProductImageInfo insertAndReturn(String url) {
        ProductImageInfo img = buildImage(url);
        productImageInfoMapper.insert(img);
        return img;
    }

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入图片并返回自增ID")
        void insert_shouldReturnGeneratedId() {
            ProductImageInfo img = buildImage("http://test.com/insert.jpg");
            int affected = productImageInfoMapper.insert(img);
            assertThat(affected).isEqualTo(1);
            assertThat(img.getId()).isPositive();
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据ID查询图片URL")
        void selectURLById_shouldReturnImage() {
            ProductImageInfo img = insertAndReturn("http://test.com/select.jpg");
            ProductImageInfo found = productImageInfoMapper.selectURLById(img.getId());
            assertThat(found).isNotNull();
            assertThat(found.getUrl()).isEqualTo("http://test.com/select.jpg");
        }

        @Test
        @DisplayName("查询不存在的图片返回null")
        void selectURLById_notFound_shouldReturnNull() {
            ProductImageInfo found = productImageInfoMapper.selectURLById(99999);
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("根据ID集合查询多张图片")
        void selectByIds_shouldReturnImages() {
            ProductImageInfo img1 = insertAndReturn("http://test.com/img1.jpg");
            ProductImageInfo img2 = insertAndReturn("http://test.com/img2.jpg");
            List<ProductImageInfo> list = productImageInfoMapper.selectByIds(List.of(img1.getId(), img2.getId()));
            assertThat(list).hasSize(2);
        }

    }

    @Nested
    @DisplayName("更新操作")
    class UpdateTests {

        @Test
        @DisplayName("更新图片URL")
        void updateUrl_shouldUpdate() {
            ProductImageInfo img = insertAndReturn("http://test.com/old.jpg");
            img.setUrl("http://test.com/new.jpg");

            int affected = productImageInfoMapper.updateUrl(img);
            assertThat(affected).isEqualTo(1);

            ProductImageInfo updated = productImageInfoMapper.selectURLById(img.getId());
            assertThat(updated.getUrl()).isEqualTo("http://test.com/new.jpg");
        }
    }

    @Nested
    @DisplayName("删除操作")
    class DeleteTests {

        @Test
        @DisplayName("删除图片")
        void deleteById_shouldDelete() {
            ProductImageInfo img = insertAndReturn("http://test.com/delete.jpg");
            int affected = productImageInfoMapper.deleteById(img.getId());
            assertThat(affected).isEqualTo(1);
            assertThat(productImageInfoMapper.selectURLById(img.getId())).isNull();
        }

        @Test
        @DisplayName("删除不存在的图片返回0")
        void deleteById_notFound_shouldReturnZero() {
            int affected = productImageInfoMapper.deleteById(99999);
            assertThat(affected).isEqualTo(0);
        }
    }
}
