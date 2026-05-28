package com.gzasc.aishopping.chat.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@lombok.Data
@NoArgsConstructor
@AllArgsConstructor
public class AiResponse {
    private String message;
    private String reason;
    private Data data;
}
