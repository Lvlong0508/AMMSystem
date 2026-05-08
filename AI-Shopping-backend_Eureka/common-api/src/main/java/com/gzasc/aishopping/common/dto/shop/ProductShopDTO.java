package com.gzasc.aishopping.common.dto.shop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 商品店铺关联 DTO
 * 用于服务间数据传输
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductShopDTO implements Serializable {
    private String id;
    private String productId;
    private String shopId;
    private Date createdAt;
}