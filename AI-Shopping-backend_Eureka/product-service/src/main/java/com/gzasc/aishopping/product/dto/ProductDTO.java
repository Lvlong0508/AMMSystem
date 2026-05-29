package com.gzasc.aishopping.product.dto;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private String tags;
    private String imageUrl;
    private Integer imageId;
    private String description;
    private Integer stock;
    private Boolean isSale;
    private ShopInfoDTO shop;
    private Date createdAt;
    private Date updatedAt;
}
