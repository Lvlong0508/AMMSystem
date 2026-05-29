package com.gzasc.aishopping.chat.model;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ChatModel {

    @Bean
    public ChatLanguageModel dashScopeChatModel() {

        QwenChatModel qwenChatModel = QwenChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("glm-5.1")
                .enableSearch(false)
                .temperature(0.7f)
                .build();

        return qwenChatModel;
    }
}
