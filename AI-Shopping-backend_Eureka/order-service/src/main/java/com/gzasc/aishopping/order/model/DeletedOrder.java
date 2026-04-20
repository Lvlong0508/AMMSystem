package com.gzasc.aishopping.order.model;

import lombok.Data;
import java.sql.Timestamp;

/**
 * 已删除订单实体类
 * 用于备份被删除的订单信息
 */
@Data
public class DeletedOrder {
    private Integer id;           // 自增ID
    private String orderId;       // 订单ID
    private String productId;     // 商品ID
    private int quantity;         // 购买数量
    private double totalPrice;    // 订单总价
    private String orderStatus;   // 删除时的订单状态
    private Timestamp orderDate;  // 原下单时间
    private Integer contactId;    // 联系人ID
    private Integer logisticsId;  // 物流信息ID
    private Timestamp deletedAt;  // 删除时间

    /**
     * 从 Order 对象构建 DeletedOrder
     */
    public static DeletedOrder fromOrder(Order order) {
        DeletedOrder deletedOrder = new DeletedOrder();
        deletedOrder.setOrderId(order.getOrderId());
        deletedOrder.setProductId(order.getProductId());
        deletedOrder.setQuantity(order.getQuantity());
        deletedOrder.setTotalPrice(order.getTotalPrice());
        deletedOrder.setOrderStatus(order.getOrderStatus());
        deletedOrder.setOrderDate(order.getOrderDate());
        deletedOrder.setContactId(order.getContactId());
        deletedOrder.setLogisticsId(order.getLogisticsId());
        deletedOrder.setDeletedAt(new Timestamp(System.currentTimeMillis()));
        return deletedOrder;
    }
}
