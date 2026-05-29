package com.gzasc.aishopping.chat.tools;

import com.gzasc.aishopping.chat.context.UserContext;
import com.gzasc.aishopping.chat.exception.AiToolException;
import com.gzasc.aishopping.common.feign.order.OrderFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderToolsTest {

    @Mock
    private OrderFeignClient orderFeignClient;

    @InjectMocks
    private OrderTools orderTools;

    @Test
    @DisplayName("CH-026 getOrderById - 正常查询")
    void getOrderById_normal() throws Exception {
        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenReturn(100L);

            Map<String, Object> order = new HashMap<>();
            order.put("orderId", "ORD001");
            order.put("orderStatus", "PAID");
            order.put("totalPrice", 5998.0);

            when(orderFeignClient.getOrderById("ORD001", 100L)).thenReturn(ApiResponse.success(order));

            Map<String, Object> result = orderTools.getOrderById("ORD001");
            assertEquals("ORD001", result.get("orderId"));
            assertEquals("PAID", result.get("orderStatus"));
            assertEquals(5998.0, result.get("totalPrice"));
        }
    }

    @Test
    @DisplayName("CH-027 getOrderById - Feign 返回 null")
    void getOrderById_feignNull() throws Exception {
        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenReturn(100L);

            when(orderFeignClient.getOrderById("ORD999", 100L)).thenReturn(null);

            AiToolException ex = assertThrows(AiToolException.class,
                    () -> orderTools.getOrderById("ORD999"));
            assertEquals("订单不存在", ex.getMessage());
        }
    }

    @Test
    @DisplayName("CH-028 getOrderById - Feign 返回错误状态码")
    void getOrderById_errorCode() throws Exception {
        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenReturn(100L);

            when(orderFeignClient.getOrderById("ORD001", 100L)).thenReturn(ApiResponse.error(500, "服务异常"));

            AiToolException ex = assertThrows(AiToolException.class,
                    () -> orderTools.getOrderById("ORD001"));
            assertEquals("订单不存在", ex.getMessage());
        }
    }

    @Test
    @DisplayName("CH-029 getOrderById - UserContext 无 request")
    void getOrderById_noRequestContext() throws Exception {
        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenThrow(new RuntimeException("No request context available"));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> orderTools.getOrderById("ORD001"));
            assertEquals("No request context available", ex.getMessage());
        }
    }

    @Test
    @DisplayName("CH-030 getOrderById - X-User-Id 缺失")
    void getOrderById_missingHeader() throws Exception {
        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenThrow(new RuntimeException("X-User-Id header is missing"));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> orderTools.getOrderById("ORD001"));
            assertEquals("X-User-Id header is missing", ex.getMessage());
        }
    }

    @Test
    @DisplayName("CH-031 getAllOrders - 正常查询")
    void getAllOrders_normal() throws Exception {
        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenReturn(100L);

            Map<String, Object> order1 = new HashMap<>();
            order1.put("orderId", "ORD001");
            order1.put("orderStatus", "PAID");
            Map<String, Object> order2 = new HashMap<>();
            order2.put("orderId", "ORD002");
            order2.put("orderStatus", "SHIPPED");

            when(orderFeignClient.getAllOrders(100L)).thenReturn(ApiResponse.success(List.of(order1, order2)));

            List<Map<String, Object>> result = orderTools.getAllOrders();
            assertEquals(2, result.size());
            assertEquals("ORD001", result.get(0).get("orderId"));
            assertEquals("ORD002", result.get(1).get("orderId"));
        }
    }

    @Test
    @DisplayName("CH-032 getAllOrders - Feign 返回错误状态码")
    void getAllOrders_errorCode() throws Exception {
        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenReturn(100L);

            when(orderFeignClient.getAllOrders(100L)).thenReturn(ApiResponse.error(500, "服务异常"));

            List<Map<String, Object>> result = orderTools.getAllOrders();
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @DisplayName("CH-033 getOrdersByStatus - 正常过滤")
    void getOrdersByStatus_filter() throws Exception {
        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenReturn(100L);

            Map<String, Object> order1 = new HashMap<>();
            order1.put("orderId", "ORD001");
            order1.put("orderStatus", "PAID");
            Map<String, Object> order2 = new HashMap<>();
            order2.put("orderId", "ORD002");
            order2.put("orderStatus", "PAID");
            Map<String, Object> order3 = new HashMap<>();
            order3.put("orderId", "ORD003");
            order3.put("orderStatus", "SHIPPED");

            when(orderFeignClient.getAllOrders(100L)).thenReturn(ApiResponse.success(List.of(order1, order2, order3)));

            List<Map<String, Object>> result = orderTools.getOrdersByStatus("PAID");
            assertEquals(2, result.size());
            assertEquals("PAID", result.get(0).get("orderStatus"));
            assertEquals("PAID", result.get(1).get("orderStatus"));
        }
    }

    @Test
    @DisplayName("CH-034 getOrdersByStatus - 无匹配状态")
    void getOrdersByStatus_noMatch() throws Exception {
        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenReturn(100L);

            Map<String, Object> order = new HashMap<>();
            order.put("orderId", "ORD001");
            order.put("orderStatus", "PAID");

            when(orderFeignClient.getAllOrders(100L)).thenReturn(ApiResponse.success(List.of(order)));

            List<Map<String, Object>> result = orderTools.getOrdersByStatus("CANCELLED");
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @DisplayName("CH-035 getOrdersByStatus - 无效状态值")
    void getOrdersByStatus_invalidStatus() throws Exception {
        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenReturn(100L);

            Map<String, Object> order = new HashMap<>();
            order.put("orderId", "ORD001");
            order.put("orderStatus", "PAID");

            when(orderFeignClient.getAllOrders(100L)).thenReturn(ApiResponse.success(List.of(order)));

            List<Map<String, Object>> result = orderTools.getOrdersByStatus("INVALID_STATUS");
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @DisplayName("CH-036 getOrdersByStatus - status 为 null")
    void getOrdersByStatus_nullStatus() throws Exception {
        try (MockedStatic<UserContext> uc = Mockito.mockStatic(UserContext.class)) {
            uc.when(UserContext::getUserId).thenReturn(100L);

            Map<String, Object> order = new HashMap<>();
            order.put("orderId", "ORD001");
            order.put("orderStatus", "PAID");

            when(orderFeignClient.getAllOrders(100L)).thenReturn(ApiResponse.success(List.of(order)));

            List<Map<String, Object>> result = orderTools.getOrdersByStatus(null);
            assertTrue(result.isEmpty());
        }
    }
}
