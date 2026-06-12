package com.gzasc.aishopping.chat.tools;

import com.gzasc.aishopping.chat.exception.AiToolException;
import com.gzasc.aishopping.chat.service.impl.ChatSessionService;
import com.gzasc.aishopping.common.feign.order.OrderFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @Mock
    private ChatSessionService chatSessionService;

    @InjectMocks
    private OrderTools orderTools;

    private static final String SESSION_ID = "507f1f77bcf86cd799439011";

    @Test
    @DisplayName("CH-026 getOrderById - 正常查询")
    void getOrderById_normal() throws Exception {
        Map<String, Object> order = new HashMap<>();
        order.put("orderId", "ORD001");
        order.put("orderStatus", "PAID");
        order.put("totalPrice", 5998.0);

        when(chatSessionService.getSessionUserId(SESSION_ID)).thenReturn(100L);
        when(orderFeignClient.getOrderById("ORD001", 100L)).thenReturn(ApiResponse.success(order));

        Map<String, Object> result = orderTools.getOrderById("ORD001", SESSION_ID);
        assertEquals("ORD001", result.get("orderId"));
        assertEquals("PAID", result.get("orderStatus"));
        assertEquals(5998.0, result.get("totalPrice"));
    }

    @Test
    @DisplayName("CH-027 getOrderById - Feign 返回 null")
    void getOrderById_feignNull() throws Exception {
        when(chatSessionService.getSessionUserId(SESSION_ID)).thenReturn(100L);
        when(orderFeignClient.getOrderById("ORD999", 100L)).thenReturn(null);

        AiToolException ex = assertThrows(AiToolException.class,
                () -> orderTools.getOrderById("ORD999", SESSION_ID));
        assertEquals("订单不存在", ex.getMessage());
    }

    @Test
    @DisplayName("CH-028 getOrderById - Feign 返回错误状态码")
    void getOrderById_errorCode() throws Exception {
        when(chatSessionService.getSessionUserId(SESSION_ID)).thenReturn(100L);
        when(orderFeignClient.getOrderById("ORD001", 100L)).thenReturn(ApiResponse.error(500, "服务异常"));

        AiToolException ex = assertThrows(AiToolException.class,
                () -> orderTools.getOrderById("ORD001", SESSION_ID));
        assertEquals("订单不存在", ex.getMessage());
    }

    @Test
    @DisplayName("CH-031 getAllOrders - 正常查询")
    void getAllOrders_normal() throws Exception {
        Map<String, Object> order1 = new HashMap<>();
        order1.put("orderId", "ORD001");
        order1.put("orderStatus", "PAID");
        Map<String, Object> order2 = new HashMap<>();
        order2.put("orderId", "ORD002");
        order2.put("orderStatus", "SHIPPED");

        when(chatSessionService.getSessionUserId(SESSION_ID)).thenReturn(100L);
        when(orderFeignClient.getAllOrders(100L)).thenReturn(ApiResponse.success(List.of(order1, order2)));

        List<Map<String, Object>> result = orderTools.getAllOrders(SESSION_ID);
        assertEquals(2, result.size());
        assertEquals("ORD001", result.get(0).get("orderId"));
        assertEquals("ORD002", result.get(1).get("orderId"));
    }

    @Test
    @DisplayName("CH-032 getAllOrders - Feign 返回错误状态码")
    void getAllOrders_errorCode() throws Exception {
        when(chatSessionService.getSessionUserId(SESSION_ID)).thenReturn(100L);
        when(orderFeignClient.getAllOrders(100L)).thenReturn(ApiResponse.error(500, "服务异常"));

        List<Map<String, Object>> result = orderTools.getAllOrders(SESSION_ID);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("CH-033 getOrdersByStatus - 正常过滤")
    void getOrdersByStatus_filter() throws Exception {
        Map<String, Object> order1 = new HashMap<>();
        order1.put("orderId", "ORD001");
        order1.put("orderStatus", "PAID");
        Map<String, Object> order2 = new HashMap<>();
        order2.put("orderId", "ORD002");
        order2.put("orderStatus", "PAID");
        Map<String, Object> order3 = new HashMap<>();
        order3.put("orderId", "ORD003");
        order3.put("orderStatus", "SHIPPED");

        when(chatSessionService.getSessionUserId(SESSION_ID)).thenReturn(100L);
        when(orderFeignClient.getAllOrders(100L)).thenReturn(ApiResponse.success(List.of(order1, order2, order3)));

        List<Map<String, Object>> result = orderTools.getOrdersByStatus("PAID", SESSION_ID);
        assertEquals(2, result.size());
        assertEquals("PAID", result.get(0).get("orderStatus"));
        assertEquals("PAID", result.get(1).get("orderStatus"));
    }

    @Test
    @DisplayName("CH-034 getOrdersByStatus - 无匹配状态")
    void getOrdersByStatus_noMatch() throws Exception {
        Map<String, Object> order = new HashMap<>();
        order.put("orderId", "ORD001");
        order.put("orderStatus", "PAID");

        when(chatSessionService.getSessionUserId(SESSION_ID)).thenReturn(100L);
        when(orderFeignClient.getAllOrders(100L)).thenReturn(ApiResponse.success(List.of(order)));

        List<Map<String, Object>> result = orderTools.getOrdersByStatus("CANCELLED", SESSION_ID);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("CH-035 getOrdersByStatus - 无效状态值")
    void getOrdersByStatus_invalidStatus() throws Exception {
        Map<String, Object> order = new HashMap<>();
        order.put("orderId", "ORD001");
        order.put("orderStatus", "PAID");

        when(chatSessionService.getSessionUserId(SESSION_ID)).thenReturn(100L);
        when(orderFeignClient.getAllOrders(100L)).thenReturn(ApiResponse.success(List.of(order)));

        List<Map<String, Object>> result = orderTools.getOrdersByStatus("INVALID_STATUS", SESSION_ID);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("CH-036 getOrdersByStatus - status 为 null")
    void getOrdersByStatus_nullStatus() throws Exception {
        Map<String, Object> order = new HashMap<>();
        order.put("orderId", "ORD001");
        order.put("orderStatus", "PAID");

        when(chatSessionService.getSessionUserId(SESSION_ID)).thenReturn(100L);
        when(orderFeignClient.getAllOrders(100L)).thenReturn(ApiResponse.success(List.of(order)));

        List<Map<String, Object>> result = orderTools.getOrdersByStatus(null, SESSION_ID);
        assertTrue(result.isEmpty());
    }
}
