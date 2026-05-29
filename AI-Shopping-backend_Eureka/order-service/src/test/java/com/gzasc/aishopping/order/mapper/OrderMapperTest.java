package com.gzasc.aishopping.order.mapper;

import com.gzasc.aishopping.order.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("OrderMapper 集成测试")
class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    private static String nextOrderId() {
        return "ORD" + System.nanoTime();
    }

    private static Order createTestOrder(String orderId) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(1001L);
        order.setShopId("SHOP001");
        order.setProductId("PROD001");
        order.setQuantity(2);
        order.setTotalPrice(99.99);
        order.setOrderStatus(Order.PENDING);
        order.setOrderDate(new Timestamp(System.currentTimeMillis()));
        return order;
    }

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入订单成功")
        void insertOrder_shouldSucceed() {
            Order order = createTestOrder(nextOrderId());
            int affected = orderMapper.insertOrder(order);
            assertThat(affected).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据ID查询订单")
        void selectOrderById_shouldReturnOrder() {
            String orderId = nextOrderId();
            orderMapper.insertOrder(createTestOrder(orderId));

            Order found = orderMapper.selectOrderById(orderId);

            assertThat(found).isNotNull();
            assertThat(found.getOrderId()).isEqualTo(orderId);
            assertThat(found.getUserId()).isEqualTo(1001L);
            assertThat(found.getOrderStatus()).isEqualTo(Order.PENDING);
        }

        @Test
        @DisplayName("查询不存在的订单返回null")
        void selectOrderById_notFound_shouldReturnNull() {
            Order found = orderMapper.selectOrderById("NONEXIST");
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("根据用户ID查询订单列表")
        void selectAbstractOrdersByUserId_shouldReturnOrders() {
            String orderId = nextOrderId();
            orderMapper.insertOrder(createTestOrder(orderId));

            List<Order> orders = orderMapper.selectAbstractOrdersByUserId(1001L);

            assertThat(orders).isNotEmpty();
            assertThat(orders.get(0).getOrderId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("查询无订单的用户返回空列表")
        void selectAbstractOrdersByUserId_noOrder_shouldReturnEmpty() {
            List<Order> orders = orderMapper.selectAbstractOrdersByUserId(9999L);
            assertThat(orders).isEmpty();
        }

        @Test
        @DisplayName("根据用户ID和订单ID查询订单详情")
        void selectOrderDetailByUser_shouldReturnOrder() {
            String orderId = nextOrderId();
            orderMapper.insertOrder(createTestOrder(orderId));

            Order found = orderMapper.selectOrderDetailByUser(1001L, orderId);

            assertThat(found).isNotNull();
            assertThat(found.getOrderId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("查询不存在的用户-订单详情返回null")
        void selectOrderDetailByUser_notFound_shouldReturnNull() {
            Order found = orderMapper.selectOrderDetailByUser(9999L, "NONEXIST");
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("根据店铺ID查询订单列表")
        void selectAbstractOrdersByShopId_shouldReturnOrders() {
            String orderId = nextOrderId();
            orderMapper.insertOrder(createTestOrder(orderId));

            List<Order> orders = orderMapper.selectAbstractOrdersByShopId("SHOP001");

            assertThat(orders).isNotEmpty();
            assertThat(orders.get(0).getOrderId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("查询无订单的店铺返回空列表")
        void selectAbstractOrdersByShopId_noOrder_shouldReturnEmpty() {
            List<Order> orders = orderMapper.selectAbstractOrdersByShopId("NONEXIST_SHOP");
            assertThat(orders).isEmpty();
        }

        @Test
        @DisplayName("根据店铺ID和订单ID查询订单详情")
        void selectOrderDetailByShop_shouldReturnOrder() {
            String orderId = nextOrderId();
            orderMapper.insertOrder(createTestOrder(orderId));

            Order found = orderMapper.selectOrderDetailByShop("SHOP001", orderId);

            assertThat(found).isNotNull();
            assertThat(found.getOrderId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("查询不存在的店铺-订单详情返回null")
        void selectOrderDetailByShop_notFound_shouldReturnNull() {
            Order found = orderMapper.selectOrderDetailByShop("NONEXIST_SHOP", "NONEXIST");
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("查询过期待支付订单")
        void selectExpiredPendingOrders_shouldReturnExpiredOrders() {
            String orderId = nextOrderId();
            Order order = createTestOrder(orderId);
            order.setOrderDate(Timestamp.valueOf("2024-01-01 00:00:00"));
            orderMapper.insertOrder(order);

            List<Order> expired = orderMapper.selectExpiredPendingOrders(1);

            assertThat(expired).isNotEmpty();
            assertThat(expired.get(0).getOrderId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("无过期订单时返回空列表")
        void selectExpiredPendingOrders_noExpired_shouldReturnEmpty() {
            List<Order> expired = orderMapper.selectExpiredPendingOrders(0);
            assertThat(expired).isEmpty();
        }
    }

    @Nested
    @DisplayName("更新操作")
    class UpdateTests {

        @Test
        @DisplayName("更新订单状态")
        void updateOrderStatus_shouldUpdate() {
            String orderId = nextOrderId();
            orderMapper.insertOrder(createTestOrder(orderId));

            int affected = orderMapper.updateOrderStatus(orderId, Order.PAID);

            assertThat(affected).isEqualTo(1);
            Order updated = orderMapper.selectOrderById(orderId);
            assertThat(updated.getOrderStatus()).isEqualTo(Order.PAID);
        }

        @Test
        @DisplayName("CAS更新订单状态成功")
        void updateOrderStatusCas_success() {
            String orderId = nextOrderId();
            orderMapper.insertOrder(createTestOrder(orderId));

            int affected = orderMapper.updateOrderStatusCas(orderId, Order.PAID, Order.PENDING);

            assertThat(affected).isEqualTo(1);
            Order updated = orderMapper.selectOrderById(orderId);
            assertThat(updated.getOrderStatus()).isEqualTo(Order.PAID);
        }

        @Test
        @DisplayName("CAS更新订单状态失败（oldStatus不匹配）")
        void updateOrderStatusCas_fail() {
            String orderId = nextOrderId();
            orderMapper.insertOrder(createTestOrder(orderId));

            int affected = orderMapper.updateOrderStatusCas(orderId, Order.PAID, Order.SHIPPED);

            assertThat(affected).isEqualTo(0);
            Order unchanged = orderMapper.selectOrderById(orderId);
            assertThat(unchanged.getOrderStatus()).isEqualTo(Order.PENDING);
        }

        @Test
        @DisplayName("CAS Multi更新订单状态成功")
        void updateOrderStatusCasMulti_success() {
            String orderId = nextOrderId();
            orderMapper.insertOrder(createTestOrder(orderId));

            int affected = orderMapper.updateOrderStatusCasMulti(
                    orderId, Order.PAID, List.of(Order.PENDING, Order.CANCELLED));

            assertThat(affected).isEqualTo(1);
            Order updated = orderMapper.selectOrderById(orderId);
            assertThat(updated.getOrderStatus()).isEqualTo(Order.PAID);
        }

        @Test
        @DisplayName("CAS Multi更新订单状态失败（expectedStatuses不匹配）")
        void updateOrderStatusCasMulti_fail() {
            String orderId = nextOrderId();
            orderMapper.insertOrder(createTestOrder(orderId));

            int affected = orderMapper.updateOrderStatusCasMulti(
                    orderId, Order.PAID, List.of(Order.SHIPPED, Order.DELIVERED));

            assertThat(affected).isEqualTo(0);
            Order unchanged = orderMapper.selectOrderById(orderId);
            assertThat(unchanged.getOrderStatus()).isEqualTo(Order.PENDING);
        }
    }

    @Nested
    @DisplayName("删除操作")
    class DeleteTests {

        @Test
        @DisplayName("删除订单成功")
        void deleteOrderById_shouldDelete() {
            String orderId = nextOrderId();
            orderMapper.insertOrder(createTestOrder(orderId));

            int affected = orderMapper.deleteOrderById(orderId);

            assertThat(affected).isEqualTo(1);
            assertThat(orderMapper.selectOrderById(orderId)).isNull();
        }

        @Test
        @DisplayName("删除不存在的订单返回0")
        void deleteOrderById_notFound_shouldReturnZero() {
            int affected = orderMapper.deleteOrderById("NONEXIST");
            assertThat(affected).isEqualTo(0);
        }
    }
}
