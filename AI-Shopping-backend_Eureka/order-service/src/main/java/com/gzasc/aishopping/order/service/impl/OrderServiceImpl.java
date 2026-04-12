package com.gzasc.aishopping.order.service.impl;

import com.gzasc.aishopping.order.mapper.OrderMapper;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public int createOrder(Order order) {
        System.out.println(new Date() + ":run createOrder");
        try {
            return orderMapper.insertOrder(order);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public int deleteOrder(String orderId) {
        System.out.println(new Date() + ": run deleteOrder");
        return orderMapper.deleteOrderById(orderId);
    }

    @Override
    public int updateOrder(Order order) {
        System.out.println(new Date() + ": run updateOrder");
        return orderMapper.updateOrder(order);
    }

    @Override
    public Order getOrderById(String orderId) {
        System.out.println(new Date() + ": run getOrderById");
        return orderMapper.selectOrderById(orderId);
    }

    @Override
    public List<Order> getAllOrders() {
        System.out.println(new Date() + ": run getAllOrders");
        return orderMapper.selectAllOrders();
    }

    @Override
    public List<Order> getOrdersByStatus(String status) {
        System.out.println(new Date() + ": run getOrdersByStatus");
        return orderMapper.selectOrdersByStatus(status);
    }

    @Override
    public int updateOrderStatus(String orderId, String status) {
        System.out.println(new Date() + ": run updateOrderStatus");
        return orderMapper.updateOrderStatus(orderId, status);
    }

    @Override
    public String generateOrderId() {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = "order:seq:" + currentDate;
        Long sequence = redisTemplate.opsForValue().increment(key);
        if (sequence != null && sequence == 1) {
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        }
        String seqStr = String.format("%05d", sequence);
        String randomChars = generateRandomLetters();
        return currentDate + seqStr + randomChars;
    }

    private String generateRandomLetters() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            char c = (char) ('A' + random.nextInt(26));
            sb.append(c);
        }
        return sb.toString();
    }
}
