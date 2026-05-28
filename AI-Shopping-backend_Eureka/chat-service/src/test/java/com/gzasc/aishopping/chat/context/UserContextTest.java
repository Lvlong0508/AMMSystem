package com.gzasc.aishopping.chat.context;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserContextTest {

    @Test
    @DisplayName("CH-038 getUserId - 正常解析")
    void getUserId_normal() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-User-Id")).thenReturn("100");
        ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
        when(attrs.getRequest()).thenReturn(request);

        try (MockedStatic<RequestContextHolder> rch = Mockito.mockStatic(RequestContextHolder.class)) {
            rch.when(RequestContextHolder::getRequestAttributes).thenReturn(attrs);

            Long userId = UserContext.getUserId();
            assertEquals(100L, userId);
        }
    }

    @Test
    @DisplayName("CH-039 getUserId - X-User-Id 为数字字符串")
    void getUserId_numericString() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-User-Id")).thenReturn("42");
        ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
        when(attrs.getRequest()).thenReturn(request);

        try (MockedStatic<RequestContextHolder> rch = Mockito.mockStatic(RequestContextHolder.class)) {
            rch.when(RequestContextHolder::getRequestAttributes).thenReturn(attrs);

            Long userId = UserContext.getUserId();
            assertEquals(42L, userId);
        }
    }

    @Test
    @DisplayName("CH-040 getUserId - X-User-Id 为大数字")
    void getUserId_largeNumber() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-User-Id")).thenReturn("9999999999");
        ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
        when(attrs.getRequest()).thenReturn(request);

        try (MockedStatic<RequestContextHolder> rch = Mockito.mockStatic(RequestContextHolder.class)) {
            rch.when(RequestContextHolder::getRequestAttributes).thenReturn(attrs);

            Long userId = UserContext.getUserId();
            assertEquals(9999999999L, userId);
        }
    }

    @Test
    @DisplayName("CH-041 getUserId - 无请求上下文")
    void getUserId_noRequestContext() {
        try (MockedStatic<RequestContextHolder> rch = Mockito.mockStatic(RequestContextHolder.class)) {
            rch.when(RequestContextHolder::getRequestAttributes).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class, UserContext::getUserId);
            assertEquals("No request context available", ex.getMessage());
        }
    }

    @Test
    @DisplayName("CH-042 getUserId - X-User-Id Header 缺失")
    void getUserId_missingHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-User-Id")).thenReturn(null);
        ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
        when(attrs.getRequest()).thenReturn(request);

        try (MockedStatic<RequestContextHolder> rch = Mockito.mockStatic(RequestContextHolder.class)) {
            rch.when(RequestContextHolder::getRequestAttributes).thenReturn(attrs);

            RuntimeException ex = assertThrows(RuntimeException.class, UserContext::getUserId);
            assertEquals("X-User-Id header is missing", ex.getMessage());
        }
    }

    @Test
    @DisplayName("CH-043 getUserId - X-User-Id Header 为空串")
    void getUserId_emptyHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-User-Id")).thenReturn("");
        ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
        when(attrs.getRequest()).thenReturn(request);

        try (MockedStatic<RequestContextHolder> rch = Mockito.mockStatic(RequestContextHolder.class)) {
            rch.when(RequestContextHolder::getRequestAttributes).thenReturn(attrs);

            RuntimeException ex = assertThrows(RuntimeException.class, UserContext::getUserId);
            assertEquals("X-User-Id header is missing", ex.getMessage());
        }
    }

    @Test
    @DisplayName("CH-044 getUserId - X-User-Id 为非数字")
    void getUserId_nonNumeric() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-User-Id")).thenReturn("abc");
        ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
        when(attrs.getRequest()).thenReturn(request);

        try (MockedStatic<RequestContextHolder> rch = Mockito.mockStatic(RequestContextHolder.class)) {
            rch.when(RequestContextHolder::getRequestAttributes).thenReturn(attrs);

            assertThrows(NumberFormatException.class, UserContext::getUserId);
        }
    }
}
