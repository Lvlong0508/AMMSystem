package com.gzasc.aishopping.common.dto.shop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 订单店铺关联 DTO
 * 用于服务间数据传输
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderShopDTO implements Serializable {
    private String id;
    private String orderId;
    private String shopId;
    private Date createdAt;
}