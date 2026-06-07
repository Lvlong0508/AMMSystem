package com.gzasc.aishopping.auth.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.gzasc.aishopping.auth.dto.LoginResult;
import com.gzasc.aishopping.auth.dto.RegisterEmployeeRequest;
import com.gzasc.aishopping.auth.dto.RegisterRequest;
import com.gzasc.aishopping.auth.exception.AuthException;
import com.gzasc.aishopping.auth.mapper.merchant.MerchantMapper;
import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.model.MerchantInfo;
import com.gzasc.aishopping.auth.service.MerchantInfoService;
import com.gzasc.aishopping.auth.util.BCryptUtil;
import com.gzasc.aishopping.common.util.SafeIdGenerator;
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
class MerchantAuthServiceImplTest {

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private MerchantInfoService merchantInfoService;

    @InjectMocks
    private MerchantAuthServiceImpl merchantAuthService;

    @Captor
    private ArgumentCaptor<Merchant> merchantCaptor;

    // ────────────────────────────── register ──────────────────────────────

    @Test
    @DisplayName("register 应成功注册商家并返回 LoginResult")
    void register_shouldSucceed() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("seller1");
        request.setPassword("Seller123");
        request.setNickname("商家一号");
        request.setPhone("13900139001");

        when(merchantMapper.countByUsername("seller1")).thenReturn(0);
        when(merchantMapper.countByPhone("13900139001")).thenReturn(0);

        MerchantInfo merchantInfo = new MerchantInfo();
        merchantInfo.setId(1);
        when(merchantInfoService.createMerchantInfo(any())).thenReturn(1);

        SaSession mockSession = mock(SaSession.class);

        try (MockedStatic<SafeIdGenerator> sf = mockStatic(SafeIdGenerator.class);
             MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class);
             MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {

            sf.when(SafeIdGenerator::nextId).thenReturn(200L);
            bc.when(() -> BCryptUtil.hashPassword("Seller123")).thenReturn("$2a$12$merchantHash");
            stp.when(() -> StpUtil.login(200L)).then(invocation -> null);
            stp.when(StpUtil::getTokenSession).thenReturn(mockSession);
            stp.when(StpUtil::getTokenValue).thenReturn("merchant-token");

            LoginResult result = merchantAuthService.register(request);

            assertNotNull(result);
            assertEquals("merchant-token", result.getToken());
            assertEquals("MERCHANT", result.getAccountType());
            Merchant account = (Merchant) result.getAccount();
            assertEquals(200L, account.getId());

            verify(merchantMapper).insert(merchantCaptor.capture());
            Merchant inserted = merchantCaptor.getValue();
            assertEquals(200L, inserted.getId());
            assertEquals("seller1", inserted.getUsername());
            assertEquals("$2a$12$merchantHash", inserted.getPassword());
            assertEquals("13900139001", inserted.getPhone());
            assertEquals(1, inserted.getInfoId());
            assertEquals(1, inserted.getStatus());

            stp.verify(() -> StpUtil.login(200L));
            stp.verify(StpUtil::getTokenSession);
            stp.verify(StpUtil::getTokenValue);
            verify(mockSession).set("accountType", "MERCHANT");
        }
    }

    @Test
    @DisplayName("register 商家用户名已存在时应抛出异常")
    void register_duplicateUsername_shouldThrow() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setPassword("Seller123");

        when(merchantMapper.countByUsername("existing")).thenReturn(1);

        AuthException ex = assertThrows(AuthException.class, () -> merchantAuthService.register(request));
        assertEquals("商家用户名已存在", ex.getMessage());
        verify(merchantMapper, never()).insert(any());
    }

    @Test
    @DisplayName("register 商家手机号已注册时应抛出异常")
    void register_duplicatePhone_shouldThrow() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("seller1");
        request.setPassword("Seller123");
        request.setPhone("13900139001");

        when(merchantMapper.countByUsername("seller1")).thenReturn(0);
        when(merchantMapper.countByPhone("13900139001")).thenReturn(1);

        AuthException ex = assertThrows(AuthException.class, () -> merchantAuthService.register(request));
        assertEquals("手机号已被注册", ex.getMessage());
        verify(merchantMapper, never()).insert(any());
    }

    // ────────────────────────────── login ──────────────────────────────

    @Test
    @DisplayName("login 应成功登录商家并返回 LoginResult")
    void login_shouldSucceed() {
        Merchant merchant = new Merchant();
        merchant.setId(200L);
        merchant.setUsername("seller1");
        merchant.setPassword("$2a$12$hashed");
        merchant.setStatus(1);

        when(merchantMapper.selectByUsername("seller1")).thenReturn(merchant);

        SaSession mockSession = mock(SaSession.class);

        try (MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class);
             MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {

            bc.when(() -> BCryptUtil.verifyPassword("Seller123", "$2a$12$hashed")).thenReturn(true);
            stp.when(() -> StpUtil.login(200L)).then(invocation -> null);
            stp.when(StpUtil::getTokenSession).thenReturn(mockSession);
            stp.when(StpUtil::getTokenValue).thenReturn("login-token");

            LoginResult result = merchantAuthService.login("seller1", "Seller123");

            assertNotNull(result);
            assertEquals("login-token", result.getToken());
            assertEquals("MERCHANT", result.getAccountType());
            assertEquals(200L, ((Merchant) result.getAccount()).getId());
        }
    }

    @Test
    @DisplayName("login 商家不存在时应抛出异常")
    void login_merchantNotFound_shouldThrow() {
        when(merchantMapper.selectByUsername("nobody")).thenReturn(null);

        AuthException ex = assertThrows(AuthException.class, () -> merchantAuthService.login("nobody", "Seller123"));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("login 商家密码错误时应抛出异常")
    void login_wrongPassword_shouldThrow() {
        Merchant merchant = new Merchant();
        merchant.setId(200L);
        merchant.setUsername("seller1");
        merchant.setPassword("$2a$12$hashed");
        merchant.setStatus(1);

        when(merchantMapper.selectByUsername("seller1")).thenReturn(merchant);

        try (MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class)) {
            bc.when(() -> BCryptUtil.verifyPassword("wrong", "$2a$12$hashed")).thenReturn(false);

            AuthException ex = assertThrows(AuthException.class, () -> merchantAuthService.login("seller1", "wrong"));
            assertEquals("用户名或密码错误", ex.getMessage());
        }
    }

    @Test
    @DisplayName("login 商家账号已禁用时应抛出异常")
    void login_accountDisabled_shouldThrow() {
        Merchant merchant = new Merchant();
        merchant.setId(200L);
        merchant.setUsername("seller1");
        merchant.setPassword("$2a$12$hashed");
        merchant.setStatus(0);

        when(merchantMapper.selectByUsername("seller1")).thenReturn(merchant);

        try (MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class)) {
            bc.when(() -> BCryptUtil.verifyPassword("Seller123", "$2a$12$hashed")).thenReturn(true);

            AuthException ex = assertThrows(AuthException.class, () -> merchantAuthService.login("seller1", "Seller123"));
            assertEquals("账号已被禁用", ex.getMessage());
        }
    }

    // ────────────────────────────── logout ──────────────────────────────

    @Test
    @DisplayName("logout 应调用 StpUtil.logout")
    void logout_shouldCallStpUtil() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            merchantAuthService.logout();
            stp.verify(StpUtil::logout);
        }
    }

    // ────────────────────────────── registerEmployee ──────────────────────────────

    @Test
    @DisplayName("registerEmployee 应成功创建店员并返回 ID")
    void registerEmployee_shouldSucceed() {
        RegisterEmployeeRequest request = new RegisterEmployeeRequest();
        request.setUsername("emp01");
        request.setPassword("Emp123");
        request.setNickname("店员A");
        request.setPhone("13200132001");

        when(merchantMapper.countByUsername("emp01")).thenReturn(0);
        when(merchantMapper.countByPhone("13200132001")).thenReturn(0);

        MerchantInfo merchantInfo = new MerchantInfo();
        merchantInfo.setId(2);
        when(merchantInfoService.createMerchantInfo(any())).thenReturn(2);

        try (MockedStatic<SafeIdGenerator> sf = mockStatic(SafeIdGenerator.class);
             MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class)) {

            sf.when(SafeIdGenerator::nextId).thenReturn(300L);
            bc.when(() -> BCryptUtil.hashPassword("Emp123")).thenReturn("$2a$12$empHash");

            Long result = merchantAuthService.registerEmployee(request);

            assertEquals(300L, result);

            verify(merchantMapper).insert(merchantCaptor.capture());
            Merchant inserted = merchantCaptor.getValue();
            assertEquals(300L, inserted.getId());
            assertEquals("emp01", inserted.getUsername());
            assertEquals("$2a$12$empHash", inserted.getPassword());
            assertEquals("13200132001", inserted.getPhone());
            assertEquals(2, inserted.getInfoId());
            assertEquals(1, inserted.getStatus());
        }
    }

    @Test
    @DisplayName("registerEmployee 无密码时应使用默认密码 123456")
    void registerEmployee_defaultPassword() {
        RegisterEmployeeRequest request = new RegisterEmployeeRequest();
        request.setUsername("emp02");
        request.setNickname("店员B");
        request.setPhone("13200132002");

        when(merchantMapper.countByUsername("emp02")).thenReturn(0);
        when(merchantMapper.countByPhone("13200132002")).thenReturn(0);

        MerchantInfo info = new MerchantInfo();
        info.setId(3);
        when(merchantInfoService.createMerchantInfo(any())).thenReturn(3);

        try (MockedStatic<SafeIdGenerator> sf = mockStatic(SafeIdGenerator.class);
             MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class)) {

            sf.when(SafeIdGenerator::nextId).thenReturn(301L);
            bc.when(() -> BCryptUtil.hashPassword("123456")).thenReturn("$2a$12$defaultHash");

            merchantAuthService.registerEmployee(request);

            verify(merchantMapper).insert(merchantCaptor.capture());
            assertEquals("$2a$12$defaultHash", merchantCaptor.getValue().getPassword());
        }
    }

    @Test
    @DisplayName("registerEmployee 无昵称时应跳过商家信息创建")
    void registerEmployee_withoutNickname() {
        RegisterEmployeeRequest request = new RegisterEmployeeRequest();
        request.setUsername("emp03");
        request.setPassword("Emp123");

        when(merchantMapper.countByUsername("emp03")).thenReturn(0);

        try (MockedStatic<SafeIdGenerator> sf = mockStatic(SafeIdGenerator.class);
             MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class)) {

            sf.when(SafeIdGenerator::nextId).thenReturn(302L);
            bc.when(() -> BCryptUtil.hashPassword("Emp123")).thenReturn("$2a$12$hash");

            merchantAuthService.registerEmployee(request);

            verify(merchantInfoService, never()).createMerchantInfo(any());
            verify(merchantMapper).insert(merchantCaptor.capture());
            assertNull(merchantCaptor.getValue().getInfoId());
        }
    }

    @Test
    @DisplayName("registerEmployee 用户名已存在时应抛出异常")
    void registerEmployee_duplicateUsername_shouldThrow() {
        RegisterEmployeeRequest request = new RegisterEmployeeRequest();
        request.setUsername("existing");
        request.setPassword("Emp123");

        when(merchantMapper.countByUsername("existing")).thenReturn(1);

        AuthException ex = assertThrows(AuthException.class, () -> merchantAuthService.registerEmployee(request));
        assertEquals("用户名已存在", ex.getMessage());
        verify(merchantMapper, never()).insert(any());
    }

    @Test
    @DisplayName("registerEmployee 手机号已注册时应抛出异常")
    void registerEmployee_duplicatePhone_shouldThrow() {
        RegisterEmployeeRequest request = new RegisterEmployeeRequest();
        request.setUsername("emp01");
        request.setPassword("Emp123");
        request.setPhone("13200132001");

        when(merchantMapper.countByUsername("emp01")).thenReturn(0);
        when(merchantMapper.countByPhone("13200132001")).thenReturn(1);

        AuthException ex = assertThrows(AuthException.class, () -> merchantAuthService.registerEmployee(request));
        assertEquals("手机号已被注册", ex.getMessage());
        verify(merchantMapper, never()).insert(any());
    }

    // ────────────────────────────── existsByUsername ──────────────────────────────

    @Test
    @DisplayName("existsByUsername 用户名存在时应返回 true")
    void existsByUsername_shouldReturnTrue() {
        when(merchantMapper.countByUsername("seller1")).thenReturn(1);
        assertTrue(merchantAuthService.existsByUsername("seller1"));
    }

    @Test
    @DisplayName("existsByUsername 用户名不存在时应返回 false")
    void existsByUsername_shouldReturnFalse() {
        when(merchantMapper.countByUsername("nobody")).thenReturn(0);
        assertFalse(merchantAuthService.existsByUsername("nobody"));
    }

    // ────────────────────────────── existsByPhone ──────────────────────────────

    @Test
    @DisplayName("existsByPhone 手机号存在时应返回 true")
    void existsByPhone_shouldReturnTrue() {
        when(merchantMapper.countByPhone("13900139001")).thenReturn(1);
        assertTrue(merchantAuthService.existsByPhone("13900139001"));
    }

    @Test
    @DisplayName("existsByPhone 手机号不存在时应返回 false")
    void existsByPhone_shouldReturnFalse() {
        when(merchantMapper.countByPhone("13900139000")).thenReturn(0);
        assertFalse(merchantAuthService.existsByPhone("13900139000"));
    }

    @Test
    @DisplayName("existsByPhone 手机号为 null 或空时应返回 false")
    void existsByPhone_nullOrEmpty_shouldReturnFalse() {
        assertFalse(merchantAuthService.existsByPhone(null));
        assertFalse(merchantAuthService.existsByPhone(""));
        verify(merchantMapper, never()).countByPhone(anyString());
    }

    // ────────────────────────────── getMerchantById ──────────────────────────────

    @Test
    @DisplayName("getMerchantById 应返回商家")
    void getMerchantById_shouldReturnMerchant() {
        Merchant expected = new Merchant();
        expected.setId(200L);
        expected.setUsername("seller1");

        when(merchantMapper.selectById(200L)).thenReturn(expected);

        Merchant result = merchantAuthService.getMerchantById(200L);
        assertNotNull(result);
        assertEquals(200L, result.getId());
        assertEquals("seller1", result.getUsername());
    }

    @Test
    @DisplayName("getMerchantById 商家不存在时应返回 null")
    void getMerchantById_shouldReturnNull() {
        when(merchantMapper.selectById(999L)).thenReturn(null);

        assertNull(merchantAuthService.getMerchantById(999L));
    }
}
