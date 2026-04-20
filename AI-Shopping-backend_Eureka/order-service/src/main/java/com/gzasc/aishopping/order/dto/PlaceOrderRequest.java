package com.gzasc.aishopping.order.dto;

import lombok.Data;

/**
 * 下单请求 DTO
 */
@Data
public class PlaceOrderRequest {
    private String productId;
    private int quantity;
    private Integer contactId;
}
