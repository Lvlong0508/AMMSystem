package com.gzasc.aishopping.chat.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.chat.dto.MessageVO;
import com.gzasc.aishopping.chat.dto.ProductItem;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class ChatMessageConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String FORMAT_MARKER = "You must answer strictly in the following JSON format:";

    public static List<MessageVO> toMessageList(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<MessageVO> result = new ArrayList<>();

        for (ChatMessage msg : messages) {
            if (msg instanceof SystemMessage) {
                continue;
            }

            if (msg instanceof UserMessage userMsg) {
                String text = cleanUserText(userMsg.singleText());
                result.add(new MessageVO("user", text, null));
                continue;
            }

            if (msg instanceof ToolExecutionResultMessage) {
                continue;
            }

            if (msg instanceof AiMessage aiMsg) {
                String rawText = aiMsg.text();
                if (rawText == null || rawText.isBlank()) {
                    continue;
                }
                ParsedAiResult parsed = parseAiText(rawText);
                result.add(new MessageVO("ai", parsed.text, parsed.products));
            }
        }

        return result;
    }

    static String cleanUserText(String text) {
        if (text == null) {
            return null;
        }
        int idx = text.indexOf(FORMAT_MARKER);
        if (idx >= 0) {
            return text.substring(0, idx).trim();
        }
        return text;
    }

    static String extractJsonBlock(String text) {
        if (text == null) return null;
        int start = text.indexOf("```");
        if (start < 0) return null;
        start = text.indexOf("\n", start);
        if (start < 0) return null;
        int end = text.lastIndexOf("```");
        if (end <= start) return null;
        return text.substring(start, end).trim();
    }

    @SuppressWarnings("unchecked")
    static ParsedAiResult parseAiText(String rawText) {
        String text = rawText;
        List<ProductItem> products = null;
        try {
            String jsonStr = extractJsonBlock(rawText);
            if (jsonStr == null) {
                jsonStr = rawText.trim();
            }
            Map<String, Object> map = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
            if (map.containsKey("message")) {
                Object msgField = map.get("message");
                if (msgField != null) {
                    text = msgField.toString();
                }
                Object dataField = map.get("data");
                if (dataField instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) dataField;
                    if ("product".equals(data.get("type")) && data.get("products") instanceof List) {
                        List<Map<String, Object>> productList = (List<Map<String, Object>>) data.get("products");
                        products = productList.stream().map(p -> new ProductItem(
                                toLong(p.get("id")),
                                (String) p.get("name"),
                                toDouble(p.get("price")),
                                (String) p.get("tags"),
                                (String) p.get("description"),
                                toInt(p.get("stock")),
                                (String) p.get("imageUrl"),
                                (String) p.get("shopName")
                        )).toList();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("AiMessage text is not JSON, treating as plain text: {}", e.getMessage());
        }
        return new ParsedAiResult(text, products);
    }

    private static Long toLong(Object obj) {
        if (obj instanceof Number) return ((Number) obj).longValue();
        return null;
    }

    private static Double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        return null;
    }

    private static Integer toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        return null;
    }

    record ParsedAiResult(String text, List<ProductItem> products) {}
}
