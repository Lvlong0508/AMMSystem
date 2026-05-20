package com.gzasc.aishopping.contact.dto;

import com.gzasc.aishopping.contact.model.Contact;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联系人响应 DTO
 */
@Data
public class ContactResponse {
    private Integer id;
    private String name;
    private String phone;
    private String address;
    private Integer isDefault;       // 是否默认：0-否 1-是
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ContactResponse fromContact(Contact contact) {
        if (contact == null) {
            return null;
        }
        ContactResponse response = new ContactResponse();
        response.setId(contact.getId());
        response.setName(contact.getName());
        response.setPhone(contact.getPhone());
        response.setAddress(contact.getAddress());
        response.setIsDefault(contact.getIsDefault());
        response.setCreatedAt(contact.getCreatedAt());
        response.setUpdatedAt(contact.getUpdatedAt());
        return response;
    }
}