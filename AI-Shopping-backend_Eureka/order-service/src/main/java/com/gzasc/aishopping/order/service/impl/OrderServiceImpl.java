package com.gzasc.aishopping.order.service.impl;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.dto.product.StockDeductRequest;
import com.gzasc.aishopping.common.dto.product.StockReserveRequest;
import com.gzasc.aishopping.common.feign.contact.ContactFeignClient;
import com.gzasc.aishopping.common.feign.logistics.LogisticsFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.common.feign.shop.ShopFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.converter.OrderConverter;
import com.gzasc.aishopping.order.dto.*;
import com.gzasc.aishopping.order.exception.OrderException;
import com.gzasc.aishopping.order.id.OrderIdSelector;
import com.gzasc.aishopping.order.mapper.DeletedOrderMapper;
import com.gzasc.aishopping.order.mapper.OrderMapper;
import com.gzasc.aishopping.order.model.DeletedOrder;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final DeletedOrderMapper deletedOrderMapper;
    private final OrderIdSelector orderIdSelector;
    private final ProductFeignClient productFeignClient;
    private final ShopFeignClient shopFeignClient;
    private final LogisticsFeignClient logisticsFeignClient;
    private final ContactFeignClient contactFeignClient;
    private final OrderConverter orderConverter;

    @Override
    @Transactional
    public String createOrder(PlaceOrderRequest request, Long userId) {
        Map<String, Object> productMap = productFeignClient.getProductById(request.getProductId());
        if (productMap == null) {
            throw new OrderException("商品不存在（错误代码：O-003）");
        }

        double price = productMap.get("price") != null
                ? ((Number) productMap.get("price")).doubleValue() : 0.0;
        int stock = productMap.get("stock") != null
                ? ((Number) productMap.get("stock")).intValue() : 0;

        if (stock < request.getQuantity()) {
            throw new OrderException("商品库存不足，当前库存：" + stock + "（错误代码：O-005）");
        }

        Map<String, Object> shopResult = shopFeignClient.getShopIdByProductId(request.getProductId());
        if (shopResult == null || !Boolean.TRUE.equals(shopResult.get("success"))) {
            throw new OrderException("获取店铺信息失败");
        }
        String shopId = String.valueOf(shopResult.get("shopId"));

        String orderId = orderIdSelector.generate();
        Order order = Order.buildInitOrder(orderId, userId, shopId, request.getProductId(),
                request.getQuantity(), price * request.getQuantity());
        order.setContactId(request.getContactId());

        int result = orderMapper.insertOrder(order);
        if (result <= 0) {
            throw new OrderException("创建订单失败");
        }

        productFeignClient.reserveStock(new StockReserveRequest(orderId, order.getProductId(), request.getQuantity()));

        return orderId;
    }

    @Override
    @Transactional
    public void deleteOrder(Long userId, String orderId) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限删除");
        }

        if (!order.canTransition(order.getOrderStatus(), Order.DELETED)) {
            throw new OrderException("当前订单状态不允许删除");
        }

        DeletedOrder deletedOrder = DeletedOrder.fromOrder(order);
        int backupResult = deletedOrderMapper.insertDeletedOrder(deletedOrder);
        if (backupResult <= 0) {
            throw new OrderException("备份订单失败");
        }

        orderMapper.deleteOrderById(orderId);
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, String orderId) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限取消");
        }

        String originalStatus = order.getOrderStatus();
        order.transitionTo(Order.CANCELLED);

        if (Order.PAID.equals(originalStatus)) {
            StockDeductRequest stockReq = new StockDeductRequest(order.getProductId(), order.getQuantity());
            productFeignClient.restoreStock(stockReq);
        } else if (Order.PENDING.equals(originalStatus)) {
            productFeignClient.releaseReservation(orderId);
        }

        orderMapper.updateOrderStatus(orderId, Order.CANCELLED);
    }

    @Override
    @Transactional
    public void shipOrder(String shopId, String orderId, ShipOrderRequest request) {
        Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限发货");
        }

        order.transitionTo(Order.SHIPPED);

        LogisticsRequest logisticsRequest = new LogisticsRequest();
        logisticsRequest.setOrderId(orderId);
        logisticsRequest.setType("DELIVERY");
        logisticsRequest.setContactId(request.getContactId());
        logisticsRequest.setTrackingNumber(request.getTrackingNumber());

        ApiResponse<Map<String, Object>> logisticsResponse =
                logisticsFeignClient.createLogistics(logisticsRequest);
        if (logisticsResponse == null || logisticsResponse.getData() == null) {
            throw new OrderException("创建物流记录失败");
        }

        int result = orderMapper.updateOrderStatus(orderId, Order.SHIPPED);
        if (result <= 0) {
            throw new OrderException("更新订单状态失败");
        }
    }

    @Override
    @Transactional
    public void deliverOrder(Long userId, String orderId) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        order.transitionTo(Order.DELIVERED);
        orderMapper.updateOrderStatus(orderId, Order.DELIVERED);
    }

    @Override
    @Transactional
    public void requestReturn(Long userId, String orderId) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        order.transitionTo(Order.RETURN_PENDING);
        orderMapper.updateOrderStatus(orderId, Order.RETURN_PENDING);
    }

    @Override
    @Transactional
    public void payOrder(Long userId, String orderId) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        if (!order.canTransition(order.getOrderStatus(), Order.PAID)) {
            throw new OrderException("当前订单状态不允许支付");
        }
        Map<String, Object> result = productFeignClient.confirmReservation(orderId);
        Boolean success = (Boolean) result.get("success");
        if (!Boolean.TRUE.equals(success)) {
            throw new OrderException((String) result.get("message"));
        }
        orderMapper.updateOrderStatus(orderId, Order.PAID);
    }

    @Override
    @Transactional
    public void approveReturn(String shopId, String orderId) {
        Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        order.transitionTo(Order.RETURNING);
        orderMapper.updateOrderStatus(orderId, Order.RETURNING);
    }

    @Override
    @Transactional
    public void confirmReturn(String shopId, String orderId) {
        Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        order.transitionTo(Order.RETURNED);
        orderMapper.updateOrderStatus(orderId, Order.RETURNED);
    }

    @Override
    public List<OrderAbstractUserDTO> getOrdersByUserId(Long userId) {
        List<Order> orders = orderMapper.selectAbstractOrdersByUserId(userId);
        return orderConverter.toUserAbstractDTOList(orders);
    }

    @Override
    public OrderDetailDTO getOrderDetailByUser(Long userId, String orderId) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限查看");
        }
        return buildDetailDTO(order);
    }

    @Override
    public List<OrderAbstractSellerDTO> getOrdersByShopId(String shopId) {
        List<Order> orders = orderMapper.selectAbstractOrdersByShopId(shopId);
        return orderConverter.toSellerAbstractDTOList(orders);
    }

    @Override
    public OrderDetailDTO getOrderDetailByShop(String shopId, String orderId) {
        Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限查看");
        }
        return buildDetailDTO(order);
    }

    private OrderDetailDTO buildDetailDTO(Order order) {
        OrderDetailDTO dto = orderConverter.toDetailDTO(order);

        Map<String, Object> contactInfo = null;
        try {
            if (order.getContactId() != null) {
                contactInfo = contactFeignClient.getContactById(order.getContactId());
            }
        } catch (Exception e) {
            System.err.println("获取联系人信息失败: " + e.getMessage());
        }

        Map<String, Object> logisticsInfo = null;
        try {
            ApiResponse<Map<String, Object>> response =
                    logisticsFeignClient.getLatestLogistics(order.getOrderId(), "DELIVERY");
            if (response != null) {
                logisticsInfo = Map.of("data", response.getData());
            }
        } catch (Exception e) {
            System.err.println("获取物流信息失败: " + e.getMessage());
        }

        orderConverter.enrichDetailDTO(dto, contactInfo, logisticsInfo);
        return dto;
    }
}
