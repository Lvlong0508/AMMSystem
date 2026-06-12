package com.gzasc.aishopping.chat.config.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserContextTest {

    @Test
    @DisplayName("CH-038 getUserId - 正常设置并获取")
    void getUserId_normal() {
        UserContext.setUserId(100L);
        assertEquals(100L, UserContext.getUserId());
        UserContext.clear();
    }

    @Test
    @DisplayName("CH-039 getUserId - 大数字")
    void getUserId_largeNumber() {
        UserContext.setUserId(9999999999L);
        assertEquals(9999999999L, UserContext.getUserId());
        UserContext.clear();
    }

    @Test
    @DisplayName("CH-040 getUserId - 无上下文抛出异常")
    void getUserId_noContext() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, UserContext::getUserId);
        assertEquals("No user context available", ex.getMessage());
    }

    @Test
    @DisplayName("CH-041 getUserId - clear 后获取抛异常")
    void getUserId_afterClear() {
        UserContext.setUserId(100L);
        UserContext.clear();
        assertThrows(IllegalStateException.class, UserContext::getUserId);
    }

    @Test
    @DisplayName("CH-042 setUserId - 设置 null 后抛异常")
    void getUserId_afterSetNull() {
        UserContext.setUserId(null);
        IllegalStateException ex = assertThrows(IllegalStateException.class, UserContext::getUserId);
        assertEquals("No user context available", ex.getMessage());
        UserContext.clear();
    }
}
