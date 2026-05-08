package com.gzasc.aishopping.shop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRole {
    private String id;
    private String merchantId;
    private String shopId;
    private String role;
    private String assignedBy;
    private LocalDateTime createdAt;
}