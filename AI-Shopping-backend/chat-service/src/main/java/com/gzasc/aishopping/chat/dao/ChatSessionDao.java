package com.gzasc.aishopping.chat.dao;

import com.gzasc.aishopping.chat.entity.ChatSession;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;

public interface ChatSessionDao {
    void insert(ChatSession session);
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
    void deleteById(ObjectId id);
    void updateTitleAndUpdatedAt(ObjectId id, String title, Date updatedAt);
    void updateUpdatedAt(ObjectId id, Date updatedAt);
    ChatSession findById(ObjectId id);
    ChatSession findByIdAndUserId(ObjectId id, Long userId);
}
