package com.gzasc.aishopping.common.dto.contact;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 联系人 DTO
 * 用于服务间数据传输
 */
@Data
public class ContactDTO implements Serializable {
    private int id;
    private String name;
    private String phone;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
