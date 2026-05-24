package com.gzasc.aishopping.order.dto;

import lombok.Data;

@Data
public class OrderAbstractSellerDTO {
    private String orderId;
    private String productId;
    private Integer contactId;
    private int quantity;
    private String orderStatus;
}
