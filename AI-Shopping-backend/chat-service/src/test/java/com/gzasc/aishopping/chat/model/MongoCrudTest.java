package com.gzasc.aishopping.chat.model;

import com.gzasc.aishopping.chat.entity.ChatMessages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@SpringBootTest
public class MongoCrudTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void messageTestInsert() {
        ChatMessages chatMessage = new ChatMessages();
        chatMessage.setContent("聊天记录列表");
        mongoTemplate.insert(chatMessage);
    }

    @Test
    public void testFindById() {
        ChatMessages chatMessage = mongoTemplate.findById("6a2acce922ad9a3c3819969c", ChatMessages.class);
        System.out.println(chatMessage);
    }

    @Test
    public void testUpdate() {
        Criteria criteria = Criteria.where("_id").is("6a2acce922ad9a3c3819969c");
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("content", "新的聊天记录列表");

        mongoTemplate.upsert(query, update, ChatMessages.class);
    }

    @Test
    public void testDelete() {
        Criteria criteria = Criteria.where("_id").is("6a2acce922ad9a3c3819969c");
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class);
    }
}
