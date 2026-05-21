package com.gzasc.aishopping.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductAbstractDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private String tags;
    private Integer imageId;
}
