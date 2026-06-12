package com.gzasc.aishopping.chat.AiService;

import com.gzasc.aishopping.chat.dto.AiResponse;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService(chatModel = "dashScopeChatModel", chatMemoryProvider = "chatMemoryProvider")
public interface Assistant {
    @SystemMessage(fromResource = "prompts/system-prompt.txt")
    AiResponse chat(@MemoryId String memoryId, @UserMessage String userMessage);
}
