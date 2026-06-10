package com.gzasc.aishopping.order.task;

import com.gzasc.aishopping.order.mapper.OrderMapper;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "order.task.timeout.enabled", havingValue = "true", matchIfMissing = true)
public class OrderTimeoutTask {



    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @Value("${order.timeout.payment-minutes:30}")
    private int paymentTimeoutMinutes;

    @Scheduled(fixedRate = 60000)
    public void cancelExpiredOrders() {
        List<Order> expired = orderMapper.selectExpiredPendingOrders(paymentTimeoutMinutes);
        for (Order order : expired) {
            try {
                orderService.cancelOrder(order.getUserId(), order.getOrderId());
            } catch (Exception e) {
                log.warn("系统取消订单失败: {}", order.getOrderId(), e);
            }
        }
    }
}
