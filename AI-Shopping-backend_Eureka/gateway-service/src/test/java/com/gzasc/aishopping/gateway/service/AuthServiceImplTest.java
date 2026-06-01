package com.gzasc.aishopping.gateway.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gzasc.aishopping.gateway.config.AuthWhitelistProperties;
import com.gzasc.aishopping.gateway.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.List;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private AuthWhitelistProperties whitelistProperties;

    @Mock
    private ServerHttpRequest request;

    private AuthServiceImpl authService;

    private final List<String> whitelistPaths = List.of(
            "/api/user/auth/login",
            "/api/user/auth/register",
            "/api/user/auth/check-username",
            "/api/user/auth/check-phone",
            "/api/seller/auth/login",
            "/api/seller/auth/register",
            "/api/seller/auth/check-username",
            "/api/seller/auth/check-phone",
            "/api/seller/shop/register"
    );

    @BeforeEach
    void setUp() {
        when(whitelistProperties.getPaths()).thenReturn(whitelistPaths);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        authService = new AuthServiceImpl(stringRedisTemplate, whitelistProperties);
    }

    @Test
    @DisplayName("GW-RL-001: USER角色访问用户端API成功")
    void hasPermission_userAccessUserApi_returnsTrue() {
        assertTrue(authService.hasPermission("USER", "/api/user/product/all", request));
    }

    @Test
    @DisplayName("GW-RL-002: USER角色访问商家端API被拒")
    void hasPermission_userAccessSellerApi_returnsFalse() {
        assertFalse(authService.hasPermission("USER:u001", "/api/seller/product/all", request));
    }

    @Test
    @DisplayName("GW-RL-003: MERCHANT角色访问商家端API成功")
    void hasPermission_merchantAccessSellerApi_returnsTrue() {
        assertTrue(authService.hasPermission("MERCHANT", "/api/seller/product/list", request));
    }

    @Test
    @DisplayName("GW-RL-004: MERCHANT角色访问用户端API被拒")
    void hasPermission_merchantAccessUserApi_returnsFalse() {
        assertFalse(authService.hasPermission("MERCHANT:m001", "/api/user/order/list", request));
    }

    @Test
    @DisplayName("GW-RL-005: 店长访问店铺管理API成功")
    void hasPermission_shopOwnerAccessManageApi_returnsTrue() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Merchant-Role", "1");
        headers.add("X-Shop-Id", "100");
        when(request.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));

        assertTrue(authService.hasPermission("MERCHANT", "/api/seller/shop/manage/update", request));
    }

    @Test
    @DisplayName("GW-RL-006: 非店长角色访问管理API被拒")
    void hasPermission_staffAccessManageApi_returnsFalse() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Merchant-Role", "2");
        when(request.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));

        assertFalse(authService.hasPermission("MERCHANT:m001", "/api/seller/shop/manage/update", request));
    }

    @Test
    @DisplayName("GW-RL-007: 无X-Merchant-Role Header访问管理API被拒")
    void hasPermission_noMerchantRoleHeader_returnsFalse() {
        assertFalse(authService.hasPermission("MERCHANT:m001", "/api/seller/shop/manage/update", request));
    }

    @Test
    @DisplayName("GW-RL-008: 无X-Shop-Id Header访问管理API被拒")
    void hasPermission_noShopIdHeader_returnsFalse() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Merchant-Role", "1");
        when(request.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));

        assertFalse(authService.hasPermission("MERCHANT:m001", "/api/seller/shop/manage/update", request));
    }

    @Test
    @DisplayName("GW-RL-009: 店员访问店铺查询API成功")
    void hasPermission_staffAccessQueryApi_returnsTrue() {
        assertTrue(authService.hasPermission("MERCHANT", "/api/seller/shop/query/list", request));
    }

    @Test
    @DisplayName("GW-RL-010: 店员访问店铺发货API成功（/manage/**/ship 排除在店长API之外）")
    void hasPermission_staffAccessShipApi_returnsTrue() {
        assertTrue(authService.hasPermission("MERCHANT", "/api/seller/shop/manage/100/ship", request));
    }

    @Test
    @DisplayName("GW-RL-011: 店长访问商品编辑API")
    void hasPermission_shopOwnerAccessProductEdit_returnsTrue() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Merchant-Role", "1");
        headers.add("X-Shop-Id", "100");
        when(request.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));

        assertTrue(authService.hasPermission("MERCHANT", "/api/seller/shop/100/products/200", request));
    }

    @Test
    @DisplayName("GW-RL-012: 店员访问员工注册API被拒")
    void hasPermission_staffAccessEmployeeRegister_returnsFalse() {
        assertFalse(authService.hasPermission("MERCHANT:m001", "/api/seller/shop/100/employees/register", request));
    }

    @Test
    @DisplayName("GW-RL-013: 店员访问员工管理API被拒")
    void hasPermission_staffAccessEmployeeManage_returnsFalse() {
        assertFalse(authService.hasPermission("MERCHANT:m001", "/api/seller/shop/100/employees/50", request));
    }

    @Test
    @DisplayName("GW-RL-014: 非标准前缀路径直接放行")
    void hasPermission_nonStandardPath_returnsTrue() {
        assertTrue(authService.hasPermission("ANY:001", "/actuator/health", request));
    }

    @Test
    @DisplayName("GW-RL-015: 角色前缀不匹配但路径允许")
    void hasPermission_unknownRoleOnNonApiPath_returnsTrue() {
        assertTrue(authService.hasPermission("ANONYMOUS:xxx", "/actuator/info", request));
    }

    @Test
    @DisplayName("extractRole: USER前缀返回USER")
    void extractRole_userPrefix_returnsUSER() {
        assertEquals("USER", authService.extractRole("USER:u001"));
    }

    @Test
    @DisplayName("extractRole: MERCHANT前缀返回MERCHANT")
    void extractRole_merchantPrefix_returnsMERCHANT() {
        assertEquals("MERCHANT", authService.extractRole("MERCHANT:m001"));
    }

    @Test
    @DisplayName("extractRole: 未知前缀返回UNKNOWN")
    void extractRole_unknownPrefix_returnsUNKNOWN() {
        assertEquals("UNKNOWN", authService.extractRole("ADMIN:admin"));
    }

    @Test
    @DisplayName("GW-WL-001: 用户登录白名单放行")
    void isWhiteList_userLogin_returnsTrue() {
        assertTrue(authService.isWhiteList("/api/user/auth/login"));
    }

    @Test
    @DisplayName("GW-WL-002: 用户注册白名单放行")
    void isWhiteList_userRegister_returnsTrue() {
        assertTrue(authService.isWhiteList("/api/user/auth/register"));
    }

    @Test
    @DisplayName("GW-WL-003: 用户检查用户名白名单")
    void isWhiteList_userCheckUsername_returnsTrue() {
        assertTrue(authService.isWhiteList("/api/user/auth/check-username"));
    }

    @Test
    @DisplayName("GW-WL-004: 用户检查手机号白名单")
    void isWhiteList_userCheckPhone_returnsTrue() {
        assertTrue(authService.isWhiteList("/api/user/auth/check-phone"));
    }

    @Test
    @DisplayName("GW-WL-005: 商家登录白名单放行")
    void isWhiteList_sellerLogin_returnsTrue() {
        assertTrue(authService.isWhiteList("/api/seller/auth/login"));
    }

    @Test
    @DisplayName("GW-WL-006: 商家注册白名单放行")
    void isWhiteList_sellerRegister_returnsTrue() {
        assertTrue(authService.isWhiteList("/api/seller/auth/register"));
    }

    @Test
    @DisplayName("GW-WL-007: 商家店铺注册白名单放行")
    void isWhiteList_sellerShopRegister_returnsTrue() {
        assertTrue(authService.isWhiteList("/api/seller/shop/register"));
    }

    @Test
    @DisplayName("GW-WL-008: 商家检查用户名白名单")
    void isWhiteList_sellerCheckUsername_returnsTrue() {
        assertTrue(authService.isWhiteList("/api/seller/auth/check-username"));
    }

    @Test
    @DisplayName("GW-WL-009: 商家检查手机号白名单")
    void isWhiteList_sellerCheckPhone_returnsTrue() {
        assertTrue(authService.isWhiteList("/api/seller/auth/check-phone"));
    }

    @Test
    @DisplayName("GW-WL-010: 白名单路径带额外路径段不匹配")
    void isWhiteList_extraPathSegment_returnsFalse() {
        assertFalse(authService.isWhiteList("/api/user/auth/login/extra"));
    }

    @Test
    @DisplayName("GW-WL-011: 白名单路径大小写敏感不匹配")
    void isWhiteList_caseSensitiveMismatch_returnsFalse() {
        assertFalse(authService.isWhiteList("/api/user/auth/Login"));
    }

    @Test
    @DisplayName("isPreFlightRequest: OPTIONS请求返回true")
    void isPreFlightRequest_optionsRequest_returnsTrue() {
        when(request.getMethod()).thenReturn(HttpMethod.OPTIONS);
        assertTrue(authService.isPreFlightRequest(request));
    }

    @Test
    @DisplayName("isPreFlightRequest: GET请求返回false")
    void isPreFlightRequest_getRequest_returnsFalse() {
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        assertFalse(authService.isPreFlightRequest(request));
    }
}
