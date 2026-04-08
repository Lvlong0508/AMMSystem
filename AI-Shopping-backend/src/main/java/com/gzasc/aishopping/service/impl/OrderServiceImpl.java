package com.gzasc.aishopping.service.impl;

import com.gzasc.aishopping.mapper.OrderMapper;
import com.gzasc.aishopping.model.Order;
import com.gzasc.aishopping.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public int createOrder(Order order) {

        System.out.println(new Date()+":run createOrder");
        try {
            return orderMapper.insertOrder(order);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public int deleteOrder(String orderId) {
        System.out.println(new Date()+": run deleteOrder");
        return orderMapper.deleteOrderById(orderId);
    }

    @Override
    public int updateOrder(Order order) {
        System.out.println(new Date()+": run updateOrder");
        return orderMapper.updateOrder(order);
    }

    @Override
    public Order getOrderById(String orderId) {
        System.out.println(new Date()+": run getOrderById");
        return orderMapper.selectOrderById(orderId);
    }

    @Override
    public List<Order> getAllOrders() {
        System.out.println(new Date()+": run getAllOrders");
        return orderMapper.selectAllOrders();
    }

    @Override
    public List<Order> getOrdersByCustomerName(String customerName) {
        System.out.println(new Date()+": run getOrdersByCustomerName");
        return orderMapper.selectOrdersByCustomerName(customerName);
    }

    @Override
    public List<Order> getOrdersByStatus(String status) {
        System.out.println(new Date()+": run getOrdersByStatus");
        return orderMapper.selectOrdersByStatus(status);
    }

    @Override
    public int updateOrderStatus(String orderId, String status) {
        System.out.println(new Date()+": run updateOrderStatus");
        return orderMapper.updateOrderStatus(orderId, status);
    }

    @Override
    public String generateOrderId() {
        // 1. 获取当前日期
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 2. 定义 Redis 键名，每天一个独立的 Key (例如: order:seq:20260401)
        String key = "order:seq:" + currentDate;

        // 3. 利用 Redis 的原子自增命令
        // 如果 Key 不存在，Redis 会自动创建并从 1 开始
        Long sequence = redisTemplate.opsForValue().increment(key);

        // 4. 设置过期时间（可选）：24小时后过期，节省 Redis 内存
        if (sequence != null && sequence == 1) {
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        }

        // 5. 格式化序号为 5 位
        String seqStr = String.format("%05d", sequence);

        // 6. 生成 5 位随机大写字母
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