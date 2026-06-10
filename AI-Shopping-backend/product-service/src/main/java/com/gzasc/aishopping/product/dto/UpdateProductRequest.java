package com.gzasc.aishopping.product.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateProductRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;

    private String tags;
}
