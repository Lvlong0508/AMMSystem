package com.gzasc.aishopping.order.service;

import com.gzasc.aishopping.order.model.Order;

import java.util.List;

public interface OrderService {
    int createOrder(Order order);
    int deleteOrder(String orderId);
    Order getOrderById(String orderId);
    List<Order> getAllOrders();
    List<Order> getOrdersByStatus(String status);
    int updateOrderStatus(String orderId, String status);
    String generateOrderId();
    List<Order> getOrdersByIds(List<String> orderIds);
    String getShopIdByProductId(String productId);
    int createUserOrder(Integer userId, String orderId);
    List<Order> getOrdersByUserId(Integer userId);
    Order getOrderByUserId(Integer userId, String orderId);
    int deleteUserOrder(String orderId);
}
