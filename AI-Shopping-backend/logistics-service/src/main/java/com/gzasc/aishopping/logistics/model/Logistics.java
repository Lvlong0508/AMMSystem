package com.gzasc.aishopping.logistics.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class Logistics {
    private Integer id;
    private String orderId;
    private String type;
    private Integer contactId;
    private Timestamp createdAt;
    private String trackingNumber;
}
