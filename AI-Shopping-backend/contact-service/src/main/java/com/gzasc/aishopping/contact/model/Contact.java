package com.gzasc.aishopping.contact.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 联系人实体类
 * 对应数据库 t_contact 表，存储用户联系人信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contact {
    private Integer id;           // 联系人ID

    @NotBlank(message = "姓名为空")
    private String name;          // 联系人姓名

    @NotBlank(message = "电话为空")
    private String phone;         // 联系电话

    @NotBlank(message = "地址为空")
    private String address;        // 联系地址

    private Integer isDefault;       // 是否默认联系人：0-否 1-是
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间
}