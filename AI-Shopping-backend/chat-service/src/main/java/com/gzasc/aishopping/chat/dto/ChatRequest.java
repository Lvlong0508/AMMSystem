package com.gzasc.aishopping.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "消息内容不能为空")
    private String message;
}
