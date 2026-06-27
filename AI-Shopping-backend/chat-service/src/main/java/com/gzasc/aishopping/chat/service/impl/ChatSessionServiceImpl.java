package com.gzasc.aishopping.chat.service.impl;

import com.gzasc.aishopping.chat.dao.ChatMessageDao;
import com.gzasc.aishopping.chat.dao.ChatSessionDao;
import com.gzasc.aishopping.chat.dto.SessionVO;
import com.gzasc.aishopping.chat.entity.ChatSession;
import com.gzasc.aishopping.chat.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionDao chatSessionDao;
    private final ChatMessageDao chatMessageDao;

    @Override
    public SessionVO createSession(Long userId) {
        ChatSession session = new ChatSession();
        session.setId(new ObjectId());
        session.setUserId(userId);
        session.setTitle("新对话");
        session.setCreatedAt(new Date());
        session.setUpdatedAt(new Date());
        chatSessionDao.insert(session);
        return toVO(session);
    }

    @Override
    public List<SessionVO> listSessions(Long userId) {
        return chatSessionDao.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        ObjectId id = safeParseObjectId(sessionId);
        chatSessionDao.deleteById(id);
        chatMessageDao.deleteByMemoryId(sessionId);
    }

    @Override
    public void updateTitle(String sessionId, String title) {
        ObjectId id = safeParseObjectId(sessionId);
        chatSessionDao.updateTitleAndUpdatedAt(id, title, new Date());
    }

    @Override
    public void touchUpdatedAt(String sessionId) {
        ObjectId id = safeParseObjectId(sessionId);
        chatSessionDao.updateUpdatedAt(id, new Date());
    }

    @Override
    public Long getSessionUserId(String sessionId) {
        ObjectId id = safeParseObjectId(sessionId);
        ChatSession session = chatSessionDao.findById(id);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }
        return session.getUserId();
    }

    @Override
    public boolean isSessionOwner(String sessionId, Long userId) {
        ObjectId id = safeParseObjectId(sessionId);
        return chatSessionDao.findByIdAndUserId(id, userId) != null;
    }

    private static ObjectId safeParseObjectId(String sessionId) {
        if (!ObjectId.isValid(sessionId)) {
            throw new IllegalArgumentException("无效的会话ID，不是合法的ObjectId格式: " + sessionId);
        }
        return new ObjectId(sessionId);
    }

    private SessionVO toVO(ChatSession s) {
        return new SessionVO(s.getId().toString(), s.getTitle(), s.getUpdatedAt().toString());
    }
}
