package com.gzasc.aishopping.shop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shop {
    private Long id;
    private Long merchantId;
    private Long shopInfoId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}