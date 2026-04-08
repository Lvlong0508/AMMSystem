package com.gzasc.aishopping.dto;

import lombok.Data;

/**
 * 下单请求基础 DTO
 */
@Data
public class PlaceOrderRequest {
    private String productId;
    private int quantity;
}
