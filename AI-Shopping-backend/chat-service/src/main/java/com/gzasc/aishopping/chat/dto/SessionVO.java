package com.gzasc.aishopping.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SessionVO {
    private String id;
    private String title;
    private String updatedAt;
}
