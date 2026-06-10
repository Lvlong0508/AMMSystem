package com.gzasc.aishopping.auth.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.gzasc.aishopping.auth.dto.LoginResult;
import com.gzasc.aishopping.auth.dto.RegisterRequest;
import com.gzasc.aishopping.auth.exception.AuthException;
import com.gzasc.aishopping.auth.mapper.merchant.MerchantMapper;
import com.gzasc.aishopping.auth.model.Merchant;
import com.gzasc.aishopping.auth.model.MerchantInfo;
import com.gzasc.aishopping.auth.service.MerchantInfoService;
import com.gzasc.aishopping.auth.util.BCryptUtil;
import com.gzasc.aishopping.common.dto.shop.CreateShopForMerchantRequest;
import com.gzasc.aishopping.common.feign.shop.ShopFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantAuthServiceImplTest {

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private MerchantInfoService merchantInfoService;

    @Mock
    private ShopFeignClient shopFeignClient;

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

            // 不包含店铺信息，不应调用 feign
            verify(shopFeignClient, never()).createShopForMerchant(anyString(), any());
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

    @Test
    @DisplayName("register 含店铺信息 - 成功创建店铺")
    void register_withShop_shouldCreateShop() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("seller_with_shop");
        request.setPassword("Pass123");
        request.setPhone("13800138000");
        RegisterRequest.ShopInfo shopInfo = new RegisterRequest.ShopInfo();
        shopInfo.setName("我的小店");
        shopInfo.setDescription("新店开张");
        request.setShop(shopInfo);

        when(merchantMapper.countByUsername("seller_with_shop")).thenReturn(0);
        when(merchantMapper.countByPhone("13800138000")).thenReturn(0);

        SaSession mockSession = mock(SaSession.class);
        ApiResponse<Map<String, Object>> shopResponse = ApiResponse.success(Map.of("id", 10001L));

        try (MockedStatic<SafeIdGenerator> sf = mockStatic(SafeIdGenerator.class);
             MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class);
             MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {

            sf.when(SafeIdGenerator::nextId).thenReturn(300L);
            bc.when(() -> BCryptUtil.hashPassword("Pass123")).thenReturn("$2a$12$hash");
            stp.when(() -> StpUtil.login(300L)).then(invocation -> null);
            stp.when(StpUtil::getTokenSession).thenReturn(mockSession);
            stp.when(StpUtil::getTokenValue).thenReturn("merchant-token-2");

            when(shopFeignClient.createShopForMerchant(eq("auth-service"), any())).thenReturn(shopResponse);

            LoginResult result = merchantAuthService.register(request);

            assertNotNull(result);
            assertEquals("merchant-token-2", result.getToken());
            assertEquals("MERCHANT", result.getAccountType());

            verify(merchantMapper).insert(any(Merchant.class));
            verify(shopFeignClient).createShopForMerchant(eq("auth-service"), argThat(req ->
                    req.getMerchantId().equals(300L) &&
                    "我的小店".equals(req.getName()) &&
                    "新店开张".equals(req.getDescription())
            ));
        }
    }

    @Test
    @DisplayName("register 含店铺信息 - Feign调用失败应回滚")
    void register_withShop_feignFail_shouldRollback() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("seller_fail");
        request.setPassword("Pass123");
        request.setPhone("13800138001");
        RegisterRequest.ShopInfo shopInfo = new RegisterRequest.ShopInfo();
        shopInfo.setName("失败店铺");
        request.setShop(shopInfo);

        when(merchantMapper.countByUsername("seller_fail")).thenReturn(0);
        when(merchantMapper.countByPhone("13800138001")).thenReturn(0);

        try (MockedStatic<SafeIdGenerator> sf = mockStatic(SafeIdGenerator.class);
             MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class)) {

            sf.when(SafeIdGenerator::nextId).thenReturn(400L);
            bc.when(() -> BCryptUtil.hashPassword("Pass123")).thenReturn("$2a$12$hash");

            when(shopFeignClient.createShopForMerchant(eq("auth-service"), any()))
                    .thenThrow(new RuntimeException("Feign timeout"));

            AuthException ex = assertThrows(AuthException.class, () -> merchantAuthService.register(request));
            assertEquals("创建店铺失败，注册已回滚", ex.getMessage());

            // merchant应该不会被插入（事务回滚）
            verify(merchantMapper).insert(any(Merchant.class));
        }
    }

    // ────────────────────────────── login ──────────────────────────────

    @Test
    @DisplayName("login 应成功登录并返回 LoginResult")
    void login_shouldSucceed() {
        Merchant merchant = new Merchant();
        merchant.setId(200L);
        merchant.setUsername("seller1");
        merchant.setPassword("$2a$12$hash");
        merchant.setStatus(1);

        when(merchantMapper.selectByUsername("seller1")).thenReturn(merchant);

        SaSession mockSession = mock(SaSession.class);

        try (MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class);
             MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {

            bc.when(() -> BCryptUtil.verifyPassword("Seller123", "$2a$12$hash")).thenReturn(true);
            stp.when(() -> StpUtil.login(200L)).then(invocation -> null);
            stp.when(StpUtil::getTokenSession).thenReturn(mockSession);
            stp.when(StpUtil::getTokenValue).thenReturn("merchant-token");

            LoginResult result = merchantAuthService.login("seller1", "Seller123");

            assertNotNull(result);
            assertEquals("merchant-token", result.getToken());
            assertEquals("MERCHANT", result.getAccountType());
            Merchant account = (Merchant) result.getAccount();
            assertEquals(200L, account.getId());

            stp.verify(() -> StpUtil.login(200L));
            verify(mockSession).set("accountType", "MERCHANT");
        }
    }

    @Test
    @DisplayName("login 用户名不存在时应抛出异常")
    void login_userNotFound_shouldThrow() {
        when(merchantMapper.selectByUsername("nobody")).thenReturn(null);

        AuthException ex = assertThrows(AuthException.class,
                () -> merchantAuthService.login("nobody", "pass"));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("login 密码错误时应抛出异常")
    void login_wrongPassword_shouldThrow() {
        Merchant merchant = new Merchant();
        merchant.setUsername("seller1");
        merchant.setPassword("$2a$12$hash");
        merchant.setStatus(1);

        when(merchantMapper.selectByUsername("seller1")).thenReturn(merchant);

        try (MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class)) {
            bc.when(() -> BCryptUtil.verifyPassword("wrong", "$2a$12$hash")).thenReturn(false);

            AuthException ex = assertThrows(AuthException.class,
                    () -> merchantAuthService.login("seller1", "wrong"));
            assertEquals("用户名或密码错误", ex.getMessage());
        }
    }

    @Test
    @DisplayName("login 账号被禁用时应抛出异常")
    void login_disabled_shouldThrow() {
        Merchant merchant = new Merchant();
        merchant.setUsername("seller1");
        merchant.setPassword("$2a$12$hash");
        merchant.setStatus(0);

        when(merchantMapper.selectByUsername("seller1")).thenReturn(merchant);

        try (MockedStatic<BCryptUtil> bc = mockStatic(BCryptUtil.class)) {
            bc.when(() -> BCryptUtil.verifyPassword("pass", "$2a$12$hash")).thenReturn(true);

            AuthException ex = assertThrows(AuthException.class,
                    () -> merchantAuthService.login("seller1", "pass"));
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
