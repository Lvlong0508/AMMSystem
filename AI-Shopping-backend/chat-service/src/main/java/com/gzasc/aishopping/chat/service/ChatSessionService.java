package com.gzasc.aishopping.chat.service;

import com.gzasc.aishopping.chat.dto.SessionVO;
import java.util.List;

public interface ChatSessionService {
    SessionVO createSession(Long userId);
    List<SessionVO> listSessions(Long userId);
    void deleteSession(String sessionId);
    void updateTitle(String sessionId, String title);
    void touchUpdatedAt(String sessionId);
    Long getSessionUserId(String sessionId);
    boolean isSessionOwner(String sessionId, Long userId);
}
