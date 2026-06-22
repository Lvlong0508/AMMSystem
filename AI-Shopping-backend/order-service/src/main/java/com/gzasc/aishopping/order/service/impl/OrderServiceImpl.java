package com.gzasc.aishopping.order.service.impl;

import com.gzasc.aishopping.common.dto.contact.ContactDTO;
import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.dto.product.StockReserveRequest;
import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
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
import com.gzasc.aishopping.common.dto.order.ShipOrderRequest;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import com.gzasc.aishopping.order.stream.EventPublisher;
import com.gzasc.aishopping.order.stream.FileFallbackDaemon;
import com.gzasc.aishopping.order.stream.OrderEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

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
    private final ShopFeignClient shopFeignClient;
    private final OrderConverter orderConverter;
    private final FileFallbackDaemon fileFallbackDaemon;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public String createOrder(PlaceOrderRequest request, Long userId) {
        CompletableFuture<ProductDTO> productFuture =
                CompletableFuture.supplyAsync(() -> fetchProduct(request.getProductId()));
        CompletableFuture<Void> contactValidateFuture =
                CompletableFuture.runAsync(() -> validateContactOwner(request.getContactId(), userId));

        ProductDTO product;
        try {
            CompletableFuture.allOf(productFuture, contactValidateFuture).join();
            product = productFuture.join();
        } catch (CompletionException e) {
            throw OrderException.unwrap(e);
        }

        BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        int stock = product.getStock() != null ? product.getStock() : 0;

        if (stock < request.getQuantity()) {
            throw new OrderException("商品库存不足，当前库存：" + stock + "（错误代码：O-005）");
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

    private ProductDTO fetchProduct(Long productId) {
        ApiResponse<ProductDTO> resp = productFeignClient.getProductById(productId);
        if (resp == null || resp.getData() == null) {
            throw new OrderException("商品不存在（错误代码：O-003）");
        }
        return resp.getData();
    }

    private void validateContactOwner(Integer contactId, Long userId) {
        ApiResponse<Boolean> resp = contactFeignClient.validateContactOwner(contactId, userId);
        if (resp == null || resp.getData() == null || !resp.getData()) {
            throw new OrderException("联系信息不存在或无权限使用，请重新选择联系信息（错误代码：O-006）");
        }
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
            log.info("已支付订单取消, 异步恢复库存, orderId={}", orderId);
            eventPublisher.publishAfterCommit(OrderEventType.STOCK_RESTORE.name(), orderId, null);
            return;
        }

        updated = orderMapper.updateOrderStatusCas(orderId, Order.CANCELLED, Order.PENDING);
        if (updated > 0) {
            log.info("未支付订单取消, 异步释放预占, orderId={}", orderId);
            eventPublisher.publishAfterCommit(OrderEventType.RESERVATION_RELEASE.name(), orderId, null);
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

        eventPublisher.publishAfterCommit(OrderEventType.LOGISTICS_CREATE.name(), orderId,
                Map.of("contactId", String.valueOf(request.getContactId()),
                        "trackingNumber", request.getTrackingNumber()));
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

        eventPublisher.publishAfterCommit(OrderEventType.STOCK_CONFIRM.name(), orderId,
                Map.of("productId", order.getProductId(),
                        "quantity", String.valueOf(order.getQuantity())));
    }

    @Override
    @Transactional
    public void agreeReturnRequest(String shopId, String orderId) {
        Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        int updated = orderMapper.updateOrderStatusCasMulti(
                orderId, Order.RETURN_PENDING, List.of(Order.SHIPPED, Order.DELIVERED));
        if (updated <= 0) {
            throw new OrderException("订单状态变更失败，请重试");
        }
        log.info("退货审核通过, orderId={}", orderId);
    }

    @Override
    @Transactional
    public void submitReturnLogisticsStatus(Long userId, String orderId) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        int updated = orderMapper.updateOrderStatusCas(orderId, Order.RETURNING, Order.RETURN_PENDING);
        if (updated <= 0) {
            throw new OrderException("订单状态变更失败，请重试");
        }
        log.info("退货物流已提交, orderId={}", orderId);
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

        eventPublisher.publishAfterCommit(OrderEventType.STOCK_RESTORE.name(), orderId, null);
    }

    @Override
    public List<UserOrderCardDTO> getOrdersByUserId(Long userId) {
        List<Order> orders = orderMapper.selectAbstractOrdersByUserId(userId);
        if (orders.isEmpty()) return List.of();

        Map<String, ProductDTO> productMap = buildProductMap(orders);
        Map<Long, ShopInfoDTO> shopMap = buildShopMap(orders);
        return orderConverter.toUserCardDTOList(orders, productMap, shopMap);
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
    public List<SellerOrderCardDTO> getOrdersByShopId(String shopId) {
        List<Order> orders = orderMapper.selectAbstractOrdersByShopId(shopId);
        if (orders.isEmpty()) return List.of();

        Map<String, ProductDTO> productMap = buildProductMap(orders);
        Map<Integer, ContactDTO> contactMap = buildContactMap(orders);
        return orderConverter.toSellerCardDTOList(orders, productMap, contactMap);
    }

    @Override
    public List<ShipmentOrderCardDTO> getShipmentOrdersByShopId(String shopId) {
        List<Order> orders = orderMapper.selectPaidOrdersByShopId(shopId);
        if (orders.isEmpty()) return List.of();

        Map<String, ProductDTO> productMap = buildProductMap(orders);
        Map<Integer, ContactDTO> contactMap = buildContactMap(orders);
        return orderConverter.toShipmentCardDTOList(orders, productMap, contactMap);
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

        ProductDTO product = null;
        try {
            if (order.getProductId() != null) {
                ApiResponse<ProductDTO> productResp = productFeignClient.getProductById(Long.valueOf(order.getProductId()));
                if (productResp != null) {
                    product = productResp.getData();
                }
            }
        } catch (Exception e) {
            log.warn("获取商品信息失败", e);
        }

        ShopInfoDTO shop = null;
        try {
            if (order.getShopId() != null) {
                ApiResponse<ShopInfoDTO> shopResp = shopFeignClient.getShopInfo(Long.valueOf(order.getShopId()));
                if (shopResp != null) {
                    shop = shopResp.getData();
                }
            }
        } catch (Exception e) {
            log.warn("获取店铺信息失败", e);
        }

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

        orderConverter.enrichDetailDTO(dto, product, shop, contactInfo, logisticsInfo);
        return dto;
    }

    // ==================== 辅助：批量获取商品/店铺/联系人 ====================

    private Map<String, ProductDTO> buildProductMap(List<Order> orders) {
        return orders.stream()
            .map(Order::getProductId)
            .filter(id -> id != null)
            .distinct()
            .map(id -> {
                try {
                    ApiResponse<ProductDTO> resp = productFeignClient.getProductById(Long.valueOf(id));
                    return resp != null ? resp.getData() : null;
                } catch (Exception e) {
                    log.warn("获取商品信息失败, productId={}", id, e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(p -> String.valueOf(p.getId()), p -> p, (a, b) -> a));
    }

    private Map<Long, ShopInfoDTO> buildShopMap(List<Order> orders) {
        return orders.stream()
            .map(Order::getShopId)
            .filter(Objects::nonNull)
            .distinct()
            .map(id -> {
                try {
                    ApiResponse<ShopInfoDTO> resp = shopFeignClient.getShopInfo(Long.valueOf(id));
                    if (resp != null && resp.getData() != null) {
                        return Map.entry(Long.valueOf(id), resp.getData());
                    }
                } catch (Exception e) {
                    log.warn("获取店铺信息失败, shopId={}", id, e);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
    }

    private Map<Integer, ContactDTO> buildContactMap(List<Order> orders) {
        return orders.stream()
            .map(Order::getContactId)
            .filter(Objects::nonNull)
            .distinct()
            .map(id -> {
                try {
                    ApiResponse<ContactDTO> resp = contactFeignClient.getContactById(id);
                    return resp != null ? Map.entry(id, resp.getData()) : null;
                } catch (Exception e) {
                    log.warn("获取联系人信息失败, contactId={}", id, e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
