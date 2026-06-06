package com.gzasc.aishopping.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailVO {
    private String id;
    private String name;
    private BigDecimal price;
    private String tags;
    private String description;
    private Integer stock;
    private Boolean isSale;
    private Integer imageId;
    private String imageUrl;
    private ShopInfoVO shop;
    private Date createdAt;
    private Date updatedAt;
}
