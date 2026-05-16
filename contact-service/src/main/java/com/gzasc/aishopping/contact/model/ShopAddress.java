package com.gzasc.aishopping.contact.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShopAddress {
    private int id;
    private String name;
    private String phone;
    private String address;
    private int addressType;
    private int isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}