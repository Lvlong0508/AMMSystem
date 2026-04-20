package com.gzasc.aishopping.order.dto;

import lombok.Data;

/**
 * 发货请求 DTO
 */
@Data
public class ShipOrderRequest {
    private String trackingNumber;
    private Integer contactId;
    private String shippingDate;
}
