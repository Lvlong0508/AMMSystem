package com.gzasc.aishopping.shop.service.impl;

import com.gzasc.aishopping.shop.mapper.OrderShopMapper;
import com.gzasc.aishopping.shop.model.OrderShop;
import com.gzasc.aishopping.shop.service.OrderShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderShopServiceImpl implements OrderShopService {

    private final OrderShopMapper orderShopMapper;

    @Override
    public OrderShop selectById(String id) {
        return orderShopMapper.selectById(id);
    }

    @Override
    public List<OrderShop> selectByShopId(String shopId) {
        return orderShopMapper.selectByShopId(shopId);
    }

    @Override
    public List<OrderShop> selectByOrderId(String orderId) {
        return orderShopMapper.selectByOrderId(orderId);
    }

    @Override
    public int insert(OrderShop orderShop) {
        return orderShopMapper.insert(orderShop);
    }

    @Override
    public int deleteById(String id) {
        return orderShopMapper.deleteById(id);
    }
}