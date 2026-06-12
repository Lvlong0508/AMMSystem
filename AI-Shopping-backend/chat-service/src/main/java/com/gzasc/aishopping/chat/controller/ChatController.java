package com.gzasc.aishopping.chat.controller;

import com.gzasc.aishopping.chat.AiService.Assistant;
import com.gzasc.aishopping.chat.dto.AiResponse;
import com.gzasc.aishopping.chat.dto.ChatRequest;
import com.gzasc.aishopping.chat.service.impl.ChatSessionService;
import com.gzasc.aishopping.chat.service.impl.MongoChatMemoryStore;
import com.gzasc.aishopping.common.response.ApiResponse;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public ApiResponse<List<Map<String, Object>>> getSessionMessages(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String sessionId) {
        if (!chatSessionService.isSessionOwner(sessionId, userId)) {
            return ApiResponse.error(403, "无权访问该会话");
        }
        List<ChatMessage> messages = mongoChatMemoryStore.getMessages(sessionId);
        List<Map<String, Object>> result = messages.stream()
                .filter(m -> m.type() != dev.langchain4j.data.message.ChatMessageType.SYSTEM)
                .map(m -> {
                    Map<String, Object> msg = new LinkedHashMap<>();
                    msg.put("role", m.type().name().toLowerCase());
                    String text;
                    if (m instanceof UserMessage) {
                        text = ((UserMessage) m).singleText();
                    } else if (m instanceof dev.langchain4j.data.message.AiMessage) {
                        text = ((dev.langchain4j.data.message.AiMessage) m).text();
                    } else if (m instanceof dev.langchain4j.data.message.ToolExecutionResultMessage) {
                        text = ((dev.langchain4j.data.message.ToolExecutionResultMessage) m).text();
                    } else {
                        text = m.text();
                    }
                    msg.put("text", text);
                    return msg;
                })
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }
}
