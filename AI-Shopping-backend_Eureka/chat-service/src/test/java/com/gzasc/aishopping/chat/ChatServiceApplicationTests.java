package com.gzasc.aishopping.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ChatServiceApplicationTests {

    @Value("${langchain4j.dashscope.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.dashscope.chat-model.model-name}")
    private String modelName;

    @Value("${langchain4j.dashscope.chat-model.temperature}")
    private Double temperature;

    @Test
    void apiKeyShouldBeReadable() {
        assertNotNull(apiKey, "API Key should not be null");
        assertEquals("test-sk-mock-key", apiKey);
    }

    @Test
    void modelNameShouldBeGlm5_1() {
        assertEquals("glm-5.1", modelName);
    }

    @Test
    void temperatureShouldBe0_7() {
        assertEquals(0.7, temperature, 0.001);
    }
}
