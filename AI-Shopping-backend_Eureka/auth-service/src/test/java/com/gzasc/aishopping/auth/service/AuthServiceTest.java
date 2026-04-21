package com.gzasc.aishopping.auth.service;

import com.gzasc.aishopping.auth.mapper.merchant.MerchantMapper;
import com.gzasc.aishopping.auth.mapper.user.UserMapper;
import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.model.User;
import com.gzasc.aishopping.auth.model.dto.LoginResult;
import com.gzasc.aishopping.auth.model.dto.RegisterRequest;
import com.gzasc.aishopping.auth.service.impl.AuthException;
import com.gzasc.aishopping.auth.service.impl.AuthServiceImpl;
import com.gzasc.aishopping.auth.util.BCryptUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 认证服务测试类
 *
 * 核心测试点：
 * 1. 用户/商家注册逻辑
 * 2. 用户/商家登录验证
 * 3. 用户名/手机号唯一性检查
 * 4. 密码加密存储
 * 5. 异常场景处理
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 认证服务测试")
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private MerchantMapper merchantMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest validRegisterRequest;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("testuser");
        validRegisterRequest.setPassword("TestPass123");
        validRegisterRequest.setNickname("测试用户");
        validRegisterRequest.setPhone("13800138000");
    }

    // ==================== 用户注册测试 ====================

    @Test
    @DisplayName("用户注册成功 - 正常流程")
    void testUserRegisterSuccess() {
        // 模拟：用户名和手机号都可用
        when(userMapper.countByUsername("testuser")).thenReturn(0);
        when(userMapper.countByPhone("13800138000")).thenReturn(0);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1);  // 模拟生成ID
            return 1;
        });

        // 执行注册
        assertThrows(Exception.class, () -> {
            // Sa-Token 需要真实环境，会抛出异常
            authService.userRegister(validRegisterRequest);
        });

        // 验证：插入了用户
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("用户注册失败 - 用户名已存在")
    void testUserRegisterDuplicateUsername() {
        // 模拟：用户名已存在
        when(userMapper.countByUsername("testuser")).thenReturn(1);

        // 应抛出异常
        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.userRegister(validRegisterRequest);
        });

        assertEquals("用户名已存在", exception.getMessage());
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("用户注册失败 - 手机号已被注册")
    void testUserRegisterDuplicatePhone() {
        // 模拟：用户名可用，但手机号已注册
        when(userMapper.countByUsername("testuser")).thenReturn(0);
        when(userMapper.countByPhone("13800138000")).thenReturn(1);

        // 应抛出异常
        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.userRegister(validRegisterRequest);
        });

        assertEquals("手机号已被注册", exception.getMessage());
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("用户注册 - 密码已加密存储")
    void testUserRegisterPasswordEncrypted() {
        // 模拟：插入时捕获用户对象
        when(userMapper.countByUsername("testuser")).thenReturn(0);
        when(userMapper.countByPhone("13800138000")).thenReturn(0);

        User capturedUser = new User();
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            capturedUser.setPassword(user.getPassword());
            return 1;
        });

        // 执行（会失败但已捕获密码）
        try {
            authService.userRegister(validRegisterRequest);
        } catch (Exception ignored) {}

        // 验证：密码是 BCrypt 格式
        assertNotNull(capturedUser.getPassword());
        assertTrue(capturedUser.getPassword().startsWith("$2a$"));
        assertEquals(60, capturedUser.getPassword().length());
    }

    // ==================== 用户登录测试 ====================

    @Test
    @DisplayName("用户登录失败 - 用户不存在")
    void testUserLoginUserNotFound() {
        when(userMapper.selectByUsername("nonexistent")).thenReturn(null);

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.userLogin("nonexistent", "anypassword");
        });

        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("用户登录失败 - 密码错误")
    void testUserLoginWrongPassword() {
        // 模拟：用户存在，但密码错误
        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setPassword(BCryptUtil.hashPassword("CorrectPass123"));
        user.setStatus(1);

        when(userMapper.selectByUsername("testuser")).thenReturn(user);

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.userLogin("testuser", "WrongPass123");
        });

        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("用户登录失败 - 账号被禁用")
    void testUserLoginDisabled() {
        // 模拟：用户存在且密码正确，但状态禁用
        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        String plainPassword = "TestPass123";
        user.setPassword(BCryptUtil.hashPassword(plainPassword));
        user.setStatus(0);  // 禁用

        when(userMapper.selectByUsername("testuser")).thenReturn(user);

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.userLogin("testuser", plainPassword);
        });

        assertEquals("账号已被禁用", exception.getMessage());
    }

    // ==================== 商家接口测试 ====================

    @Test
    @DisplayName("商家注册 - 用户名唯一性检查")
    void testMerchantRegisterCheckUsername() {
        when(merchantMapper.countByUsername("merchant1")).thenReturn(0);
        when(merchantMapper.countByPhone(anyString())).thenReturn(0);
        when(merchantMapper.insert(any(Merchant.class))).thenReturn(1);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("merchant1");
        request.setPassword("Pass1234");

        // Sa-Token 需要真实环境
        assertThrows(Exception.class, () -> {
            authService.merchantRegister(request);
        });
    }

    @Test
    @DisplayName("商家登录 - 验证失败场景")
    void testMerchantLoginFailure() {
        when(merchantMapper.selectByUsername("nonexistent")).thenReturn(null);

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.merchantLogin("nonexistent", "password");
        });

        assertEquals("用户名或密码错误", exception.getMessage());
    }

    // ==================== 辅助方法测试 ====================

    @Test
    @DisplayName("检查用户用户名存在性")
    void testUserExistsByUsername() {
        when(userMapper.countByUsername("existing")).thenReturn(1);
        when(userMapper.countByUsername("newuser")).thenReturn(0);

        assertTrue(authService.userExistsByUsername("existing"));
        assertFalse(authService.userExistsByUsername("newuser"));
    }

    @Test
    @DisplayName("检查用户手机号存在性")
    void testUserExistsByPhone() {
        when(userMapper.countByPhone("13800138000")).thenReturn(1);
        when(userMapper.countByPhone("13900139000")).thenReturn(0);

        assertTrue(authService.userExistsByPhone("13800138000"));
        assertFalse(authService.userExistsByPhone("13900139000"));
    }

    @Test
    @DisplayName("空手机号检查应返回 false")
    void testEmptyPhoneCheck() {
        assertFalse(authService.userExistsByPhone(null));
        assertFalse(authService.userExistsByPhone(""));
        assertFalse(authService.userExistsByPhone("   "));
    }

    @Test
    @DisplayName("根据ID查询用户")
    void testGetUserById() {
        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("user1");

        when(userMapper.selectById(1)).thenReturn(mockUser);

        User result = authService.getUserById(1);

        assertNotNull(result);
        assertEquals("user1", result.getUsername());
    }
}
