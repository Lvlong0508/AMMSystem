package com.gzasc.aishopping.order.task;

import com.gzasc.aishopping.order.mapper.OrderMapper;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutTaskTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderService orderService;

    private OrderTimeoutTask task;

    private Order createOrder(String orderId, Long userId, String status) {
        Order o = new Order();
        o.setOrderId(orderId);
        o.setUserId(userId);
        o.setOrderStatus(status);
        o.setOrderDate(new Timestamp(System.currentTimeMillis() - 31 * 60 * 1000));
        return o;
    }

    @BeforeEach
    void setUp() {
        task = new OrderTimeoutTask(orderMapper, orderService);
        ReflectionTestUtils.setField(task, "paymentTimeoutMinutes", 30);
    }

    @Test
    @DisplayName("OR-056 超时取消 - PENDING超过30分钟")
    void cancelExpiredOrders_expired() {
        Order expired = createOrder("ORDER001", 100L, "PENDING");
        when(orderMapper.selectExpiredPendingOrders(30)).thenReturn(List.of(expired));

        task.cancelExpiredOrders();

        verify(orderService).cancelOrder(100L, "ORDER001");
    }

    @Test
    @DisplayName("OR-057 超时取消 - 30分钟内不取消")
    void cancelExpiredOrders_noExpired() {
        when(orderMapper.selectExpiredPendingOrders(30)).thenReturn(List.of());

        task.cancelExpiredOrders();

        verify(orderService, never()).cancelOrder(anyLong(), anyString());
    }

    @Test
    @DisplayName("OR-058 超时取消 - 已支付订单不受影响（select只查PENDING）")
    void cancelExpiredOrders_paidNotAffected() {
        when(orderMapper.selectExpiredPendingOrders(30)).thenReturn(List.of());

        task.cancelExpiredOrders();

        verify(orderMapper).selectExpiredPendingOrders(30);
        verify(orderService, never()).cancelOrder(anyLong(), anyString());
    }

    @Test
    @DisplayName("OR-059 超时取消 - 批量场景")
    void cancelExpiredOrders_batch() {
        Order o1 = createOrder("ORDER001", 100L, "PENDING");
        Order o2 = createOrder("ORDER002", 200L, "PENDING");
        Order o3 = createOrder("ORDER003", 300L, "PENDING");
        when(orderMapper.selectExpiredPendingOrders(30)).thenReturn(List.of(o1, o2, o3));

        task.cancelExpiredOrders();

        verify(orderService).cancelOrder(100L, "ORDER001");
        verify(orderService).cancelOrder(200L, "ORDER002");
        verify(orderService).cancelOrder(300L, "ORDER003");
    }

    @Test
    @DisplayName("OR-059 超时取消 - 个别失败不影响其他")
    void cancelExpiredOrders_partialFailure() {
        Order o1 = createOrder("ORDER001", 100L, "PENDING");
        Order o2 = createOrder("ORDER002", 200L, "PENDING");
        when(orderMapper.selectExpiredPendingOrders(30)).thenReturn(List.of(o1, o2));
        doThrow(new RuntimeException("取消失败")).when(orderService).cancelOrder(100L, "ORDER001");

        task.cancelExpiredOrders();

        verify(orderService).cancelOrder(100L, "ORDER001");
        verify(orderService).cancelOrder(200L, "ORDER002");
    }
}
