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
    private Long merchantId;
    private Long shopId;
    private Integer role; // 1=店长, 2=店员
    private Long assignedBy;
    private LocalDateTime createdAt;
}