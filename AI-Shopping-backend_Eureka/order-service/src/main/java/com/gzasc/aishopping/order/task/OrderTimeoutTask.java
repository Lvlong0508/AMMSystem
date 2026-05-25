package com.gzasc.aishopping.order.task;

import com.gzasc.aishopping.order.mapper.OrderMapper;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

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
