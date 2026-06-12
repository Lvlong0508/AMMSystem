package com.gzasc.aishopping.chat.service.impl;

import com.gzasc.aishopping.chat.dto.SessionVO;
import com.gzasc.aishopping.chat.entity.ChatSession;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatSessionService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public SessionVO createSession(Long userId) {
        ChatSession session = new ChatSession();
        session.setId(new ObjectId());
        session.setUserId(userId);
        session.setTitle("新对话");
        session.setCreatedAt(new Date());
        session.setUpdatedAt(new Date());
        mongoTemplate.insert(session);
        return toVO(session);
    }

    public List<SessionVO> listSessions(Long userId) {
        Query query = new Query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "updatedAt"));
        List<ChatSession> sessions = mongoTemplate.find(query, ChatSession.class);
        return sessions.stream().map(this::toVO).collect(Collectors.toList());
    }

    public void deleteSession(String sessionId) {
        ObjectId id = new ObjectId(sessionId);
        mongoTemplate.remove(new Query(Criteria.where("id").is(id)), ChatSession.class);
        mongoTemplate.remove(new Query(Criteria.where("memoryId").is(sessionId)), "chat_message");
    }

    public void updateTitle(String sessionId, String title) {
        ObjectId id = new ObjectId(sessionId);
        Query query = new Query(Criteria.where("id").is(id));
        org.springframework.data.mongodb.core.query.Update update =
                new org.springframework.data.mongodb.core.query.Update();
        update.set("title", title);
        update.set("updatedAt", new Date());
        mongoTemplate.updateFirst(query, update, ChatSession.class);
    }

    public void touchUpdatedAt(String sessionId) {
        ObjectId id = new ObjectId(sessionId);
        Query query = new Query(Criteria.where("id").is(id));
        org.springframework.data.mongodb.core.query.Update update =
                new org.springframework.data.mongodb.core.query.Update();
        update.set("updatedAt", new Date());
        mongoTemplate.updateFirst(query, update, ChatSession.class);
    }

    public Long getSessionUserId(String sessionId) {
        ObjectId id = new ObjectId(sessionId);
        ChatSession session = mongoTemplate.findOne(
                new Query(Criteria.where("id").is(id)),
                ChatSession.class);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }
        return session.getUserId();
    }

    public boolean isSessionOwner(String sessionId, Long userId) {
        ObjectId id = new ObjectId(sessionId);
        ChatSession session = mongoTemplate.findOne(
                new Query(Criteria.where("id").is(id).and("userId").is(userId)),
                ChatSession.class);
        return session != null;
    }

    private SessionVO toVO(ChatSession s) {
        return new SessionVO(s.getId().toString(), s.getTitle(), s.getUpdatedAt().toString());
    }
}
