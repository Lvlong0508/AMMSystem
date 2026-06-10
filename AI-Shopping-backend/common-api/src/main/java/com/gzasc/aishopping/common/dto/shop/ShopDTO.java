package com.gzasc.aishopping.common.dto.shop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 店铺 DTO
 * 用于服务间数据传输
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopDTO implements Serializable {
    private String id;
    private String name;
    private String description;
    private String logoUrl;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
    private String merchantId;
}