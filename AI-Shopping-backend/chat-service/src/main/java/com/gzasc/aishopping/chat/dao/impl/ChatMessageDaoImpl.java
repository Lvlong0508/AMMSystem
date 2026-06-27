package com.gzasc.aishopping.chat.dao.impl;

import com.gzasc.aishopping.chat.dao.ChatMessageDao;
import com.gzasc.aishopping.chat.entity.ChatMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatMessageDaoImpl implements ChatMessageDao {

    private final MongoTemplate mongoTemplate;

    @Override
    public String findContentByMemoryId(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        ChatMessages message = mongoTemplate.findOne(query, ChatMessages.class);
        return message != null ? message.getContent() : null;
    }

    @Override
    public void upsertContent(Object memoryId, String content) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("content", content);
        mongoTemplate.upsert(query, update, ChatMessages.class);
    }

    @Override
    public void deleteByMemoryId(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class);
    }
}
