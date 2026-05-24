package com.gzasc.aishopping.order.service;

import com.gzasc.aishopping.order.dto.OrderAbstractSellerDTO;
import com.gzasc.aishopping.order.dto.OrderAbstractUserDTO;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.PlaceOrderRequest;
import com.gzasc.aishopping.order.dto.ShipOrderRequest;

import java.util.List;

public interface OrderService {
    // 用户端操作
    String createOrder(PlaceOrderRequest request, Long userId);
    void cancelOrder(Long userId, String orderId);
    void deleteOrder(Long userId, String orderId);
    void payOrder(Long userId, String orderId);
    void deliverOrder(Long userId, String orderId);
    void requestReturn(Long userId, String orderId);

    // 商家端操作
    void shipOrder(String orderId, ShipOrderRequest request);
    void approveReturn(String shopId, String orderId);
    void confirmReturn(String shopId, String orderId);

    // 查询
    List<OrderAbstractUserDTO> getOrdersByUserId(Long userId);
    OrderDetailDTO getOrderDetailByUser(Long userId, String orderId);
    List<OrderAbstractSellerDTO> getOrdersByShopId(String shopId);
    OrderDetailDTO getOrderDetailByShop(String shopId, String orderId);
}
