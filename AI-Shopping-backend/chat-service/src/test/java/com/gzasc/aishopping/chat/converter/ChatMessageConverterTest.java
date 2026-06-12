package com.gzasc.aishopping.chat.converter;

import com.gzasc.aishopping.chat.dto.MessageVO;
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

    private static final String PRODUCT_JSON = """
            {
              "message": "为您找到以下商品",
              "reason": "调用接口获取商品列表",
              "data": {
                "type": "product",
                "products": [
                  {"id":1,"name":"手机","price":2999.0,"tags":"tag","description":"desc","stock":100,"imageUrl":"url","shopName":"shop"}
                ]
              }
            }""";

    private static final String FENCED_PRODUCT_JSON = "```json\n" + PRODUCT_JSON + "\n```";

    @Test
    @DisplayName("空列表返回空 List")
    void emptyList() {
        assertTrue(ChatMessageConverter.toMessageList(List.of()).isEmpty());
    }

    @Test
    @DisplayName("null 返回空 List")
    void nullList() {
        assertTrue(ChatMessageConverter.toMessageList(null).isEmpty());
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
    }

    @Test
    @DisplayName("ToolExecutionResultMessage 被跳过")
    void toolExecutionResultSkipped() {
        var messages = List.<ChatMessage>of(
                new UserMessage("推荐商品"),
                new ToolExecutionResultMessage("tool", "{}", "[{\"id\":1}]"),
                new AiMessage("结果是")
        );
        List<MessageVO> result = ChatMessageConverter.toMessageList(messages);
        assertEquals(2, result.size());
        assertEquals("user", result.get(0).getRole());
        assertEquals("ai", result.get(1).getRole());
    }

    @Test
    @DisplayName("简单对话 — UserMessage + 纯文本 AiMessage")
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
    @DisplayName("用户消息截断格式指令")
    void userTextCleaned() {
        String dirty = "有什么商品\nYou must answer strictly in the following JSON format: {\"message\": \"...\"}";
        assertEquals("有什么商品", ChatMessageConverter.cleanUserText(dirty));
    }

    @Test
    @DisplayName("用户消息无格式指令时原样返回")
    void userTextNoFormatter() {
        assertEquals("你好", ChatMessageConverter.cleanUserText("你好"));
    }

    @Test
    @DisplayName("AI JSON 回复（带 markdown 围栏）")
    void aiJsonWithFences() {
        var parsed = ChatMessageConverter.parseAiText(FENCED_PRODUCT_JSON);
        assertEquals("为您找到以下商品", parsed.text());
        assertNotNull(parsed.products());
        assertEquals(1, parsed.products().size());
        assertEquals("手机", parsed.products().get(0).name());
    }

    @Test
    @DisplayName("AI JSON 回复（无围栏）")
    void aiJsonWithoutFences() {
        var parsed = ChatMessageConverter.parseAiText(PRODUCT_JSON);
        assertEquals("为您找到以下商品", parsed.text());
        assertNotNull(parsed.products());
        assertEquals(1, parsed.products().size());
    }

    @Test
    @DisplayName("AI 纯文本回复（非 JSON）")
    void aiPlainText() {
        var parsed = ChatMessageConverter.parseAiText("您好，请问有什么可以帮您？");
        assertEquals("您好，请问有什么可以帮您？", parsed.text());
        assertNull(parsed.products());
    }

    @Test
    @DisplayName("带 tool call 的完整对话轮次")
    void fullToolCallConversation() {
        var messages = List.<ChatMessage>of(
                new SystemMessage("system"),
                new UserMessage("有什么商品\nYou must answer strictly in the following JSON format: {...}"),
                new AiMessage(""),
                new ToolExecutionResultMessage("tool", "{}", "[{\"id\":1}]"),
                new AiMessage(FENCED_PRODUCT_JSON)
        );
        List<MessageVO> result = ChatMessageConverter.toMessageList(messages);
        assertEquals(2, result.size());

        assertEquals("user", result.get(0).getRole());
        assertEquals("有什么商品", result.get(0).getText());

        assertEquals("ai", result.get(1).getRole());
        assertEquals("为您找到以下商品", result.get(1).getText());
        assertNotNull(result.get(1).getProducts());
        assertEquals(1, result.get(1).getProducts().size());
    }

    @Test
    @DisplayName("连续多轮对话")
    void multiTurnConversation() {
        var messages = List.<ChatMessage>of(
                new UserMessage("你好"),
                new AiMessage("你好！"),
                new UserMessage("有什么商品\nYou must answer strictly in the following JSON format: {...}"),
                new AiMessage(FENCED_PRODUCT_JSON)
        );
        List<MessageVO> result = ChatMessageConverter.toMessageList(messages);
        assertEquals(4, result.size());

        assertNull(result.get(0).getProducts());
        assertNull(result.get(1).getProducts());
        assertNull(result.get(2).getProducts());
        assertNotNull(result.get(3).getProducts());
    }
}
