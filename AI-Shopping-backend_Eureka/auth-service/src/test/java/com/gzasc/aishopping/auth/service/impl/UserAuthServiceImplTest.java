package com.gzasc.aishopping.auth.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.gzasc.aishopping.auth.dto.LoginResult;
import com.gzasc.aishopping.auth.dto.RegisterRequest;
import com.gzasc.aishopping.auth.dto.UpdateProfileRequest;
import com.gzasc.aishopping.auth.exception.AuthException;
import com.gzasc.aishopping.auth.mapper.user.UserMapper;
import com.gzasc.aishopping.auth.model.User;
import com.gzasc.aishopping.auth.model.UserInfo;
import com.gzasc.aishopping.auth.service.UserInfoService;
import com.gzasc.aishopping.auth.util.BCryptUtil;
import com.gzasc.aishopping.common.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserInfoService userInfoService;

    @InjectMocks
    private UserAuthServiceImpl userAuthService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    // ────────────────────────────── register ──────────────────────────────

    @Test
    @DisplayName("register 应成功注册用户并返回 LoginResult")
    void register_shouldSucceed() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("Abc123");
        request.setNickname("昵称");
        request.setPhone("13800138001");

        when(userMapper.countByUsername("newuser")).thenReturn(0);
        when(userMapper.countByPhone("13800138001")).thenReturn(0);

        UserInfo userInfo = new UserInfo();
        userInfo.setId(1);
        when(userInfoService.createUserInfo(any())).thenReturn(1);

        SaSession mockSession = mock(SaSession.class);

        try (MockedStatic<SnowflakeIdGenerator> sf = mockStatic(SnowflakeIdGenerator.class);
             MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class);
             MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {

            sf.when(SnowflakeIdGenerator::nextId).thenReturn(100L);
            bc.when(() -> BCryptUtil.hashPassword("Abc123")).thenReturn("$2a$12$hashed");
            stp.when(() -> StpUtil.login(100L)).then(invocation -> null);
            stp.when(StpUtil::getTokenSession).thenReturn(mockSession);
            stp.when(StpUtil::getTokenValue).thenReturn("test-token");

            LoginResult result = userAuthService.register(request);

            assertNotNull(result);
            assertEquals("test-token", result.getToken());
            assertEquals("USER", result.getAccountType());
            assertNotNull(result.getAccount());
            assertEquals(100L, ((User) result.getAccount()).getId());

            verify(userMapper).countByUsername("newuser");
            verify(userMapper).countByPhone("13800138001");
            verify(userInfoService).createUserInfo(any(UserInfo.class));
            verify(userMapper).insert(userCaptor.capture());

            User inserted = userCaptor.getValue();
            assertEquals(100L, inserted.getId());
            assertEquals("newuser", inserted.getUsername());
            assertEquals("$2a$12$hashed", inserted.getPassword());
            assertEquals("13800138001", inserted.getPhone());
            assertEquals(1, inserted.getInfoId());
            assertEquals(1, inserted.getStatus());

            stp.verify(() -> StpUtil.login(100L));
            stp.verify(StpUtil::getTokenSession);
            stp.verify(StpUtil::getTokenValue);
            verify(mockSession).set("accountType", "USER");
        }
    }

    @Test
    @DisplayName("register 无昵称时不应创建 UserInfo")
    void register_withoutNickname_shouldSkipUserInfo() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("Abc123");
        request.setPhone("13800138001");

        when(userMapper.countByUsername("newuser")).thenReturn(0);
        when(userMapper.countByPhone("13800138001")).thenReturn(0);

        SaSession mockSession = mock(SaSession.class);

        try (MockedStatic<SnowflakeIdGenerator> sf = mockStatic(SnowflakeIdGenerator.class);
             MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class);
             MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {

            sf.when(SnowflakeIdGenerator::nextId).thenReturn(101L);
            bc.when(() -> BCryptUtil.hashPassword("Abc123")).thenReturn("$2a$12$hashed");
            stp.when(() -> StpUtil.login(101L)).then(invocation -> null);
            stp.when(StpUtil::getTokenSession).thenReturn(mockSession);
            stp.when(StpUtil::getTokenValue).thenReturn("token-no-nick");

            LoginResult result = userAuthService.register(request);

            verify(userInfoService, never()).createUserInfo(any());
            verify(userMapper).insert(userCaptor.capture());
            assertNull(userCaptor.getValue().getInfoId());
            assertEquals("token-no-nick", result.getToken());
        }
    }

    @Test
    @DisplayName("register 用户名已存在时应抛出异常")
    void register_duplicateUsername_shouldThrow() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setPassword("Abc123");

        when(userMapper.countByUsername("existing")).thenReturn(1);

        AuthException ex = assertThrows(AuthException.class, () -> userAuthService.register(request));
        assertEquals("用户名已存在", ex.getMessage());
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("register 手机号已注册时应抛出异常")
    void register_duplicatePhone_shouldThrow() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("Abc123");
        request.setPhone("13800138001");

        when(userMapper.countByUsername("newuser")).thenReturn(0);
        when(userMapper.countByPhone("13800138001")).thenReturn(1);

        AuthException ex = assertThrows(AuthException.class, () -> userAuthService.register(request));
        assertEquals("手机号已被注册", ex.getMessage());
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("register 手机号为 null 时不应检查手机号重复")
    void register_nullPhone_shouldSkipPhoneCheck() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("Abc123");

        when(userMapper.countByUsername("newuser")).thenReturn(0);

        SaSession mockSession = mock(SaSession.class);

        try (MockedStatic<SnowflakeIdGenerator> sf = mockStatic(SnowflakeIdGenerator.class);
             MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class);
             MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {

            sf.when(SnowflakeIdGenerator::nextId).thenReturn(102L);
            bc.when(() -> BCryptUtil.hashPassword("Abc123")).thenReturn("$2a$12$hashed");
            stp.when(() -> StpUtil.login(102L)).then(invocation -> null);
            stp.when(StpUtil::getTokenSession).thenReturn(mockSession);
            stp.when(StpUtil::getTokenValue).thenReturn("token");

            userAuthService.register(request);

            verify(userMapper, never()).countByPhone(anyString());
        }
    }

    // ────────────────────────────── login ──────────────────────────────

    @Test
    @DisplayName("login 应成功登录并返回 LoginResult")
    void login_shouldSucceed() {
        User user = new User();
        user.setId(100L);
        user.setUsername("testuser");
        user.setPassword("$2a$12$hashed");
        user.setStatus(1);

        when(userMapper.selectByUsername("testuser")).thenReturn(user);

        SaSession mockSession = mock(SaSession.class);

        try (MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class);
             MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {

            bc.when(() -> BCryptUtil.verifyPassword("Abc123", "$2a$12$hashed")).thenReturn(true);
            stp.when(() -> StpUtil.login(100L)).then(invocation -> null);
            stp.when(StpUtil::getTokenSession).thenReturn(mockSession);
            stp.when(StpUtil::getTokenValue).thenReturn("login-token");

            LoginResult result = userAuthService.login("testuser", "Abc123");

            assertNotNull(result);
            assertEquals("login-token", result.getToken());
            assertEquals("USER", result.getAccountType());
            assertEquals(100L, ((User) result.getAccount()).getId());
        }
    }

    @Test
    @DisplayName("login 用户名不存在时应抛出异常")
    void login_userNotFound_shouldThrow() {
        when(userMapper.selectByUsername("nobody")).thenReturn(null);

        AuthException ex = assertThrows(AuthException.class, () -> userAuthService.login("nobody", "Abc123"));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("login 密码错误时应抛出异常")
    void login_wrongPassword_shouldThrow() {
        User user = new User();
        user.setId(100L);
        user.setUsername("testuser");
        user.setPassword("$2a$12$hashed");
        user.setStatus(1);

        when(userMapper.selectByUsername("testuser")).thenReturn(user);

        try (MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class)) {
            bc.when(() -> BCryptUtil.verifyPassword("wrong", "$2a$12$hashed")).thenReturn(false);

            AuthException ex = assertThrows(AuthException.class, () -> userAuthService.login("testuser", "wrong"));
            assertEquals("用户名或密码错误", ex.getMessage());
        }
    }

    @Test
    @DisplayName("login 账号已禁用时应抛出异常")
    void login_accountDisabled_shouldThrow() {
        User user = new User();
        user.setId(100L);
        user.setUsername("testuser");
        user.setPassword("$2a$12$hashed");
        user.setStatus(0);

        when(userMapper.selectByUsername("testuser")).thenReturn(user);

        try (MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class)) {
            bc.when(() -> BCryptUtil.verifyPassword("Abc123", "$2a$12$hashed")).thenReturn(true);

            AuthException ex = assertThrows(AuthException.class, () -> userAuthService.login("testuser", "Abc123"));
            assertEquals("账号已被禁用", ex.getMessage());
        }
    }

    // ────────────────────────────── logout ──────────────────────────────

    @Test
    @DisplayName("logout 应调用 StpUtil.logout")
    void logout_shouldCallStpUtil() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            userAuthService.logout();
            stp.verify(StpUtil::logout);
        }
    }

    // ────────────────────────────── existsByUsername ──────────────────────────────

    @Test
    @DisplayName("existsByUsername 用户名存在时应返回 true")
    void existsByUsername_shouldReturnTrue() {
        when(userMapper.countByUsername("testuser")).thenReturn(1);
        assertTrue(userAuthService.existsByUsername("testuser"));
    }

    @Test
    @DisplayName("existsByUsername 用户名不存在时应返回 false")
    void existsByUsername_shouldReturnFalse() {
        when(userMapper.countByUsername("nobody")).thenReturn(0);
        assertFalse(userAuthService.existsByUsername("nobody"));
    }

    // ────────────────────────────── existsByPhone ──────────────────────────────

    @Test
    @DisplayName("existsByPhone 手机号存在时应返回 true")
    void existsByPhone_shouldReturnTrue() {
        when(userMapper.countByPhone("13800138001")).thenReturn(1);
        assertTrue(userAuthService.existsByPhone("13800138001"));
    }

    @Test
    @DisplayName("existsByPhone 手机号不存在时应返回 false")
    void existsByPhone_shouldReturnFalse() {
        when(userMapper.countByPhone("13800138000")).thenReturn(0);
        assertFalse(userAuthService.existsByPhone("13800138000"));
    }

    @Test
    @DisplayName("existsByPhone 手机号为 null 或空时应返回 false")
    void existsByPhone_nullOrEmpty_shouldReturnFalse() {
        assertFalse(userAuthService.existsByPhone(null));
        assertFalse(userAuthService.existsByPhone(""));
        verify(userMapper, never()).countByPhone(anyString());
    }

    // ────────────────────────────── getUserById ──────────────────────────────

    @Test
    @DisplayName("getUserById 应返回用户")
    void getUserById_shouldReturnUser() {
        User expected = new User();
        expected.setId(100L);
        expected.setUsername("testuser");

        when(userMapper.selectById(100L)).thenReturn(expected);

        User result = userAuthService.getUserById(100L);
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("getUserById 用户不存在时应返回 null")
    void getUserById_shouldReturnNull() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertNull(userAuthService.getUserById(999L));
    }

    // ────────────────────────────── updateProfile ──────────────────────────────

    @Test
    @DisplayName("updateProfile 应成功更新用户信息")
    void updateProfile_shouldSucceed() {
        User user = new User();
        user.setId(100L);
        user.setUsername("testuser");
        user.setPhone("13800138000");
        user.setEmail("old@test.com");
        user.setInfoId(1);
        user.setStatus(1);

        UserInfo existingInfo = new UserInfo();
        existingInfo.setId(1);
        existingInfo.setNickname("旧昵称");
        existingInfo.setAvatar("old.jpg");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("新昵称");
        request.setAvatar("new.jpg");
        request.setPhone("13800138002");
        request.setEmail("new@test.com");

        when(userMapper.selectById(100L)).thenReturn(user);
        when(userMapper.selectByPhone("13800138002")).thenReturn(null);
        when(userInfoService.getUserInfoById(1)).thenReturn(existingInfo);

        userAuthService.updateProfile(100L, request);

        assertEquals("新昵称", existingInfo.getNickname());
        assertEquals("new.jpg", existingInfo.getAvatar());
        verify(userInfoService).updateUserInfo(existingInfo);

        assertEquals("13800138002", user.getPhone());
        assertEquals("new@test.com", user.getEmail());
        verify(userMapper).update(user);
    }

    @Test
    @DisplayName("updateProfile 用户不存在时应抛出异常")
    void updateProfile_userNotFound_shouldThrow() {
        when(userMapper.selectById(999L)).thenReturn(null);

        AuthException ex = assertThrows(AuthException.class,
                () -> userAuthService.updateProfile(999L, new UpdateProfileRequest()));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    @DisplayName("updateProfile 手机号被其他用户使用时应抛出异常")
    void updateProfile_phoneTakenByOther_shouldThrow() {
        User user = new User();
        user.setId(100L);
        user.setUsername("testuser");
        user.setPhone("13800138000");

        User other = new User();
        other.setId(200L);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("13800138002");

        when(userMapper.selectById(100L)).thenReturn(user);
        when(userMapper.selectByPhone("13800138002")).thenReturn(other);

        AuthException ex = assertThrows(AuthException.class,
                () -> userAuthService.updateProfile(100L, request));
        assertEquals("手机号已被注册", ex.getMessage());
        verify(userMapper, never()).update(any());
    }

    @Test
    @DisplayName("updateProfile 用户使用自己的手机号应允许")
    void updateProfile_ownPhone_shouldAllow() {
        User user = new User();
        user.setId(100L);
        user.setUsername("testuser");
        user.setPhone("13800138000");
        user.setStatus(1);

        User samePhoneUser = new User();
        samePhoneUser.setId(100L);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("13800138000");

        when(userMapper.selectById(100L)).thenReturn(user);
        when(userMapper.selectByPhone("13800138000")).thenReturn(samePhoneUser);

        userAuthService.updateProfile(100L, request);

        verify(userMapper).update(user);
    }

    @Test
    @DisplayName("updateProfile 用户无 infoId 时应创建新的 UserInfo")
    void updateProfile_noInfoId_shouldCreateUserInfo() {
        User user = new User();
        user.setId(100L);
        user.setUsername("testuser");
        user.setPhone("13800138000");
        user.setInfoId(null);
        user.setStatus(1);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("新昵称");

        UserInfo createdInfo = new UserInfo();
        createdInfo.setId(1);

        when(userMapper.selectById(100L)).thenReturn(user);
        when(userInfoService.createUserInfo(any())).thenReturn(1);

        userAuthService.updateProfile(100L, request);

        verify(userInfoService).createUserInfo(any(UserInfo.class));
        assertEquals(1, user.getInfoId().intValue());
        verify(userMapper).update(user);
    }
}
