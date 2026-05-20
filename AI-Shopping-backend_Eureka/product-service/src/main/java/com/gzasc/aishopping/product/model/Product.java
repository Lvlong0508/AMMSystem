package com.gzasc.aishopping.product.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Long id;

    private String name;
    private BigDecimal price;
    private String tags;
    private String description;
    private Integer stock;

    private boolean isSale;
    private Integer imageId;

    private Date createdAt;
    private Date updatedAt;
}