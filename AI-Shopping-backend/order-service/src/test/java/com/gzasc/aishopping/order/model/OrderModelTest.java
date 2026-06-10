package com.gzasc.aishopping.order.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order/DeletedOrder 模型层单元测试")
class OrderModelTest {

    // ==================== Order.buildInitOrder ====================

    @Test
    @DisplayName("MD-001 buildInitOrder - 字段全部正确赋值")
    void buildInitOrder_allFields() {
        Timestamp before = new Timestamp(System.currentTimeMillis());
        Order order = Order.buildInitOrder("ORDER001", 100L, "SHOP001",
                "PROD001", 2, new BigDecimal("199.98"));
        Timestamp after = new Timestamp(System.currentTimeMillis());

        assertEquals("ORDER001", order.getOrderId());
        assertEquals(100L, order.getUserId());
        assertEquals("SHOP001", order.getShopId());
        assertEquals("PROD001", order.getProductId());
        assertEquals(2, order.getQuantity());
        assertEquals(new BigDecimal("199.98"), order.getTotalPrice());
        assertEquals(Order.PENDING, order.getOrderStatus());
        assertNotNull(order.getOrderDate());
        assertTrue(!order.getOrderDate().before(before) && !order.getOrderDate().after(after),
                "orderDate 应在调用前后时间之间");
        assertNull(order.getContactId());
    }

    @Test
    @DisplayName("MD-002 buildInitOrder - 初始状态固定为 PENDING")
    void buildInitOrder_statusAlwaysPending() {
        Order o1 = Order.buildInitOrder("A", 1L, "S", "P", 1, BigDecimal.ZERO);
        Order o2 = Order.buildInitOrder("B", 1L, "S", "P", 1, BigDecimal.ZERO);
        assertEquals("PENDING", o1.getOrderStatus());
        assertEquals("PENDING", o2.getOrderStatus());
    }

    @Test
    @DisplayName("MD-003 buildInitOrder - 数量为 0 时也允许创建")
    void buildInitOrder_zeroQuantity() {
        Order order = Order.buildInitOrder("A", 1L, "S", "P", 0, BigDecimal.ZERO);
        assertEquals(0, order.getQuantity());
        assertEquals(0, BigDecimal.ZERO.compareTo(order.getTotalPrice()));
    }

    @Test
    @DisplayName("MD-004 buildInitOrder - BigDecimal.ZERO 也能接受")
    void buildInitOrder_zeroPrice() {
        Order order = Order.buildInitOrder("A", 1L, "S", "P", 1, BigDecimal.ZERO);
        assertEquals(0, BigDecimal.ZERO.compareTo(order.getTotalPrice()));
    }

    @Test
    @DisplayName("MD-005 buildInitOrder - totalPrice 大数值精度保持")
    void buildInitOrder_bigDecimalPrecision() {
        BigDecimal precise = new BigDecimal("0.10").add(new BigDecimal("0.20"));
        Order order = Order.buildInitOrder("A", 1L, "S", "P", 1, precise);
        assertEquals(0, new BigDecimal("0.30").compareTo(order.getTotalPrice()));
    }

    // ==================== Order.canTransition 全状态机 ====================

    @Test
    @DisplayName("MD-006 canTransition - 所有合法状态转换")
    void canTransition_allValidTransitions() {
        Order o = new Order();

        assertTrue(o.canTransition(Order.PENDING, Order.PAID));
        assertTrue(o.canTransition(Order.PENDING, Order.CANCELLED));

        assertTrue(o.canTransition(Order.PAID, Order.SHIPPED));
        assertTrue(o.canTransition(Order.PAID, Order.CANCELLED));

        assertTrue(o.canTransition(Order.SHIPPED, Order.DELIVERED));
        assertTrue(o.canTransition(Order.SHIPPED, Order.RETURN_PENDING));

        assertTrue(o.canTransition(Order.DELIVERED, Order.RETURN_PENDING));
        assertTrue(o.canTransition(Order.DELIVERED, Order.DELETED));

        assertTrue(o.canTransition(Order.RETURN_PENDING, Order.RETURNING));

        assertTrue(o.canTransition(Order.RETURNING, Order.RETURNED));

        assertTrue(o.canTransition(Order.CANCELLED, Order.DELETED));
    }

    @Test
    @DisplayName("MD-007 canTransition - 所有非法状态转换")
    void canTransition_allInvalidTransitions() {
        Order o = new Order();

        assertFalse(o.canTransition(Order.PENDING, Order.SHIPPED));
        assertFalse(o.canTransition(Order.PENDING, Order.DELIVERED));
        assertFalse(o.canTransition(Order.PAID, Order.PENDING));
        assertFalse(o.canTransition(Order.PAID, Order.DELIVERED));
        assertFalse(o.canTransition(Order.SHIPPED, Order.CANCELLED));
        assertFalse(o.canTransition(Order.SHIPPED, Order.PAID));
        assertFalse(o.canTransition(Order.DELIVERED, Order.PAID));
        assertFalse(o.canTransition(Order.RETURN_PENDING, Order.PAID));
        assertFalse(o.canTransition(Order.RETURNING, Order.RETURN_PENDING));
    }

    @Test
    @DisplayName("MD-008 canTransition - 未知状态作为 fromStatus 返回 false")
    void canTransition_unknownFromStatus() {
        Order o = new Order();
        assertFalse(o.canTransition("UNKNOWN_STATE", Order.PAID));
        assertFalse(o.canTransition("PENDING", "UNKNOWN_STATE"));
    }

    @Test
    @DisplayName("MD-009 transitionTo - 合法转换后状态更新并返回自身")
    void transitionTo_success() {
        Order o = new Order();
        o.setOrderStatus(Order.PENDING);
        Order result = o.transitionTo(Order.PAID);
        assertSame(o, result);
        assertEquals("PAID", o.getOrderStatus());
    }

    @Test
    @DisplayName("MD-010 transitionTo - 链式多次合法转换")
    void transitionTo_chain() {
        Order o = new Order();
        o.setOrderStatus(Order.PENDING);
        o.transitionTo(Order.PAID);
        o.transitionTo(Order.SHIPPED);
        o.transitionTo(Order.DELIVERED);
        assertEquals("DELIVERED", o.getOrderStatus());
    }

    @Test
    @DisplayName("MD-011 transitionTo - 异常信息包含 fromStatus 和 toStatus")
    void transitionTo_exceptionMessage() {
        Order o = new Order();
        o.setOrderStatus(Order.PENDING);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> o.transitionTo(Order.DELIVERED));
        assertTrue(ex.getMessage().contains("PENDING"));
        assertTrue(ex.getMessage().contains("DELIVERED"));
    }

    // ==================== Order 字段类型 ====================

    @Test
    @DisplayName("MD-012 Order.totalPrice 字段类型为 BigDecimal")
    void order_totalPriceFieldType() throws NoSuchFieldException {
        Field f = Order.class.getDeclaredField("totalPrice");
        assertEquals(BigDecimal.class, f.getType());
    }

    // ==================== DeletedOrder.fromOrder ====================

    @Test
    @DisplayName("MD-013 fromOrder - 字段全映射")
    void fromOrder_allFieldsMapped() {
        Timestamp orderDate = Timestamp.valueOf("2026-05-28 10:00:00");
        Order order = new Order();
        order.setOrderId("ORDER001");
        order.setUserId(100L);
        order.setShopId("SHOP001");
        order.setProductId("PROD001");
        order.setQuantity(2);
        order.setTotalPrice(new BigDecimal("199.98"));
        order.setOrderStatus(Order.PAID);
        order.setOrderDate(orderDate);
        order.setContactId(5);

        Timestamp before = new Timestamp(System.currentTimeMillis());
        DeletedOrder deleted = DeletedOrder.fromOrder(order);
        Timestamp after = new Timestamp(System.currentTimeMillis());

        assertEquals("ORDER001", deleted.getOrderId());
        assertEquals(100L, deleted.getUserId());
        assertEquals("SHOP001", deleted.getShopId());
        assertEquals("PROD001", deleted.getProductId());
        assertEquals(2, deleted.getQuantity());
        assertEquals(new BigDecimal("199.98"), deleted.getTotalPrice());
        assertEquals("PAID", deleted.getOrderStatus());
        assertEquals(orderDate, deleted.getOrderDate());
        assertEquals(5, deleted.getContactId());
        assertNull(deleted.getId());
        assertNotNull(deleted.getDeletedAt());
        assertTrue(!deleted.getDeletedAt().before(before) && !deleted.getDeletedAt().after(after));
    }

    @Test
    @DisplayName("MD-014 fromOrder - contactId 为 null 时不抛异常")
    void fromOrder_nullContactId() {
        Order order = new Order();
        order.setOrderId("O");
        order.setOrderStatus(Order.PENDING);
        order.setTotalPrice(BigDecimal.ZERO);

        DeletedOrder deleted = DeletedOrder.fromOrder(order);

        assertNull(deleted.getContactId());
    }

    @Test
    @DisplayName("MD-015 fromOrder - 不会与原 Order 共享 totalPrice 引用")
    void fromOrder_independentBigDecimal() {
        BigDecimal originalPrice = new BigDecimal("99.99");
        Order order = new Order();
        order.setTotalPrice(originalPrice);

        DeletedOrder deleted = DeletedOrder.fromOrder(order);
        deleted.setTotalPrice(new BigDecimal("0.00"));

        assertEquals(new BigDecimal("99.99"), order.getTotalPrice());
        assertEquals(0, BigDecimal.ZERO.compareTo(deleted.getTotalPrice()));
    }

    @Test
    @DisplayName("MD-016 DeletedOrder.totalPrice 字段类型为 BigDecimal")
    void deletedOrder_totalPriceFieldType() throws NoSuchFieldException {
        Field f = DeletedOrder.class.getDeclaredField("totalPrice");
        assertEquals(BigDecimal.class, f.getType());
    }
}
