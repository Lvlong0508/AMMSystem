package com.gzasc.aishopping.logistics.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogisticsRequest {
    private Integer contactId;
    private String trackingNumber;
    private String shippingDate;
}
