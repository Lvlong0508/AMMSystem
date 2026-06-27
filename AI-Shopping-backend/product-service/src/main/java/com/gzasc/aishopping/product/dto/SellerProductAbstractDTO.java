package com.gzasc.aishopping.product.dto;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerProductAbstractDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private String tags;
    private Integer imageId;
    private String imageUrl;
    @JsonProperty("isSale")
    private boolean isSale;
    private ShopInfoDTO shop;
}
