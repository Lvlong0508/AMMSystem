package com.gzasc.aishopping.contact.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 店铺收货地址实体类
 * 对应数据库 shop_address 表，存储店铺的收货/发货地址信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopAddress {
    private Integer id;              // 地址ID

    @NotBlank(message = "收货人不能为空")
    private String name;            // 收货人姓名

    @NotBlank(message = "电话不能为空")
    private String phone;           // 联系电话

    @NotBlank(message = "地址不能为空")
    private String address;          // 详细地址

    @NotNull(message = "地址类型不能为空")
    private Integer addressType;     // 地址类型：1-收货地址 2-发货地址
    private Integer isDefault;      // 是否默认地址：0-否 1-是
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}