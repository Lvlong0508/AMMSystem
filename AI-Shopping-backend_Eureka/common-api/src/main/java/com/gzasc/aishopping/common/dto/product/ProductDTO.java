package com.gzasc.aishopping.common.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 商品 DTO
 * 用于服务间数据传输
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO implements Serializable {
    private String id;
    private String name;
    private Double price;
    private String tags;
    private String description;
    private Integer stock;
    private Date createdAt;
    private Date updatedAt;
}
