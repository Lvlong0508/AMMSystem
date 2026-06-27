package com.gzasc.aishopping.chat.controller;

import com.gzasc.aishopping.chat.dto.AiResponse;
import com.gzasc.aishopping.chat.dto.ChatRequest;
import com.gzasc.aishopping.chat.dto.MessageVO;
import com.gzasc.aishopping.chat.service.ChatService;
import com.gzasc.aishopping.chat.service.ChatSessionService;
import com.gzasc.aishopping.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatSessionService chatSessionService;

    @PostMapping("/chat")
    public ApiResponse<AiResponse> chat(@RequestHeader("X-User-Id") Long userId,
                                        @RequestBody @Valid ChatRequest request) {
        AiResponse response = chatService.chat(request);
        return ApiResponse.success(response);
    }

    @GetMapping("/session/{sessionId}/messages")
    public ApiResponse<List<MessageVO>> getSessionMessages(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String sessionId) {
        if (!chatSessionService.isSessionOwner(sessionId, userId)) {
            return ApiResponse.error(403, "无权访问该会话");
        }
        return ApiResponse.success(chatService.getSessionMessages(sessionId));
    }
}
