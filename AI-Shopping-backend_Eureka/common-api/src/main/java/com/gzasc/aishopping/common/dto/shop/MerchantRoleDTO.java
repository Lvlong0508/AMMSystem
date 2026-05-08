package com.gzasc.aishopping.common.dto.shop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 商家角色 DTO
 * 用于服务间数据传输
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRoleDTO implements Serializable {
    private String id;
    private String merchantId;
    private String shopId;
    private String role;
    private String assignedBy;
    private Date createdAt;
}