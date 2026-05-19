package com.gzasc.aishopping.contact.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新地址请求 DTO
 */
@Data
public class UpdateAddressRequest {
    @NotNull(message = "ID不能为空")
    private Integer id;

    @NotBlank(message = "收货人不能为空")
    private String name;

    @NotBlank(message = "电话不能为空")
    private String phone;

    @NotBlank(message = "地址不能为空")
    private String address;

    @NotNull(message = "地址类型不能为空")
    private Integer addressType;

    private Integer isDefault;
}