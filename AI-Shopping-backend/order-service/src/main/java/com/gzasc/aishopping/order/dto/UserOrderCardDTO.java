package com.gzasc.aishopping.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UserOrderCardDTO {
    private String orderId;
    private String shopLogoUrl;
    private String shopName;
    private String productImageUrl;
    private String productName;
    private int quantity;
    private String productType;
    private String orderStatus;
    private BigDecimal totalPrice;
}
