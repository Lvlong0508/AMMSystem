package com.gzasc.aishopping.order.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class Order {
    public static final String PENDING = "PENDING";
    public static final String PAID = "PAID";
    public static final String CANCELLED = "CANCELLED";
    public static final String SHIPPED = "SHIPPED";
    public static final String DELIVERED = "DELIVERED";
    public static final String RETURNED = "RETURNED";

    private String orderId;
    private String productId;
    private int quantity;
    private double totalPrice;
    private String orderStatus;
    private Timestamp orderDate;
    private Integer logisticsId;
    private Integer contactId;

    private boolean canTransition(String fromStatus, String toStatus) {
        if (fromStatus == null) return true;
        switch (fromStatus) {
            case PENDING:
                return toStatus.equals(PAID);
            case PAID:
                return toStatus.equals(SHIPPED) || toStatus.equals(CANCELLED);
            case SHIPPED:
                return toStatus.equals(DELIVERED) || toStatus.equals(RETURNED);
            case DELIVERED:
                return toStatus.equals(RETURNED);
            default:
                return false;
        }
    }

    public Order buildInitOrder(String orderId, String productId, int quantity, double totalPrice) {
        Order order = new Order();
        order.orderId = orderId;
        order.productId = productId;
        order.quantity = quantity;
        order.totalPrice = totalPrice;
        order.orderStatus = PENDING;
        order.orderDate = new Timestamp(System.currentTimeMillis());
        System.out.println("订单创建成功时间: " + order.orderDate);
        return order;
    }

    public Order payOrder(Order order) {
        if (!canTransition(order.orderStatus, PAID)) {
            throw new IllegalStateException("订单状态不允许支付操作，当前状态: " + order.orderStatus);
        }
        order.orderStatus = PAID;
        return order;
    }

    public Order shipOrder(Order order, Integer logisticsId, Integer contactId) {
        if (!canTransition(order.orderStatus, SHIPPED)) {
            throw new IllegalStateException("订单状态不允许发货操作，当前状态: " + order.orderStatus);
        }
        order.logisticsId = logisticsId;
        order.contactId = contactId;
        order.orderStatus = SHIPPED;
        return order;
    }

    public Order deliverOrder(Order order) {
        if (!canTransition(order.orderStatus, DELIVERED)) {
            throw new IllegalStateException("订单状态不允许送达操作，当前状态: " + order.orderStatus);
        }
        order.orderStatus = DELIVERED;
        return order;
    }

    public Order cancelOrder(Order order) {
        if (!canTransition(order.orderStatus, CANCELLED)) {
            throw new IllegalStateException("订单状态不允许取消操作，当前状态: " + order.orderStatus);
        }
        order.orderStatus = CANCELLED;
        return order;
    }

    public Order returnOrder(Order order) {
        if (!canTransition(order.orderStatus, RETURNED)) {
            throw new IllegalStateException("订单状态不允许退货操作，当前状态: " + order.orderStatus);
        }
        order.orderStatus = RETURNED;
        return order;
    }
}
