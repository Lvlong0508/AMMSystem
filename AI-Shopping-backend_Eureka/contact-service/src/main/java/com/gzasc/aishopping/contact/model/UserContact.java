package com.gzasc.aishopping.contact.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserContact {
    private int id;
    private int userId;
    private int contactId;
    private LocalDateTime createdAt;
}