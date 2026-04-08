package com.gzasc.aishopping.service;

import com.gzasc.aishopping.model.Order;

import java.util.List;

public interface OrderService {

    /**
     * 创建订单
     * @param order 订单信息
     */
    int createOrder(Order order);

    /**
     * 删除订单
     */
    int deleteOrder(String orderId);

    /**
     * 更新订单信息（仅更新客户信息）
     */
    int updateOrder(Order order);

    /**
     * 根据订单ID查询订单
     */
    Order getOrderById(String orderId);

    /**
     * 查询所有订单
     */
    List<Order> getAllOrders();

    /**
     * 根据客户名称查询订单
     */
    List<Order> getOrdersByCustomerName(String customerName);

    /**
     * 根据订单状态查询订单
     */
    List<Order> getOrdersByStatus(String status);

    /**
     * 更新订单状态
     */
    int updateOrderStatus(String orderId, String status);

    /**
     * 生成订单ID
     */
    String generateOrderId();
}
