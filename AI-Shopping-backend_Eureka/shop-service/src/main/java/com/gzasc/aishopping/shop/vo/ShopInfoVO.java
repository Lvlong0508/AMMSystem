package com.gzasc.aishopping.shop.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopInfoVO {
    private String id;
    private String name;
    private String description;
    private String logourl;
}
