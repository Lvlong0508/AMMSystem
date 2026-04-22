package com.gzasc.aishopping.gateway.route;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gateway路由配置测试
 */
@SpringBootTest
public class GatewayRouteTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void testRoutesExist() {
        assertNotNull(routeLocator);

        var routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes);
        assertTrue(routes.size() >= 12, "应该配置至少12条路由（6用户+6商家）");

        // 验证关键路由存在
        boolean hasUserAuth = routes.stream()
                .anyMatch(r -> r.getId().equals("user-auth"));
        boolean hasSellerAuth = routes.stream()
                .anyMatch(r -> r.getId().equals("seller-auth"));
        boolean hasUserOrder = routes.stream()
                .anyMatch(r -> r.getId().equals("user-order"));
        boolean hasSellerProduct = routes.stream()
                .anyMatch(r -> r.getId().equals("seller-product"));

        assertTrue(hasUserAuth, "用户认证路由应存在");
        assertTrue(hasSellerAuth, "商家认证路由应存在");
        assertTrue(hasUserOrder, "用户订单路由应存在");
        assertTrue(hasSellerProduct, "商家商品路由应存在");
    }
}
