package com.gzasc.aishopping.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Contact {
    private int id;
    private String name;
    private String phone;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
