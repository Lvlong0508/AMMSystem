package com.gzasc.aishopping.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class SellerOrderCardDTO {
    private String orderId;
    private String productImageUrl;
    private String productName;
    private int quantity;
    private String orderStatus;
    private BigDecimal totalPrice;
    private Timestamp orderDate;
    private String contactName;
    private String contactPhone;
    private String contactAddress;
}
