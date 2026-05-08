package com.gzasc.aishopping.shop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderShop {
    private String id;
    private String orderId;
    private String shopId;
    private LocalDateTime createdAt;
}