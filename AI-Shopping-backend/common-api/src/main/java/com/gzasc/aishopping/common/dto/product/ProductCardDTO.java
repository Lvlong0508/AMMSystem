package com.gzasc.aishopping.common.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCardDTO implements Serializable {
    private Long id;
    private String name;
    private String imageUrl;
    private Integer stock;
    private BigDecimal price;
}