package com.gzasc.aishopping.chat.model;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ChatMemory {

    @Value("${chat.memory.maxMessages}")
    private int maxMessages;

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .maxMessages(maxMessages)
                .id(memoryId)
                .build();
    }
}
