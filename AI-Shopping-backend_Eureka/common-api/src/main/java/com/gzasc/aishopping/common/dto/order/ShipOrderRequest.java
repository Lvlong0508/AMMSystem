package com.gzasc.aishopping.common.dto.order;

import lombok.Data;

@Data
public class ShipOrderRequest {
    private String orderId;
    private String trackingNumber;
    private Integer contactId;
    private String shippingDate;
}