package com.gzasc.aishopping.shop.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopVO {
    private String id;
    private String merchantId;
    private String shopInfoId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
