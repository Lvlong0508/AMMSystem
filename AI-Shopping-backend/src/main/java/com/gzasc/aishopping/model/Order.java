package com.gzasc.aishopping.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class Order {
    // 订单状态常量
    public static final String PENDING = "PENDING";      // 待支付
    public static final String PAID = "PAID";          // 待发货
    public static final String CANCELLED = "CANCELLED";  // 已取消
    public static final String SHIPPED = "SHIPPED";      // 已发货
    public static final String DELIVERED = "DELIVERED";  // 已送达
    public static final String RETURNED = "RETURNED";  // 已退货

    private String orderId; // 订单ID

    // 订单商品信息
    private String productId; // 商品ID
    private int quantity; // 数量
    private double totalPrice; // 总价
    private String orderStatus; // 订单状态
    private Timestamp orderDate; // 生成日期时间
    // 寄货人信息
    private Integer logisticsId; // 物流信息ID（外键）
    // 收货人信息
    private Integer contactId;  // 收货人联系人ID（外键）

    /**
     * 状态转换验证 - 确保状态按规则单向流转
     * 待支付 -> 待发货 -> 已取消/已发货 -> 已送达/已退货
     * 已送达 -> 已退货
     */
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

    /**
     * 构建待支付状态的订单
     * @return
     */
    public Order buildInitOrder(
            String orderId,
            String productId,
            int quantity,
            double totalPrice
    ) {
        Order order = new Order();
        order.orderId = orderId;
        order.productId = productId;
        order.quantity = quantity;
        order.totalPrice = totalPrice;
        order.orderStatus = PENDING;
        order.orderDate = new Timestamp(System.currentTimeMillis()); // 生成日期时间
        System.out.println("订单创建成功时间: " + order.orderDate);
        return order;
    }

    /**
     * 订单支付：待支付 -> 待发货
     * @return
     */
    public Order payOrder(Order order) {
        if (!canTransition(order.orderStatus, PAID)) {
            throw new IllegalStateException("订单状态不允许支付操作，当前状态: " + order.orderStatus);
        }
        order.orderStatus = PAID;
        return order;
    }

    /**
     * 订单发货：待发货 -> 已发货
     * @return
     */
    public Order shipOrder(
            Order order,
            Integer logisticsId,
            Integer contactId
    ) {
        if (!canTransition(order.orderStatus, SHIPPED)) {
            throw new IllegalStateException("订单状态不允许发货操作，当前状态: " + order.orderStatus);
        }
        order.logisticsId = logisticsId;
        order.contactId = contactId;
        order.orderStatus = SHIPPED;
        return order;
    }

    /**
     * 订单送达：已发货 -> 已送达
     * @return
     */
    public Order deliverOrder(Order order) {
        if (!canTransition(order.orderStatus, DELIVERED)) {
            throw new IllegalStateException("订单状态不允许送达操作，当前状态: " + order.orderStatus);
        }
        order.orderStatus = DELIVERED;
        return order;
    }

    /**
     * 订单取消：待发货 -> 已取消
     * @return
     */
    public Order cancelOrder(Order order) {
        if (!canTransition(order.orderStatus, CANCELLED)) {
            throw new IllegalStateException("订单状态不允许取消操作，当前状态: " + order.orderStatus);
        }
        order.orderStatus = CANCELLED;
        return order;
    }

    /**
     * 订单退货：已发货/已送达 -> 已退货
     * @return
     */
    public Order returnOrder(Order order) {
        if (!canTransition(order.orderStatus, RETURNED)) {
            throw new IllegalStateException("订单状态不允许退货操作，当前状态: " + order.orderStatus);
        }
        order.orderStatus = RETURNED;
        return order;
    }
}