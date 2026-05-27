package com.gzasc.aishopping.shop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopInfo {
    private Long id;
    private String name;
    private String description;
    private String logoUrl;
    private String address;
    private String phone;
}
