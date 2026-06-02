package com.gzasc.aishopping.order.model;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 已删除订单实体类
 * 用于备份被删除的订单信息
 */
@Data
public class DeletedOrder {
    private Integer id;
    private String orderId;
    private Long userId;
    private String shopId;
    private String productId;
    private int quantity;
    private BigDecimal totalPrice;
    private String orderStatus;
    private Timestamp orderDate;
    private Integer contactId;
    private Timestamp deletedAt;

    public static DeletedOrder fromOrder(Order order) {
        DeletedOrder deletedOrder = new DeletedOrder();
        deletedOrder.setOrderId(order.getOrderId());
        deletedOrder.setUserId(order.getUserId());
        deletedOrder.setShopId(order.getShopId());
        deletedOrder.setProductId(order.getProductId());
        deletedOrder.setQuantity(order.getQuantity());
        deletedOrder.setTotalPrice(order.getTotalPrice());
        deletedOrder.setOrderStatus(order.getOrderStatus());
        deletedOrder.setOrderDate(order.getOrderDate());
        deletedOrder.setContactId(order.getContactId());
        deletedOrder.setDeletedAt(new Timestamp(System.currentTimeMillis()));
        return deletedOrder;
    }
}
