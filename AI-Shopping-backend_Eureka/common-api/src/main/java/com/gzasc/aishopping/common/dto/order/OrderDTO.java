package com.gzasc.aishopping.common.dto.order;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 订单 DTO
 * 用于服务间数据传输
 */
@Data
public class OrderDTO implements Serializable {
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
    private String contactName;
}
