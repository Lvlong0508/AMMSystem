package com.gzasc.aishopping.order.service.impl;

import com.gzasc.aishopping.common.dto.contact.ContactDTO;
import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.dto.product.StockDeductRequest;
import com.gzasc.aishopping.common.dto.product.StockReserveRequest;
import com.gzasc.aishopping.common.feign.contact.ContactFeignClient;
import com.gzasc.aishopping.common.feign.logistics.LogisticsFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
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
import com.gzasc.aishopping.order.stream.FileFallbackDaemon;
import com.gzasc.aishopping.order.stream.OrderEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final DeletedOrderMapper deletedOrderMapper;
    private final OrderIdSelector orderIdSelector;
    private final ProductFeignClient productFeignClient;
    private final LogisticsFeignClient logisticsFeignClient;
    private final ContactFeignClient contactFeignClient;
    private final OrderConverter orderConverter;
    private final FileFallbackDaemon fileFallbackDaemon;

    @Override
    @Transactional
    public String createOrder(PlaceOrderRequest request, Long userId) {
        ApiResponse<ProductDTO> productResp = productFeignClient.getProductById(request.getProductId());
        if (productResp == null || productResp.getData() == null) {
            throw new OrderException("商品不存在（错误代码：O-003）");
        }
        ProductDTO product = productResp.getData();

        BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        int stock = product.getStock() != null ? product.getStock() : 0;

        if (stock < request.getQuantity()) {
            throw new OrderException("商品库存不足，当前库存：" + stock + "（错误代码：O-005）");
        }

        ApiResponse<ContactDTO> contactResp = contactFeignClient.getContactById(request.getContactId());
        if (contactResp == null || contactResp.getData() == null) {
            throw new OrderException("联系人不存在，请重新选择联系人（错误代码：O-006）");
        }

        Long shopIdObj = product.getShopId();
        if (shopIdObj == null) {
            throw new OrderException("获取店铺信息失败");
        }
        String shopId = String.valueOf(shopIdObj);

        String orderId = orderIdSelector.generate();
        Order order = Order.buildInitOrder(orderId, userId, shopId, String.valueOf(request.getProductId()),
                request.getQuantity(), price.multiply(BigDecimal.valueOf(request.getQuantity())));
        order.setContactId(request.getContactId());

        int result = orderMapper.insertOrder(order);
        if (result <= 0) {
            throw new OrderException("创建订单失败");
        }

        productFeignClient.reserveStock(new StockReserveRequest(orderId, Long.valueOf(order.getProductId()), request.getQuantity()));

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

        int updated = orderMapper.updateOrderStatusCas(orderId, Order.CANCELLED, Order.PAID);
        if (updated > 0) {
            StockDeductRequest stockReq = new StockDeductRequest(Long.valueOf(order.getProductId()), order.getQuantity());
            productFeignClient.restoreStock(stockReq);
            log.info("已支付订单取消，恢复库存, orderId={}", orderId);
            return;
        }

        updated = orderMapper.updateOrderStatusCas(orderId, Order.CANCELLED, Order.PENDING);
        if (updated > 0) {
            productFeignClient.releaseReservation(orderId);
            log.info("未支付订单取消，释放预占, orderId={}", orderId);
            return;
        }

        log.warn("取消订单失败，状态已变更, orderId={}", orderId);
        throw new OrderException("订单状态已变更，取消失败");
    }

    @Override
    @Transactional
    public void shipOrder(String shopId, String orderId, ShipOrderRequest request) {
        Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限发货");
        }

        int updated = orderMapper.updateOrderStatusCas(orderId, Order.SHIPPED, Order.PAID);
        if (updated <= 0) {
            throw new OrderException("订单状态异常，发货失败");
        }

        log.info("订单发货成功, orderId={}", orderId);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        fileFallbackDaemon.sendOrFallback(
                                OrderEventType.LOGISTICS_CREATE.name(), orderId,
                                Map.of("contactId", String.valueOf(request.getContactId()),
                                        "trackingNumber", request.getTrackingNumber())
                        );
                    }
                }
        );
    }

    @Override
    @Transactional
    public void deliverOrder(Long userId, String orderId) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        int updated = orderMapper.updateOrderStatusCas(orderId, Order.DELIVERED, Order.SHIPPED);
        if (updated <= 0) {
            throw new OrderException("订单确认收货失败");
        }
    }

    @Override
    @Transactional
    public void requestReturn(Long userId, String orderId) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        int updated = orderMapper.updateOrderStatusCas(orderId, Order.RETURN_PENDING, Order.SHIPPED);
        if (updated <= 0) {
            updated = orderMapper.updateOrderStatusCas(orderId, Order.RETURN_PENDING, Order.DELIVERED);
        }
        if (updated <= 0) {
            throw new OrderException("申请退货失败，订单状态不允许退货");
        }
    }

    @Override
    @Transactional
    public void payOrder(Long userId, String orderId) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }

        int updated = orderMapper.updateOrderStatusCas(orderId, Order.PAID, Order.PENDING);
        if (updated <= 0) {
            throw new OrderException("订单状态异常，支付失败");
        }

        log.info("订单支付成功, orderId={}, productId={}, quantity={}",
                orderId, order.getProductId(), order.getQuantity());

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        fileFallbackDaemon.sendOrFallback(
                                OrderEventType.STOCK_CONFIRM.name(), orderId,
                                Map.of("productId", order.getProductId(),
                                        "quantity", String.valueOf(order.getQuantity()))
                        );
                    }
                }
        );
    }

    @Override
    @Transactional
    public void approveReturn(String shopId, String orderId) {
        Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        int updated = orderMapper.updateOrderStatusCas(orderId, Order.RETURNING, Order.RETURN_PENDING);
        if (updated <= 0) {
            throw new OrderException("退货审核失败");
        }
    }

    @Override
    @Transactional
    public void confirmReturn(String shopId, String orderId) {
        Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }

        int updated = orderMapper.updateOrderStatusCas(orderId, Order.RETURNED, Order.RETURNING);
        if (updated <= 0) {
             throw new OrderException("退货确认失败");
        }

        log.info("退货确认成功, orderId={}, productId={}, quantity={}",
                orderId, order.getProductId(), order.getQuantity());

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        fileFallbackDaemon.sendOrFallback(
                                OrderEventType.STOCK_RESTORE.name(), orderId, null);
                    }
                }
        );
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

        ContactDTO contactInfo = null;
        try {
            if (order.getContactId() != null) {
                ApiResponse<ContactDTO> contactResp = contactFeignClient.getContactById(order.getContactId());
                if (contactResp != null) {
                    contactInfo = contactResp.getData();
                }
            }
        } catch (Exception e) {
            log.warn("获取联系人信息失败", e);
        }

        Map<String, Object> logisticsInfo = null;
        try {
            ApiResponse<Map<String, Object>> response =
                    logisticsFeignClient.getLatestLogistics(order.getOrderId(), "DELIVERY");
            if (response != null) {
                logisticsInfo = Map.of("data", response.getData());
            }
        } catch (Exception e) {
            log.warn("获取物流信息失败", e);
        }

        orderConverter.enrichDetailDTO(dto, contactInfo, logisticsInfo);
        return dto;
    }
}
