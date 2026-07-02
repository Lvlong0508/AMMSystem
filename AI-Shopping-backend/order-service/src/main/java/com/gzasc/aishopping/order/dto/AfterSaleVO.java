package com.gzasc.aishopping.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class AfterSaleVO {
    private String orderId;
    private String orderStatus;
    private Timestamp orderDate;

    private String shopId;
    private String shopName;
    private String shopLogoUrl;

    private String productId;
    private String productName;
    private String productImageUrl;
    private String productType;
    private int quantity;
    private BigDecimal totalPrice;

    private String returnStatus;
    private String returnReason;
    private Integer logisticsId;
    private String returnTrackingNumber;
    private Timestamp returnCreatedDate;
    private Timestamp returnUpdatedDate;
}
