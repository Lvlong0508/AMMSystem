package com.gzasc.aishopping.product.mapper;

import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;
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
    "spring.cloud.config.enabled=false",
    "spring.cloud.nacos.config.import-check.enabled=false",
    "eureka.client.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("ProductMapper 集成测试")
class ProductMapperTest {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductImageInfoMapper productImageInfoMapper;

    private Long uniqueId() {
        return System.nanoTime();
    }

    private Product buildProduct(Long id, String name, BigDecimal price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(price);
        p.setTags("test,unit");
        p.setDescription("test description");
        p.setStock(stock);
        p.setSale(true);
        p.setShopId(100L);
        return p;
    }

    private Product insertAndReturn(Product product) {
        productMapper.insertProduct(product);
        return product;
    }

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入商品")
        void insertProduct_shouldInsert() {
            Product p = buildProduct(uniqueId(), "insert-test", BigDecimal.valueOf(99.99), 10);
            int affected = productMapper.insertProduct(p);
            assertThat(affected).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据ID查询商品")
        void selectById_shouldReturnProduct() {
            Long id = uniqueId();
            insertAndReturn(buildProduct(id, "select-by-id", BigDecimal.valueOf(50), 5));
            Product found = productMapper.selectProductById(id);
            assertThat(found).isNotNull();
            assertThat(found.getName()).isEqualTo("select-by-id");
            assertThat(found.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(50));
        }

        @Test
        @DisplayName("查询不存在的商品返回null")
        void selectById_notFound_shouldReturnNull() {
            Product found = productMapper.selectProductById(999999999999L);
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("根据名称模糊查询")
        void selectByName_shouldReturnMatchingProducts() {
            Long id = uniqueId();
            insertAndReturn(buildProduct(id, "模糊查询test", BigDecimal.valueOf(30), 3));
            List<Product> list = productMapper.selectProductsByName("模糊查询");
            assertThat(list).isNotEmpty();
            assertThat(list).anyMatch(p -> p.getId().equals(id));
        }

        @Test
        @DisplayName("模糊查询无结果返回空列表")
        void selectByName_noMatch_shouldReturnEmpty() {
            List<Product> list = productMapper.selectProductsByName("不存在的商品名称_x1y2z3");
            assertThat(list).isEmpty();
        }

        @Test
        @DisplayName("根据价格区间分页查询")
        void selectByPriceRangeWithPage_shouldReturnProducts() {
            Long id = uniqueId();
            insertAndReturn(buildProduct(id, "page-test", BigDecimal.valueOf(150), 5));
            List<Product> list = productMapper.selectByPriceRangeWithPage(BigDecimal.valueOf(100), BigDecimal.valueOf(200), 0);
            assertThat(list).isNotEmpty();
        }

        @Test
        @DisplayName("根据ID集合查询抽象信息")
        void selectAbstractByIds_shouldReturnAbstract() {
            Long id1 = uniqueId(), id2 = uniqueId();
            insertAndReturn(buildProduct(id1, "abstract-1", BigDecimal.valueOf(10), 2));
            insertAndReturn(buildProduct(id2, "abstract-2", BigDecimal.valueOf(20), 3));
            List<Product> list = productMapper.selectAbstractProductsByIds(List.of(id1, id2));
            assertThat(list).hasSize(2);
        }

        @Test
        @DisplayName("商家端抽象信息查询")
        void selectAbstractJustMerchant_shouldReturnAbstract() {
            Long id1 = uniqueId(), id2 = uniqueId();
            insertAndReturn(buildProduct(id1, "merchant-1", BigDecimal.valueOf(15), 4));
            insertAndReturn(buildProduct(id2, "merchant-2", BigDecimal.valueOf(25), 5));
            List<Product> list = productMapper.selectAbstractProductsByIdsJustMerchant(List.of(id1, id2));
            assertThat(list).hasSize(2);
        }

        @Test
        @DisplayName("用户端名称查询不返回下架商品")
        void selectByName_shouldExcludeUnsaleProducts() {
            Long saleId = uniqueId();
            Long unsaleId = uniqueId();
            insertAndReturn(buildProduct(saleId, "过滤测试商品", BigDecimal.valueOf(30), 3));
            Product unsale = buildProduct(unsaleId, "过滤测试商品", BigDecimal.valueOf(30), 3);
            unsale.setSale(false);
            insertAndReturn(unsale);

            List<Product> list = productMapper.selectProductsByName("过滤测试商品");

            assertThat(list).anyMatch(p -> p.getId().equals(saleId));
            assertThat(list).noneMatch(p -> p.getId().equals(unsaleId));
        }

        @Test
        @DisplayName("用户端价格查询不返回下架商品")
        void selectByPriceRangeWithPage_shouldExcludeUnsaleProducts() {
            Long saleId = uniqueId();
            Long unsaleId = uniqueId();
            insertAndReturn(buildProduct(saleId, "price-sale", BigDecimal.valueOf(150), 5));
            Product unsale = buildProduct(unsaleId, "price-unsale", BigDecimal.valueOf(150), 5);
            unsale.setSale(false);
            insertAndReturn(unsale);

            List<Product> list = productMapper.selectByPriceRangeWithPage(BigDecimal.valueOf(100), BigDecimal.valueOf(200), 0);

            assertThat(list).anyMatch(p -> p.getId().equals(saleId));
            assertThat(list).noneMatch(p -> p.getId().equals(unsaleId));
        }

        @Test
        @DisplayName("用户端卡片分页只返回上架商品")
        void selectCardProductsPage_shouldReturnOnlySaleProducts() {
            Long saleId = uniqueId();
            Long unsaleId = uniqueId();
            insertAndReturn(buildProduct(saleId, "card-sale", BigDecimal.valueOf(88), 8));
            Product unsale = buildProduct(unsaleId, "card-unsale", BigDecimal.valueOf(88), 8);
            unsale.setSale(false);
            insertAndReturn(unsale);

            List<Product> list = productMapper.selectCardProductsPage(0, 200);

            assertThat(list).anyMatch(p -> p.getId().equals(saleId));
            assertThat(list).noneMatch(p -> p.getId().equals(unsaleId));
        }

        @Test
        @DisplayName("用户端店铺查询只返回上架商品")
        void selectSalableByShopId_shouldReturnOnlySaleProducts() {
            Long saleId = uniqueId();
            Long unsaleId = uniqueId();
            Product sale = buildProduct(saleId, "shop-sale", BigDecimal.valueOf(66), 6);
            sale.setShopId(98765L);
            insertAndReturn(sale);
            Product unsale = buildProduct(unsaleId, "shop-unsale", BigDecimal.valueOf(66), 6);
            unsale.setShopId(98765L);
            unsale.setSale(false);
            insertAndReturn(unsale);

            List<Product> list = productMapper.selectSalableByShopId(98765L);

            assertThat(list).anyMatch(p -> p.getId().equals(saleId));
            assertThat(list).noneMatch(p -> p.getId().equals(unsaleId));
        }

        @Test
        @DisplayName("用户端批量抽象查询只返回上架商品")
        void selectAbstractProductsByIds_shouldExcludeUnsaleProducts() {
            Long saleId = uniqueId();
            Long unsaleId = uniqueId();
            insertAndReturn(buildProduct(saleId, "abstract-sale", BigDecimal.valueOf(10), 2));
            Product unsale = buildProduct(unsaleId, "abstract-unsale", BigDecimal.valueOf(20), 3);
            unsale.setSale(false);
            insertAndReturn(unsale);

            List<Product> list = productMapper.selectAbstractProductsByIds(List.of(saleId, unsaleId));

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getId()).isEqualTo(saleId);
        }

        @Test
        @DisplayName("商家端店铺查询仍返回上下架全部商品")
        void selectByShopId_shouldReturnSaleAndUnsaleProductsForMerchant() {
            Long saleId = uniqueId();
            Long unsaleId = uniqueId();
            Product sale = buildProduct(saleId, "merchant-shop-sale", BigDecimal.valueOf(66), 6);
            sale.setShopId(87654L);
            insertAndReturn(sale);
            Product unsale = buildProduct(unsaleId, "merchant-shop-unsale", BigDecimal.valueOf(66), 6);
            unsale.setShopId(87654L);
            unsale.setSale(false);
            insertAndReturn(unsale);

            List<Product> list = productMapper.selectByShopId(87654L);

            assertThat(list).anyMatch(p -> p.getId().equals(saleId));
            assertThat(list).anyMatch(p -> p.getId().equals(unsaleId));
        }

    }

    @Nested
    @DisplayName("更新操作")
    class UpdateTests {

        @Test
        @DisplayName("恢复库存")
        void restoreStock_shouldIncreaseStock() {
            Long id = uniqueId();
            insertAndReturn(buildProduct(id, "restore-test", BigDecimal.valueOf(100), 5));
            int affected = productMapper.restoreStock(id, 3);
            assertThat(affected).isEqualTo(1);
            Product updated = productMapper.selectProductById(id);
            assertThat(updated.getStock()).isEqualTo(8);
        }

        @Test
        @DisplayName("更新上架状态为下架")
        void updateSaleStatus_shouldSetNotSale() {
            Long id = uniqueId();
            insertAndReturn(buildProduct(id, "sale-status-test", BigDecimal.valueOf(100), 5));
            int affected = productMapper.updateSaleStatus(id, false);
            assertThat(affected).isEqualTo(1);
            Product updated = productMapper.selectProductById(id);
            assertThat(updated.isSale()).isFalse();
        }

        @Test
        @DisplayName("更新商品信息")
        void updateProduct_shouldUpdateAllFields() {
            Long id = uniqueId();
            Product p = insertAndReturn(buildProduct(id, "原名称", BigDecimal.valueOf(100), 5));
            p.setName("新名称");
            p.setPrice(BigDecimal.valueOf(200));
            p.setTags("new,tag");
            p.setDescription("new desc");
            p.setStock(10);
            p.setSale(false);
            p.setShopId(300L);

            int affected = productMapper.updateProduct(p);
            assertThat(affected).isEqualTo(1);

            Product updated = productMapper.selectProductById(id);
            assertThat(updated.getName()).isEqualTo("新名称");
            assertThat(updated.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(200));
            assertThat(updated.getStock()).isEqualTo(10);
            assertThat(updated.isSale()).isFalse();
        }

        @Test
        @DisplayName("updateProduct_shouldUpdatePartialFields")
        void updateProduct_shouldUpdatePartialFields() {
            Long id = uniqueId();
            insertAndReturn(buildProduct(id, "原名称", BigDecimal.valueOf(100), 5));

            Product update = new Product();
            update.setId(id);
            update.setName("新名称");
            update.setPrice(BigDecimal.valueOf(99));

            int rows = productMapper.updateProduct(update);
            assertThat(rows).isEqualTo(1);

            Product reloaded = productMapper.selectProductById(id);
            assertThat(reloaded.getName()).isEqualTo("新名称");
            assertThat(reloaded.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(99));
            assertThat(reloaded.getStock()).isNotNull();
            assertThat(reloaded.getDescription()).isNotNull();
        }

    }

    @Nested
    @DisplayName("删除操作")
    class DeleteTests {

        @Test
        @DisplayName("删除商品")
        void deleteProduct_shouldDelete() {
            Long id = uniqueId();
            insertAndReturn(buildProduct(id, "delete-test", BigDecimal.valueOf(50), 3));
            int affected = productMapper.deleteProduct(id);
            assertThat(affected).isEqualTo(1);
            assertThat(productMapper.selectProductById(id)).isNull();
        }

        @Test
        @DisplayName("删除不存在的商品返回0")
        void deleteProduct_notFound_shouldReturnZero() {
            int affected = productMapper.deleteProduct(999999999L);
            assertThat(affected).isEqualTo(0);
        }
    }
}
