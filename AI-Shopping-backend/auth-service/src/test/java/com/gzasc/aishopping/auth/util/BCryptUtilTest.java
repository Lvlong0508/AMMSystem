package com.gzasc.aishopping.auth.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptUtilTest {

    @Test
    @DisplayName("AU-030 hashPassword 生成有效 BCrypt 哈希")
    void hashPassword_shouldReturnValidBcryptHash() {
        String hash1 = BCryptUtil.hashPassword("Abc123");
        String hash2 = BCryptUtil.hashPassword("Abc123");

        assertTrue(hash1.startsWith("$2a$"));
        assertEquals(60, hash1.length());
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("AU-031 正确密码验证通过")
    void verifyPassword_shouldReturnTrueForCorrectPassword() {
        String hash = BCryptUtil.hashPassword("Abc123");

        assertTrue(BCryptUtil.verifyPassword("Abc123", hash));
    }

    @Test
    @DisplayName("AU-032 错误密码验证失败")
    void verifyPassword_shouldReturnFalseForWrongPassword() {
        String hash = BCryptUtil.hashPassword("Abc123");

        assertFalse(BCryptUtil.verifyPassword("WrongPass1", hash));
    }

    @Test
    @DisplayName("AU-033 BCrypt 强度参数为 12")
    void hashPassword_shouldUseStrength12() {
        String hash = BCryptUtil.hashPassword("Abc123");

        assertTrue(hash.startsWith("$2a$12$"));
    }
}
