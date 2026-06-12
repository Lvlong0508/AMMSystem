package com.gzasc.aishopping.chat.controller;

import com.gzasc.aishopping.chat.AiService.Assistant;
import com.gzasc.aishopping.chat.converter.ChatMessageConverter;
import com.gzasc.aishopping.chat.dto.AiResponse;
import com.gzasc.aishopping.chat.dto.ChatRequest;
import com.gzasc.aishopping.chat.dto.MessageVO;
import com.gzasc.aishopping.chat.service.impl.ChatSessionService;
import com.gzasc.aishopping.chat.service.impl.MongoChatMemoryStore;
import com.gzasc.aishopping.common.response.ApiResponse;
import dev.langchain4j.data.message.ChatMessage;
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

    private final Assistant assistant;
    private final ChatSessionService chatSessionService;
    private final MongoChatMemoryStore mongoChatMemoryStore;

    @PostMapping("/chat")
    public ApiResponse<AiResponse> chat(@RequestHeader("X-User-Id") Long userId,
                                        @RequestBody @Valid ChatRequest request) {
        String sessionId = request.getSessionId();
        AiResponse response = assistant.chat(sessionId, request.getMessage());
        // 首次发消息时自动设置会话标题（取前20字）
        List<ChatMessage> existingMessages = mongoChatMemoryStore.getMessages(sessionId);
        long userMsgCount = existingMessages.stream()
                .filter(m -> m.type() == dev.langchain4j.data.message.ChatMessageType.USER)
                .count();
        if (userMsgCount == 1) {
            String title = request.getMessage().length() > 20
                    ? request.getMessage().substring(0, 20)
                    : request.getMessage();
            chatSessionService.updateTitle(sessionId, title);
        } else {
            chatSessionService.touchUpdatedAt(sessionId);
        }
        return ApiResponse.success(response);
    }

    @GetMapping("/session/{sessionId}/messages")
    public ApiResponse<List<MessageVO>> getSessionMessages(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String sessionId) {
        if (!chatSessionService.isSessionOwner(sessionId, userId)) {
            return ApiResponse.error(403, "无权访问该会话");
        }
        List<ChatMessage> messages = mongoChatMemoryStore.getMessages(sessionId);
        return ApiResponse.success(ChatMessageConverter.toMessageList(messages));
    }
}
