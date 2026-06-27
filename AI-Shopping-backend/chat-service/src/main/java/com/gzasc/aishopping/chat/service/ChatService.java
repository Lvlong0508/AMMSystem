package com.gzasc.aishopping.chat.service;

import com.gzasc.aishopping.chat.dto.AiResponse;
import com.gzasc.aishopping.chat.dto.ChatRequest;
import com.gzasc.aishopping.chat.dto.MessageVO;
import java.util.List;

public interface ChatService {
    AiResponse chat(ChatRequest request);
    List<MessageVO> getSessionMessages(String sessionId);
}
