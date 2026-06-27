package com.gzasc.aishopping.chat.controller;

import com.gzasc.aishopping.chat.dto.SessionVO;
import com.gzasc.aishopping.chat.service.ChatSessionService;
import com.gzasc.aishopping.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @PostMapping("/session")
    public ApiResponse<Map<String, String>> createSession(@RequestHeader("X-User-Id") Long userId) {
        SessionVO vo = chatSessionService.createSession(userId);
        return ApiResponse.success(Map.of("sessionId", vo.getId()));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionVO>> listSessions(@RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(chatSessionService.listSessions(userId));
    }

    @DeleteMapping("/session/{sessionId}")
    public ApiResponse<Void> deleteSession(@RequestHeader("X-User-Id") Long userId,
                                           @PathVariable String sessionId) {
        if (!chatSessionService.isSessionOwner(sessionId, userId)) {
            return ApiResponse.error(403, "无权删除该会话");
        }
        chatSessionService.deleteSession(sessionId);
        return ApiResponse.success(null);
    }
}
