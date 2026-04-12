package com.gzasc.aishopping.logistics.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class Logistics {
    private Integer id;
    private Integer contactId;
    private Timestamp shippingDate;
    private String trackingNumber;
}
