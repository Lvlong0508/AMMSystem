package com.gzasc.aishopping.order.service.impl;

import com.gzasc.aishopping.order.mapper.DeletedOrderMapper;
import com.gzasc.aishopping.order.mapper.OrderMapper;
import com.gzasc.aishopping.order.model.DeletedOrder;
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
    private final DeletedOrderMapper deletedOrderMapper;
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
        try {
            // 1. 先查询订单信息
            Order order = orderMapper.selectOrderById(orderId);
            if (order == null) {
                System.out.println(new Date() + ": 订单不存在，无需删除: " + orderId);
                return -1;
            }

            // 2. 将订单信息备份到 deleted_orders 表
            DeletedOrder deletedOrder = DeletedOrder.fromOrder(order);
            int backupResult = deletedOrderMapper.insertDeletedOrder(deletedOrder);
            if (backupResult <= 0) {
                System.err.println(new Date() + ": 备份订单到 deleted_orders 失败: " + orderId);
                return -1;
            }
            System.out.println(new Date() + ": 订单已备份到 deleted_orders 表: " + orderId);

            // 3. 从原表删除订单
            int deleteResult = orderMapper.deleteOrderById(orderId);
            if (deleteResult > 0) {
                System.out.println(new Date() + ": 订单删除成功: " + orderId);
                return deleteResult;
            } else {
                System.err.println(new Date() + ": 从 t_order 删除订单失败: " + orderId);
                return -1;
            }
        } catch (Exception e) {
            System.err.println(new Date() + ": 删除订单异常: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
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
