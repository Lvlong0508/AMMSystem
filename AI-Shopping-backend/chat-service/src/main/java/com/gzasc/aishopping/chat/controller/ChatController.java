package com.gzasc.aishopping.chat.controller;

import com.gzasc.aishopping.chat.AiService.Assistant;
import com.gzasc.aishopping.chat.dto.AiResponse;
import com.gzasc.aishopping.chat.dto.ChatRequest;
import com.gzasc.aishopping.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final Assistant assistant;

    @PostMapping("/chat")
    public ApiResponse<AiResponse> chat(@RequestHeader("X-User-Id") Long userId,
                                        @RequestBody @Valid ChatRequest request) {
        AiResponse response = assistant.chat(userId, request.getMessage());
        return ApiResponse.success(response);
    }
}
