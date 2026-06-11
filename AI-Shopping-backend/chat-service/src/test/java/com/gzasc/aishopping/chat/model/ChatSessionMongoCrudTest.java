package com.gzasc.aishopping.chat.model;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ChatSessionMongoCrudTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    // 使用固定的 ObjectId 便于测试（24位十六进制字符串）
    private static final String TEST_SESSION_ID = "507f1f77bcf86cd799439011";
    private final ObjectId sessionId = new ObjectId(TEST_SESSION_ID);

    @BeforeEach
    public void sessionTestInsert() {
        // 确保测试数据存在（避免重复插入异常）
        ChatSession existing = mongoTemplate.findById(sessionId, ChatSession.class);
        if (existing == null) {
            ChatSession session = new ChatSession();
            session.setSessionId(sessionId);
            session.setUserId(10001L);
            session.setTitle("初始会话标题");
            session.setCreatedAt(Instant.now());
            session.setUpdatedAt(Instant.now());
            mongoTemplate.insert(session);
        }
    }

    @AfterEach
    public void tearDown() {
        // 清理测试数据
        Query query = new Query(Criteria.where("_id").is(sessionId));
        mongoTemplate.remove(query, ChatSession.class);
    }

    @Test
    public void testInsert() {
        // 创建一个新的会话（使用不同的 ObjectId）
        ObjectId newId = new ObjectId();
        ChatSession newSession = new ChatSession();
        newSession.setSessionId(newId);
        newSession.setUserId(10002L);
        newSession.setTitle("新会话");
        newSession.setCreatedAt(Instant.now());
        newSession.setUpdatedAt(Instant.now());

        ChatSession inserted = mongoTemplate.insert(newSession);
        assertThat(inserted).isNotNull();
        assertThat(inserted.getSessionId()).isEqualTo(newId);

        // 清理本次插入的数据
        mongoTemplate.remove(new Query(Criteria.where("_id").is(newId)), ChatSession.class);
    }

    @Test
    public void testFindById() {
        ChatSession found = mongoTemplate.findById(sessionId, ChatSession.class);
        assertThat(found).isNotNull();
        assertThat(found.getUserId()).isEqualTo(10001L);
        assertThat(found.getTitle()).isEqualTo("初始会话标题");
        System.out.println("查询结果：" + found);
    }

    @Test
    public void testUpdate() {
        // 更新标题和更新时间
        Criteria criteria = Criteria.where("_id").is(sessionId);
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("title", "更新后的会话标题");
        update.set("updatedAt", Instant.now());

        // 使用 updateFirst 更新匹配的第一条记录（这里仅有一条）
        ChatSession updatedSession = mongoTemplate.findAndModify(query, update, ChatSession.class);
        assertThat(updatedSession).isNotNull();
        assertThat(updatedSession.getTitle()).isEqualTo("初始会话标题"); // 返回修改前的对象

        // 验证更新结果
        ChatSession afterUpdate = mongoTemplate.findById(sessionId, ChatSession.class);
        assertThat(afterUpdate.getTitle()).isEqualTo("更新后的会话标题");
    }

    @Test
    public void testDelete() {
        // 确保数据存在
        ChatSession beforeDelete = mongoTemplate.findById(sessionId, ChatSession.class);
        assertThat(beforeDelete).isNotNull();

        // 执行删除
        Query query = new Query(Criteria.where("_id").is(sessionId));
        mongoTemplate.remove(query, ChatSession.class);

        // 验证删除成功
        ChatSession afterDelete = mongoTemplate.findById(sessionId, ChatSession.class);
        assertThat(afterDelete).isNull();
    }
}