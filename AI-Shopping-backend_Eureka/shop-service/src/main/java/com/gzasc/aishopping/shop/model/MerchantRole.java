package com.gzasc.aishopping.shop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRole {
    private Long id;
    private String merchantId;
    private String shopId;
    private String role; // 1是店长；2是店员
    private String assignedBy;
    private LocalDateTime createdAt;
}