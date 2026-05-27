package com.gzasc.aishopping.common.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO implements Serializable {
    private Long id;
    private String name;
    private Double price;
    private String tags;
    private String description;
    private Integer stock;
    private Long shopId;
    private Date createdAt;
    private Date updatedAt;
}
