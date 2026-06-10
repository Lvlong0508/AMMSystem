package com.gzasc.aishopping.order.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class ReturnRequest {
    public static final String APPLYING = "applying";
    public static final String AGREED = "agreed";
    public static final String REJECTED = "rejected";

    private String orderId;
    private Long userId;
    private String shopId;
    private String returnReason;
    private String status;
    private Integer logisticsId;
    private Timestamp createdDate;
    private Timestamp updatedDate;

    public boolean isApplying() {
        return APPLYING.equals(this.status);
    }

    public boolean isAgreed() {
        return AGREED.equals(this.status);
    }
}
