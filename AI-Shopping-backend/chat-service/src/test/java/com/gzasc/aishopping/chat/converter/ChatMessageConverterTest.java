package com.gzasc.aishopping.chat.converter;

import com.gzasc.aishopping.chat.dto.MessageVO;
import com.gzasc.aishopping.chat.dto.ProductItem;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageConverterTest {

    @Test
    @DisplayName("空列表返回空 List")
    void emptyList() {
        List<MessageVO> result = ChatMessageConverter.toMessageList(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("null 返回空 List")
    void nullList() {
        List<MessageVO> result = ChatMessageConverter.toMessageList(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("SystemMessage 被过滤")
    void systemMessageFiltered() {
        var messages = List.<ChatMessage>of(
                new SystemMessage("system prompt"),
                new UserMessage("你好"),
                new AiMessage("你好！")
        );
        List<MessageVO> result = ChatMessageConverter.toMessageList(messages);
        assertEquals(2, result.size());
        assertEquals("user", result.get(0).getRole());
        assertEquals("ai", result.get(1).getRole());
    }

    @Test
    @DisplayName("一轮对话 - UserMessage + AiMessage（无 tool）")
    void simpleConversation() {
        var messages = List.<ChatMessage>of(
                new UserMessage("你好"),
                new AiMessage("你好！我是小物")
        );
        List<MessageVO> result = ChatMessageConverter.toMessageList(messages);
        assertEquals(2, result.size());

        assertEquals("user", result.get(0).getRole());
        assertEquals("你好", result.get(0).getText());
        assertNull(result.get(0).getProducts());

        assertEquals("ai", result.get(1).getRole());
        assertEquals("你好！我是小物", result.get(1).getText());
        assertNull(result.get(1).getProducts());
    }

    @Test
    @DisplayName("带 tool call 的对话 - UserMessage + ToolExecutionResult + AiMessage")
    void toolCallConversation() {
        var messages = List.<ChatMessage>of(
                new UserMessage("有哪些商品"),
                new ToolExecutionResultMessage("product_tool", "{\"id\":\"1\"}", "{\"type\":\"product\",\"products\":[{\"id\":1,\"name\":\"手机\",\"price\":2999.0,\"tags\":\"tag\",\"description\":\"desc\",\"stock\":100,\"imageUrl\":\"url\",\"shopName\":\"shop\"}]}"),
                new AiMessage("为您找到以下商品")
        );
        List<MessageVO> result = ChatMessageConverter.toMessageList(messages);
        assertEquals(2, result.size());

        assertEquals("user", result.get(0).getRole());
        assertNull(result.get(0).getProducts());

        assertEquals("ai", result.get(1).getRole());
        assertEquals("为您找到以下商品", result.get(1).getText());
        assertNotNull(result.get(1).getProducts());
        assertEquals(1, result.get(1).getProducts().size());
        assertEquals("手机", result.get(1).getProducts().get(0).name());
    }

    @Test
    @DisplayName("AiMessage 无 text()（纯 tool call 阶段）被跳过")
    void aiMessageWithoutTextSkipped() {
        var messages = List.<ChatMessage>of(
                new UserMessage("查询商品"),
                AiMessage.from(ToolExecutionRequest.builder().id("1").name("tool").arguments("{}").build()),
                new ToolExecutionResultMessage("tool", "{\"id\":\"1\"}", "{\"type\":\"product\",\"products\":[{\"id\":1,\"name\":\"手机\",\"price\":2999.0}]}"),
                new AiMessage("结果如下")
        );
        List<MessageVO> result = ChatMessageConverter.toMessageList(messages);
        assertEquals(2, result.size());

        assertEquals("user", result.get(0).getRole());
        assertEquals("ai", result.get(1).getRole());
        assertEquals("结果如下", result.get(1).getText());
        assertNotNull(result.get(1).getProducts());
    }

    @Test
    @DisplayName("连续多轮对话")
    void multiTurnConversation() {
        var messages = List.<ChatMessage>of(
                new UserMessage("你好"),
                new AiMessage("你好！"),
                new UserMessage("有什么手机"),
                new ToolExecutionResultMessage("tool", "{}", "{\"type\":\"product\",\"products\":[{\"id\":1,\"name\":\"手机\",\"price\":2999.0}]}"),
                new AiMessage("推荐这款手机")
        );
        List<MessageVO> result = ChatMessageConverter.toMessageList(messages);
        assertEquals(4, result.size());

        assertNull(result.get(0).getProducts());
        assertNull(result.get(1).getProducts());
        assertNull(result.get(2).getProducts());
        assertNotNull(result.get(3).getProducts());
    }

    @Test
    @DisplayName("JSON 解析失败时静默跳过，products 为 null")
    void invalidJsonSkipsSilently() {
        var messages = List.<ChatMessage>of(
                new UserMessage("查商品"),
                new ToolExecutionResultMessage("tool", "{}", "invalid json"),
                new AiMessage("抱歉没找到")
        );
        List<MessageVO> result = ChatMessageConverter.toMessageList(messages);
        assertEquals(2, result.size());
        assertEquals("ai", result.get(1).getRole());
        assertNull(result.get(1).getProducts());
    }
}
