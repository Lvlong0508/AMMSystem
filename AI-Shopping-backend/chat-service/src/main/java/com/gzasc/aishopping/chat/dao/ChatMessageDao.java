package com.gzasc.aishopping.chat.dao;

public interface ChatMessageDao {
    String findContentByMemoryId(Object memoryId);
    void upsertContent(Object memoryId, String content);
    void deleteByMemoryId(Object memoryId);
}
