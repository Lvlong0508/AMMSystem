package com.gzasc.aishopping.order.dto;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class ReturnRequestDTO {
    private String orderId;
    private Long userId;
    private String shopId;
    private String returnReason;
    private String status;
    private Integer logisticsId;
    private Timestamp createdDate;
    private Timestamp updatedDate;
}
