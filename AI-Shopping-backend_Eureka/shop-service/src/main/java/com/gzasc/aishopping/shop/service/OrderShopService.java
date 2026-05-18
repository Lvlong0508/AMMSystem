package com.gzasc.aishopping.shop.service;

import com.gzasc.aishopping.shop.model.OrderShop;

import java.util.List;

public interface OrderShopService {
    OrderShop selectById(String id);
    List<OrderShop> selectByShopId(String shopId);
    List<OrderShop> selectByOrderId(String orderId);
    int insert(OrderShop orderShop);
    int deleteById(String id);
}