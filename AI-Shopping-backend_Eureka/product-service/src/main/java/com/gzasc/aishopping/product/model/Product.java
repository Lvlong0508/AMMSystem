package com.gzasc.aishopping.product.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String id;
    private String name;
    private Double price;
    private String tags;
    private String description;
    private int stock;
    private Date createdAt;
    private Date updatedAt;
}
