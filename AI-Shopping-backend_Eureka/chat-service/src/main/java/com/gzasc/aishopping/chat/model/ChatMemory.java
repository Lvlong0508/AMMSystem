package com.gzasc.aishopping.chat.model;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ChatMemory {

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .maxMessages(10)
                .id(memoryId)
                .build();
    }
}
