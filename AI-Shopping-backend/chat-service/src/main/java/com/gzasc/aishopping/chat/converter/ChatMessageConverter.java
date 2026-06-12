package com.gzasc.aishopping.chat.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.chat.dto.MessageVO;
import com.gzasc.aishopping.chat.dto.ProductData;
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

@Slf4j
public class ChatMessageConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    public static List<MessageVO> toMessageList(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<MessageVO> result = new ArrayList<>();
        List<ProductItem> pendingProducts = null;

        for (ChatMessage msg : messages) {
            if (msg instanceof SystemMessage) {
                continue;
            }

            if (msg instanceof UserMessage userMsg) {
                result.add(new MessageVO("user", userMsg.singleText(), null));
                continue;
            }

            if (msg instanceof ToolExecutionResultMessage toolMsg) {
                pendingProducts = parseProducts(toolMsg.text());
                continue;
            }

            if (msg instanceof AiMessage aiMsg) {
                String text = aiMsg.text();
                if (text == null) {
                    continue;
                }
                result.add(new MessageVO("ai", text, pendingProducts));
                pendingProducts = null;
            }
        }

        return result;
    }

    private static List<ProductItem> parseProducts(String json) {
        try {
            ProductData productData = objectMapper.readValue(json, ProductData.class);
            return productData.products();
        } catch (Exception e) {
            log.warn("Failed to parse ToolExecutionResultMessage JSON: {}", json, e);
            return null;
        }
    }
}
