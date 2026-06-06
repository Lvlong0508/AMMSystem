package com.gzasc.aishopping.order.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailVO {
    private String orderId;
    private String userId;
    private String shopId;
    private String productId;
    private int quantity;
    private BigDecimal totalPrice;
    private String orderStatus;
    private Timestamp orderDate;
    private Integer contactId;
    private String contactName;
    private String contactPhone;
    private String contactAddress;
    private String trackingNumber;
}
