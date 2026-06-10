package com.gzasc.aishopping.contact.dto;

import com.gzasc.aishopping.contact.model.ShopAddress;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 地址响应 DTO
 */
@Data
public class AddressResponse {
    private Integer id;
    private String name;
    private String phone;
    private String address;
    private Integer addressType;
    private Integer isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AddressResponse fromShopAddress(ShopAddress shopAddress) {
        if (shopAddress == null) {
            return null;
        }
        AddressResponse response = new AddressResponse();
        response.setId(shopAddress.getId());
        response.setName(shopAddress.getName());
        response.setPhone(shopAddress.getPhone());
        response.setAddress(shopAddress.getAddress());
        response.setAddressType(shopAddress.getAddressType());
        response.setIsDefault(shopAddress.getIsDefault());
        response.setCreatedAt(shopAddress.getCreatedAt());
        response.setUpdatedAt(shopAddress.getUpdatedAt());
        return response;
    }
}