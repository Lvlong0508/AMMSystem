package com.gzasc.aishopping.product.mapper;

import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductReservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
@DisplayName("ProductReservationMapper 集成测试")
class ProductReservationMapperTest {

    @Autowired
    private ProductReservationMapper reservationMapper;

    @Autowired
    private ProductMapper productMapper;

    private String uniqueProductId() {
        return String.valueOf(System.nanoTime());
    }

    private String uniqueOrderId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void insertProduct(String productId, int stock) {
        Product p = new Product();
        p.setId(Long.parseLong(productId));
        p.setName("reservation-product");
        p.setPrice(BigDecimal.valueOf(99.99));
        p.setTags("test");
        p.setDescription("test");
        p.setStock(stock);
        p.setSale(true);
        p.setShopId(100L);
        productMapper.insertProduct(p);
    }

    private ProductReservation buildReservation(String productId, String orderId, int qty, String status, Date expiredAt) {
        ProductReservation r = new ProductReservation();
        r.setProductId(productId);
        r.setOrderId(orderId);
        r.setQuantity(qty);
        r.setStatus(status);
        r.setCreatedAt(new Date());
        r.setExpiredAt(expiredAt);
        return r;
    }

    private ProductReservation insertAndReturn(String productId, String orderId, int qty, String status, Date expiredAt) {
        ProductReservation r = buildReservation(productId, orderId, qty, status, expiredAt);
        reservationMapper.insertReservation(r);
        return r;
    }

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入库存预占记录")
        void insertReservation_shouldInsert() {
            String pid = uniqueProductId();
            String oid = uniqueOrderId();
            insertProduct(pid, 10);
            ProductReservation r = buildReservation(pid, oid, 2, ProductReservation.RESERVED,
                Date.from(LocalDateTime.now().plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant()));
            int affected = reservationMapper.insertReservation(r);
            assertThat(affected).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据订单ID查询预占记录")
        void selectByOrderId_shouldReturnReservation() {
            String pid = uniqueProductId();
            String oid = uniqueOrderId();
            insertProduct(pid, 10);
            insertAndReturn(pid, oid, 2, ProductReservation.RESERVED,
                Date.from(LocalDateTime.now().plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant()));

            ProductReservation found = reservationMapper.selectByOrderId(oid);
            assertThat(found).isNotNull();
            assertThat(found.getQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("查询不存在的订单预占返回null")
        void selectByOrderId_notFound_shouldReturnNull() {
            ProductReservation found = reservationMapper.selectByOrderId("non-existent-order");
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("查询已过期的预占记录")
        void selectExpiredReservations_shouldReturnExpired() {
            String pid = uniqueProductId();
            String oid = uniqueOrderId();
            insertProduct(pid, 10);
            insertAndReturn(pid, oid, 2, ProductReservation.RESERVED,
                Date.from(LocalDateTime.now().minusMinutes(5).atZone(ZoneId.systemDefault()).toInstant()));

            List<ProductReservation> expired = reservationMapper.selectExpiredReservations(new Date());
            assertThat(expired).isNotEmpty();
        }

        @Test
        @DisplayName("查询扣减库存-带锁")
        void selectProductStockForUpdate_shouldReturnStock() {
            String pid = uniqueProductId();
            insertProduct(pid, 15);
            int stock = reservationMapper.selectProductStockForUpdate(pid);
            assertThat(stock).isEqualTo(15);
        }

        @Test
        @DisplayName("查询已预占库存总和-带锁")
        void sumReservedQty_shouldReturnSum() {
            String pid = uniqueProductId();
            String oid1 = uniqueOrderId(), oid2 = uniqueOrderId();
            insertProduct(pid, 20);
            Date future = Date.from(LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant());
            insertAndReturn(pid, oid1, 3, ProductReservation.RESERVED, future);
            insertAndReturn(pid, oid2, 5, ProductReservation.RESERVED, future);

            int sum = reservationMapper.sumReservedQty(pid);
            assertThat(sum).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("更新操作")
    class UpdateTests {

        @Test
        @DisplayName("确认预占-状态改为CONFIRMED")
        void confirmReservation_shouldUpdateStatus() {
            String pid = uniqueProductId();
            String oid = uniqueOrderId();
            insertProduct(pid, 10);
            insertAndReturn(pid, oid, 2, ProductReservation.RESERVED,
                Date.from(LocalDateTime.now().plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant()));

            int affected = reservationMapper.confirmReservation(oid);
            assertThat(affected).isEqualTo(1);

            ProductReservation updated = reservationMapper.selectByOrderId(oid);
            assertThat(updated.getStatus()).isEqualTo(ProductReservation.CONFIRMED);
        }

        @Test
        @DisplayName("确认已释放的预占返回0")
        void confirmReservation_alreadyReleased_shouldReturnZero() {
            String pid = uniqueProductId();
            String oid = uniqueOrderId();
            insertProduct(pid, 10);
            insertAndReturn(pid, oid, 2, ProductReservation.RESERVED,
                Date.from(LocalDateTime.now().plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant()));
            reservationMapper.releaseReservation(oid);

            int affected = reservationMapper.confirmReservation(oid);
            assertThat(affected).isEqualTo(0);
        }

        @Test
        @DisplayName("释放预占-状态改为RELEASED")
        void releaseReservation_shouldUpdateStatus() {
            String pid = uniqueProductId();
            String oid = uniqueOrderId();
            insertProduct(pid, 10);
            insertAndReturn(pid, oid, 2, ProductReservation.RESERVED,
                Date.from(LocalDateTime.now().plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant()));

            int affected = reservationMapper.releaseReservation(oid);
            assertThat(affected).isEqualTo(1);

            ProductReservation updated = reservationMapper.selectByOrderId(oid);
            assertThat(updated.getStatus()).isEqualTo(ProductReservation.RELEASED);
        }

        @Test
        @DisplayName("释放不存在的预占返回0")
        void releaseReservation_notFound_shouldReturnZero() {
            int affected = reservationMapper.releaseReservation("non-existent-order");
            assertThat(affected).isEqualTo(0);
        }

        @Test
        @DisplayName("扣减商品库存-足够库存")
        void deductProductStock_shouldDecrease() {
            String pid = uniqueProductId();
            insertProduct(pid, 10);
            int affected = reservationMapper.deductProductStock(pid, 3);
            assertThat(affected).isEqualTo(1);

            int stock = reservationMapper.selectProductStockForUpdate(pid);
            assertThat(stock).isEqualTo(7);
        }

        @Test
        @DisplayName("扣减商品库存-库存不足应失败")
        void deductProductStock_insufficient_shouldFail() {
            String pid = uniqueProductId();
            insertProduct(pid, 2);
            int affected = reservationMapper.deductProductStock(pid, 10);
            assertThat(affected).isEqualTo(0);
        }
    }
}
