package com.gzasc.aishopping.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class ShipmentOrderCardDTO {
    private String orderId;
    private String productImageUrl;
    private String productName;
    private int quantity;
    private String productType;
    private String orderStatus;
    private Timestamp orderDate;
    private String contactName;
    private String contactPhone;
    private String contactAddress;
}
