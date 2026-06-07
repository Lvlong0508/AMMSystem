package com.gzasc.aishopping.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简单店铺信息，用于商家端店铺列表展示
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleShopDTO {
    private Long id;
    private String name;
    private Integer status;
}