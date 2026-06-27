package com.gzasc.aishopping.chat.dao.impl;

import com.gzasc.aishopping.chat.dao.ChatSessionDao;
import com.gzasc.aishopping.chat.entity.ChatSession;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatSessionDaoImpl implements ChatSessionDao {

    private final MongoTemplate mongoTemplate;

    @Override
    public void insert(ChatSession session) {
        mongoTemplate.insert(session);
    }

    @Override
    public List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId) {
        Query query = new Query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "updatedAt"));
        return mongoTemplate.find(query, ChatSession.class);
    }

    @Override
    public void deleteById(ObjectId id) {
        mongoTemplate.remove(new Query(Criteria.where("id").is(id)), ChatSession.class);
    }

    @Override
    public void updateTitleAndUpdatedAt(ObjectId id, String title, Date updatedAt) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update();
        update.set("title", title);
        update.set("updatedAt", updatedAt);
        mongoTemplate.updateFirst(query, update, ChatSession.class);
    }

    @Override
    public void updateUpdatedAt(ObjectId id, Date updatedAt) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update();
        update.set("updatedAt", updatedAt);
        mongoTemplate.updateFirst(query, update, ChatSession.class);
    }

    @Override
    public ChatSession findById(ObjectId id) {
        Query query = new Query(Criteria.where("id").is(id));
        return mongoTemplate.findOne(query, ChatSession.class);
    }

    @Override
    public ChatSession findByIdAndUserId(ObjectId id, Long userId) {
        Query query = new Query(Criteria.where("id").is(id).and("userId").is(userId));
        return mongoTemplate.findOne(query, ChatSession.class);
    }
}
