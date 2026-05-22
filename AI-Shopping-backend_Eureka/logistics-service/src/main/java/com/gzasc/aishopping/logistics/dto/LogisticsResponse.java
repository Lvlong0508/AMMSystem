package com.gzasc.aishopping.logistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsResponse {
    private Integer id;
    private Integer contactId;
    private String trackingNumber;
    private Timestamp shippingDate;
}
