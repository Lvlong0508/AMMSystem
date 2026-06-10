package com.gzasc.aishopping.order.service;

import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.PlaceOrderRequest;
import com.gzasc.aishopping.order.dto.SellerOrderCardDTO;
import com.gzasc.aishopping.order.dto.ShipmentOrderCardDTO;
import com.gzasc.aishopping.common.dto.order.ShipOrderRequest;
import com.gzasc.aishopping.order.dto.UserOrderCardDTO;

import java.util.List;

public interface OrderService {
    // 用户端操作
    String createOrder(PlaceOrderRequest request, Long userId);
    void cancelOrder(Long userId, String orderId);
    void deleteOrder(Long userId, String orderId);
    void payOrder(Long userId, String orderId);
    void deliverOrder(Long userId, String orderId);

    // 商家端操作
    void shipOrder(String shopId, String orderId, ShipOrderRequest request);
    void confirmReturn(String shopId, String orderId);

    // 退货相关订单状态变更
    void agreeReturnRequest(String shopId, String orderId);
    void submitReturnLogisticsStatus(Long userId, String orderId);

    // 查询
    List<UserOrderCardDTO> getOrdersByUserId(Long userId);
    OrderDetailDTO getOrderDetailByUser(Long userId, String orderId);
    List<SellerOrderCardDTO> getOrdersByShopId(String shopId);
    List<ShipmentOrderCardDTO> getShipmentOrdersByShopId(String shopId);
    OrderDetailDTO getOrderDetailByShop(String shopId, String orderId);
}
