package com.gzasc.aishopping.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderAbstractUserDTO {
    private String orderId;
    private String productId;
    private String shopId;
    private BigDecimal totalPrice;
    private int quantity;
    private String orderStatus;
}
