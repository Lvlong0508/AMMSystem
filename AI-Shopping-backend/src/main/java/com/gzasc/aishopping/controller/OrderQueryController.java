package com.gzasc.aishopping.controller;

import com.gzasc.aishopping.model.Order;
import com.gzasc.aishopping.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderQueryController {

    private final OrderService orderService;

    // 根据订单ID查询订单
    @GetMapping("/{orderId}")
    public Map<String, Object> getOrderById(@PathVariable("orderId") String orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order != null) {
                return Map.of("message", "查询成功", "order", order);
            } else {
                return Map.of("message", "查询失败：订单不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "查询订单错误：" + e.getMessage());
        }
    }

    // 查询所有订单
    @GetMapping("/list")
    public Map<String, Object> getAllOrders() {
        try {
            List<Order> orders = orderService.getAllOrders();
            return Map.of("message", "查询成功", "orders", orders, "total", orders.size());
        } catch (Exception e) {
            return Map.of("message", "查询订单错误：" + e.getMessage());
        }
    }

    // 根据客户名称查询订单
    @GetMapping("/customer/{customerName}")
    public Map<String, Object> getOrdersByCustomerName(@PathVariable("customerName") String customerName) {
        try {
            List<Order> orders = orderService.getOrdersByCustomerName(customerName);
            return Map.of("message", "查询成功", "orders", orders, "total", orders.size());
        } catch (Exception e) {
            return Map.of("message", "查询订单错误：" + e.getMessage());
        }
    }

    // 根据订单状态查询订单
    @GetMapping("/status/{status}")
    public Map<String, Object> getOrdersByStatus(@PathVariable("status") String status) {
        try {
            List<Order> orders = orderService.getOrdersByStatus(status);
            return Map.of("message", "查询成功", "orders", orders, "total", orders.size());
        } catch (Exception e) {
            return Map.of("message", "查询订单错误：" + e.getMessage());
        }
    }
}
