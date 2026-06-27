package com.gzasc.aishopping.chat.config;

import com.gzasc.aishopping.chat.service.ChatMessageService;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

    @Value("${chat.memory.maxMessages}")
    private int maxMessages;

    @Autowired
    private ChatMessageService chatMessageService;

    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(maxMessages)
                .chatMemoryStore(chatMessageService)
                .build();
    }
}
