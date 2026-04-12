package com.gzasc.aishopping.order.dto;

import lombok.Data;

import java.util.Date;

@Data
public class ProductDTO {
    private String id;
    private String name;
    private Double price;
    private String tags;
    private String description;
    private int stock;
    private Date createdAt;
    private Date updatedAt;
}
