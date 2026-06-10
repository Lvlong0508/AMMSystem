package com.gzasc.aishopping.order.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;

@Slf4j
@Data
public class Order {
    /** 待支付 */
    public static final String PENDING = "PENDING";
    /** 待发货 */
    public static final String PAID = "PAID";
    /** 待收货 */
    public static final String SHIPPED = "SHIPPED";
    /** 已完成 */
    public static final String DELIVERED = "DELIVERED";
    /** 已取消 */
    public static final String CANCELLED = "CANCELLED";
    /** 已删除 */
    public static final String DELETED = "DELETED";
    /** 待退货 */
    public static final String RETURN_PENDING = "RETURN_PENDING";
    /** 退货中 */
    public static final String RETURNING = "RETURNING";
    /** 已退货 */
    public static final String RETURNED = "RETURNED";

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            PENDING, Set.of(PAID, CANCELLED),
            PAID, Set.of(SHIPPED, CANCELLED),
            SHIPPED, Set.of(DELIVERED, RETURN_PENDING),
            DELIVERED, Set.of(RETURN_PENDING, DELETED),
            RETURN_PENDING, Set.of(RETURNING),
            RETURNING, Set.of(RETURNED),
            CANCELLED, Set.of(DELETED)
    );

    private String orderId;
    private Long userId;
    private String shopId;
    private String productId;
    private int quantity;
    private BigDecimal totalPrice;
    private String orderStatus;
    private Timestamp orderDate;
    private Integer contactId;

    public boolean canTransition(String fromStatus, String toStatus) {
        if (fromStatus == null || toStatus == null) return false;
        return TRANSITIONS.getOrDefault(fromStatus, Set.of()).contains(toStatus);
    }

    public Order transitionTo(String targetStatus) {
        if (!canTransition(this.orderStatus, targetStatus)) {
            throw new IllegalStateException("订单状态不允许从 " + this.orderStatus + " 转换为 " + targetStatus);
        }
        this.orderStatus = targetStatus;
        return this;
    }

    public static Order buildInitOrder(String orderId, Long userId, String shopId,
                                       String productId, int quantity, BigDecimal totalPrice) {
        Order order = new Order();
        order.orderId = orderId;
        order.userId = userId;
        order.shopId = shopId;
        order.productId = productId;
        order.quantity = quantity;
        order.totalPrice = totalPrice;
        order.orderStatus = PENDING;
        order.orderDate = new Timestamp(System.currentTimeMillis());
        log.info("订单创建成功时间: {}", order.orderDate);
        return order;
    }


}
