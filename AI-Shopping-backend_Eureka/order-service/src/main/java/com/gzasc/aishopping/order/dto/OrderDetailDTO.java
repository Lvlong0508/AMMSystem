package com.gzasc.aishopping.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class OrderDetailDTO {
    private String orderId;
    private Long userId;
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
