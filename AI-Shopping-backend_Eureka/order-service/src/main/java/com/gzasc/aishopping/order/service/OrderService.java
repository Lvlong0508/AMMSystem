package com.gzasc.aishopping.order.service;

import com.gzasc.aishopping.order.model.Order;

import java.util.List;

public interface OrderService {
    int createOrder(Order order);
    int deleteOrder(String orderId);
    int updateOrder(Order order);
    Order getOrderById(String orderId);
    List<Order> getAllOrders();
    List<Order> getOrdersByStatus(String status);
    int updateOrderStatus(String orderId, String status);
    String generateOrderId();
    List<Order> getOrdersByIds(List<String> orderIds);
    String getShopIdByProductId(String productId);
}
