package com.gzasc.aishopping.auth.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BCrypt 加盐加密工具测试类
 *
 * 核心测试点：
 * 1. 密码加密后长度正确
 * 2. 同一明文每次加密结果不同（随机 Salt）
 * 3. 正确密码能验证通过
 * 4. 错误密码验证失败
 * 5. 密码格式验证
 */
@DisplayName("BCryptUtil 加盐加密测试")
class BCryptUtilTest {

    @Test
    @DisplayName("密码加密后格式正确 - 应包含 BCrypt 特征前缀")
    void testHashPasswordFormat() {
        String plainPassword = "Test1234";
        String hashed = BCryptUtil.hashPassword(plainPassword);

        // 验证格式: $2a$12$...
        assertNotNull(hashed);
        assertTrue(hashed.startsWith("$2a$"), "密文应以 $2a$ 开头");
        assertTrue(hashed.contains("$"), "密文应包含分隔符 $");
        assertEquals(60, hashed.length(), "BCrypt 密文长度应为 60 字符");
    }

    @Test
    @DisplayName("同一明文多次加密结果不同 - 随机 Salt 生效")
    void testRandomSalt() {
        String plainPassword = "SamePassword123";
        String hash1 = BCryptUtil.hashPassword(plainPassword);
        String hash2 = BCryptUtil.hashPassword(plainPassword);

        // 两次加密结果应不同（因为使用了随机 Salt）
        assertNotEquals(hash1, hash2, "使用随机 Salt，相同密码应产生不同密文");

        // 但两者都能通过验证
        assertTrue(BCryptUtil.verifyPassword(plainPassword, hash1), "hash1 应能通过验证");
        assertTrue(BCryptUtil.verifyPassword(plainPassword, hash2), "hash2 应能通过验证");
    }

    @Test
    @DisplayName("正确密码验证通过")
    void testVerifyCorrectPassword() {
        String plainPassword = "CorrectPass123";
        String hashed = BCryptUtil.hashPassword(plainPassword);

        assertTrue(BCryptUtil.verifyPassword(plainPassword, hashed),
            "正确的密码应验证通过");
    }

    @Test
    @DisplayName("错误密码验证失败")
    void testVerifyWrongPassword() {
        String plainPassword = "CorrectPass123";
        String wrongPassword = "WrongPass456";
        String hashed = BCryptUtil.hashPassword(plainPassword);

        assertFalse(BCryptUtil.verifyPassword(wrongPassword, hashed),
            "错误的密码应验证失败");
    }

    @Test
    @DisplayName("空密码处理 - 应返回 false")
    void testEmptyPassword() {
        String hashed = BCryptUtil.hashPassword("SomePass123");

        assertFalse(BCryptUtil.verifyPassword(null, hashed), "null 密码应验证失败");
        assertFalse(BCryptUtil.verifyPassword("", hashed), "空密码应验证失败");
    }

    @Test
    @DisplayName("有效密码格式验证 - 应返回 true")
    void testValidPasswordFormat() {
        // 有效密码：6-20位，包含字母和数字
        assertTrue(BCryptUtil.isValidPasswordFormat("Pass123"), "字母+数字，7位应有效");
        assertTrue(BCryptUtil.isValidPasswordFormat("Password123"), "字母+数字，11位应有效");
        assertTrue(BCryptUtil.isValidPasswordFormat("P1ssw0rd!"), "包含特殊字符应有效");
        assertTrue(BCryptUtil.isValidPasswordFormat("123456aA"), "数字+大小写字母应有效");
    }

    @Test
    @DisplayName("无效密码格式验证 - 应返回 false")
    void testInvalidPasswordFormat() {
        // 太短
        assertFalse(BCryptUtil.isValidPasswordFormat("Pass1"), "5位密码应无效");

        // 太长
        assertFalse(BCryptUtil.isValidPasswordFormat("Password123456789012345"), "21位密码应无效");

        // 无数字
        assertFalse(BCryptUtil.isValidPasswordFormat("Password"), "纯字母应无效");

        // 无字母
        assertFalse(BCryptUtil.isValidPasswordFormat("12345678"), "纯数字应无效");

        // null
        assertFalse(BCryptUtil.isValidPasswordFormat(null), "null 应无效");
    }

    @Test
    @DisplayName("有效用户名格式验证 - 应返回 true")
    void testValidUsernameFormat() {
        assertTrue(BCryptUtil.isValidUsernameFormat("user001"), "字母数字应有效");
        assertTrue(BCryptUtil.isValidUsernameFormat("user_name"), "含下划线应有效");
        assertTrue(BCryptUtil.isValidUsernameFormat("usr"), "3位应有效");
        assertTrue(BCryptUtil.isValidUsernameFormat("username123456789012"), "20位应有效");
    }

    @Test
    @DisplayName("无效用户名格式验证 - 应返回 false")
    void testInvalidUsernameFormat() {
        // 太短
        assertFalse(BCryptUtil.isValidUsernameFormat("ab"), "2位应无效");

        // 太长
        assertFalse(BCryptUtil.isValidUsernameFormat("username1234567890123"), "21位应无效");

        // 特殊字符
        assertFalse(BCryptUtil.isValidUsernameFormat("user-name"), "含连字符应无效");
        assertFalse(BCryptUtil.isValidUsernameFormat("user.name"), "含点应无效");
        assertFalse(BCryptUtil.isValidUsernameFormat("user@name"), "含@应无效");
        assertFalse(BCryptUtil.isValidUsernameFormat("user name"), "含空格应无效");

        // null
        assertFalse(BCryptUtil.isValidUsernameFormat(null), "null 应无效");
    }

    @Test
    @DisplayName("从已有密文验证 - 模拟数据库验证场景")
    void testVerifyFromStoredHash() {
        // 模拟数据库中存储的密文（密码: 123456）
        String storedHash = "$2a$12$L69F5l0F8gB46Zi4W1xx6.nKdsV4B7vOERwO3iq0kXRwu8/H5L6q2";

        // 正确密码应通过
        assertTrue(BCryptUtil.verifyPassword("123456", storedHash),
            "数据库中的密文应能验证正确密码");

        // 错误密码应失败
        assertFalse(BCryptUtil.verifyPassword("1234567", storedHash),
            "错误密码应验证失败");
    }
}
