package com.gzasc.aishopping.chat.service.impl;

import com.gzasc.aishopping.chat.dao.ChatMessageDao;
import com.gzasc.aishopping.chat.service.ChatMessageService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageDao chatMessageDao;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String content = chatMessageDao.findContentByMemoryId(memoryId);
        if (content == null) {
            return new LinkedList<>();
        }
        return ChatMessageDeserializer.messagesFromJson(content);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        chatMessageDao.upsertContent(memoryId, ChatMessageSerializer.messagesToJson(messages));
    }

    @Override
    public void deleteMessages(Object memoryId) {
        chatMessageDao.deleteByMemoryId(memoryId);
    }
}
