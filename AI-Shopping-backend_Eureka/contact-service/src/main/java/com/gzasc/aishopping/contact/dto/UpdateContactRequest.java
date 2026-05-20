package com.gzasc.aishopping.contact.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新联系人请求 DTO
 */
@Data
public class UpdateContactRequest {
    @NotNull(message = "ID不能为空")
    private Integer id;

    @NotBlank(message = "姓名为空")
    private String name;

    @NotBlank(message = "电话为空")
    private String phone;

    @NotBlank(message = "地址为空")
    private String address;
}
