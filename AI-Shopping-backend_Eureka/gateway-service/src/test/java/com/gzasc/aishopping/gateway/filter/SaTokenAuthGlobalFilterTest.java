package com.gzasc.aishopping.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SaToken认证全局过滤器测试
 */
class SaTokenAuthGlobalFilterTest {

    private SaTokenAuthGlobalFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SaTokenAuthGlobalFilter();
    }

    @Test
    void testHasPermission_UserAccessUserPath() throws Exception {
        // 用户访问用户端路径 - 应该有权限
        Method hasPermissionMethod = SaTokenAuthGlobalFilter.class.getDeclaredMethod("hasPermission", String.class, String.class);
        hasPermissionMethod.setAccessible(true);

        boolean result = (boolean) hasPermissionMethod.invoke(filter, "USER:123", "/api/user/order/list");
        assertTrue(result, "用户应该能访问用户端路径");
    }

    @Test
    void testHasPermission_UserAccessSellerPath() throws Exception {
        // 用户访问商家端路径 - 应该无权限
        Method hasPermissionMethod = SaTokenAuthGlobalFilter.class.getDeclaredMethod("hasPermission", String.class, String.class);
        hasPermissionMethod.setAccessible(true);

        boolean result = (boolean) hasPermissionMethod.invoke(filter, "USER:123", "/api/seller/product/list");
        assertFalse(result, "用户不应该能访问商家端路径");
    }

    @Test
    void testHasPermission_SellerAccessSellerPath() throws Exception {
        // 商家访问商家端路径 - 应该有权限
        Method hasPermissionMethod = SaTokenAuthGlobalFilter.class.getDeclaredMethod("hasPermission", String.class, String.class);
        hasPermissionMethod.setAccessible(true);

        boolean result = (boolean) hasPermissionMethod.invoke(filter, "MERCHANT:456", "/api/seller/product/list");
        assertTrue(result, "商家应该能访问商家端路径");
    }

    @Test
    void testHasPermission_SellerAccessUserPath() throws Exception {
        // 商家访问用户端路径 - 应该无权限
        Method hasPermissionMethod = SaTokenAuthGlobalFilter.class.getDeclaredMethod("hasPermission", String.class, String.class);
        hasPermissionMethod.setAccessible(true);

        boolean result = (boolean) hasPermissionMethod.invoke(filter, "MERCHANT:456", "/api/user/order/list");
        assertFalse(result, "商家不应该能访问用户端路径");
    }

    @Test
    void testGetOrder() {
        assertEquals(-100, filter.getOrder(), "过滤器顺序应为-100");
    }
}
