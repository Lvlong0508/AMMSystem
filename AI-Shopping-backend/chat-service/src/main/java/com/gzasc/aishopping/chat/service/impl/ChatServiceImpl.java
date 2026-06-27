package com.gzasc.aishopping.chat.service.impl;

import com.gzasc.aishopping.chat.AiService.Assistant;
import com.gzasc.aishopping.chat.converter.ChatMessageConverter;
import com.gzasc.aishopping.chat.dto.AiResponse;
import com.gzasc.aishopping.chat.dto.ChatRequest;
import com.gzasc.aishopping.chat.dto.MessageVO;
import com.gzasc.aishopping.chat.service.ChatMessageService;
import com.gzasc.aishopping.chat.service.ChatService;
import com.gzasc.aishopping.chat.service.ChatSessionService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final Assistant assistant;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;

    @Override
    public AiResponse chat(ChatRequest request) {
        String sessionId = request.getSessionId();
        AiResponse response = assistant.chat(sessionId, request.getMessage());
        List<ChatMessage> existingMessages = chatMessageService.getMessages(sessionId);
        long userMsgCount = existingMessages.stream()
                .filter(m -> m.type() == ChatMessageType.USER)
                .count();
        if (userMsgCount == 1) {
            String title = request.getMessage().length() > 20
                    ? request.getMessage().substring(0, 20)
                    : request.getMessage();
            chatSessionService.updateTitle(sessionId, title);
        } else {
            chatSessionService.touchUpdatedAt(sessionId);
        }
        return response;
    }

    @Override
    public List<MessageVO> getSessionMessages(String sessionId) {
        List<ChatMessage> messages = chatMessageService.getMessages(sessionId);
        return ChatMessageConverter.toMessageList(messages);
    }
}
