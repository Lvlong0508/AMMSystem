package com.gzasc.aishopping.product.dto;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Deprecated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductWithImageAbstractDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private String tags;
    private Integer imageId;
    private String imageUrl;
    private ShopInfoDTO shop;
}
