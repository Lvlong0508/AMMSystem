package com.gzasc.aishopping.chat.model;

import com.gzasc.aishopping.chat.AiService.Assistant;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ChatMemoryIntegrationTest.TestContext.class)
class ChatMemoryIntegrationTest {

    @Autowired
    private Assistant assistant;

    @Autowired
    private SpyChatModel spyChatModel;

    @BeforeEach
    void setUp() {
        spyChatModel.capturedMessages.clear();
    }

    @SpringBootApplication(scanBasePackages = "com.gzasc.aishopping.chat.AiService")
    static class TestContext {

        @Bean
        @Primary
        public ChatLanguageModel spyChatLanguageModel() {
            return new SpyChatModel();
        }

        @Bean
        public ChatMemoryProvider chatMemoryProvider() {
            return memoryId -> MessageWindowChatMemory.builder()
                    .maxMessages(10)
                    .id(memoryId)
                    .build();
        }
    }

    static class SpyChatModel implements ChatLanguageModel {
        final List<List<ChatMessage>> capturedMessages = new ArrayList<>();

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            capturedMessages.add(new ArrayList<>(messages));
            String json = """
                    {"message":"mock回复","reason":"test","data":null}
                    """.trim();
            return Response.from(AiMessage.from(json));
        }

        @Override
        public Set<Capability> supportedCapabilities() {
            return Set.of();
        }

        List<ChatMessage> lastRequest() {
            if (capturedMessages.isEmpty()) {
                return List.of();
            }
            return capturedMessages.get(capturedMessages.size() - 1);
        }
    }

    @Test
    void testChatMemoryIsolation() {
        assistant.chat(1L, "帮我推荐手机");
        assistant.chat(1L, "有没有便宜点的");

        assistant.chat(2L, "帮我查一下我的订单");

        List<ChatMessage> lastRequest = spyChatModel.lastRequest();
        List<String> texts = lastRequest.stream()
                .map(ChatMessage::text)
                .toList();

        assertThat(texts)
                .describedAs("用户2的请求不应包含用户1的消息")
                .noneMatch(t -> t.contains("手机") || t.contains("便宜"));

        assertThat(texts)
                .describedAs("用户2的请求应包含自己的消息")
                .anyMatch(t -> t.contains("订单"));

        assertThat(spyChatModel.capturedMessages)
                .describedAs("共3次AI调用")
                .hasSize(3);
    }

    @Test
    void testSameUserSeesHistory() {
        assistant.chat(1L, "第一次说你好");

        List<ChatMessage> secondRequest = spyChatModel.lastRequest();
        assertThat(secondRequest)
                .describedAs("用户1首次调用应包含发送的消息")
                .anyMatch(m -> m.text().contains("你好"));

        assistant.chat(1L, "第二次再说你好");

        List<ChatMessage> thirdRequest = spyChatModel.lastRequest();
        assertThat(thirdRequest)
                .describedAs("用户1第二次调用应看到自己的历史消息")
                .anyMatch(m -> m.text().contains("第一次说你好"));

        assertThat(thirdRequest)
                .describedAs("用户1第二次调用应包含本次消息")
                .anyMatch(m -> m.text().contains("第二次再说你好"));
    }
}
