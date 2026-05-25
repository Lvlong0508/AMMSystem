package com.gzasc.aishopping.shop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductShop {
    private Long id;
    private Long productId;
    private Long shopId;
    private LocalDateTime createdAt;
}