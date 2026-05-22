package com.gzasc.aishopping.product.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ListProductRequest {
    private Integer page = 0;
    private Integer size = 10;
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
