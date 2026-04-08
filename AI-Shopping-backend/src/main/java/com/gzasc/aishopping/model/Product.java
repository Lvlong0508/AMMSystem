package com.gzasc.aishopping.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String id; // 商品ID
    private String name; // 商品名称
    private Double price; // 商品价格
    private String tags; // 商品标签
    private String description; // 商品描述
    private int stock; // 商品库存
    private Date createdAt; // 商品创建时间
    private Date updatedAt; // 商品更新时间
}