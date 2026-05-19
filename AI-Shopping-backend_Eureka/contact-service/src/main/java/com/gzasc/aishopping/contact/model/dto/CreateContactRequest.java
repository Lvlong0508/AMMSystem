package com.gzasc.aishopping.contact.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建联系人请求 DTO
 */
@Data
public class CreateContactRequest {
    @NotBlank(message = "姓名为空")
    private String name;

    @NotBlank(message = "电话为空")
    private String phone;

    @NotBlank(message = "地址为空")
    private String address;
}