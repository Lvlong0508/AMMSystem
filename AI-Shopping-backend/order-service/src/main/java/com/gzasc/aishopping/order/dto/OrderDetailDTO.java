package com.gzasc.aishopping.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class OrderDetailDTO {
    private String orderId;
    private Long userId;
    private String shopId;
    private String shopLogoUrl;
    private String shopName;
    private String productId;
    private String productImageUrl;
    private String productName;
    private int quantity;
    private String productType;
    private BigDecimal totalPrice;
    private String orderStatus;
    private Timestamp orderDate;
    private Integer contactId;
    private String contactName;
    private String contactPhone;
    private String contactAddress;
    private String trackingNumber;
}
