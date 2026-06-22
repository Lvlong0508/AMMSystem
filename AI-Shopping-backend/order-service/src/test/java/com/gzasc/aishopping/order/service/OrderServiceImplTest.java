package com.gzasc.aishopping.order.service;

import com.gzasc.aishopping.common.dto.contact.ContactDTO;
import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
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
import com.gzasc.aishopping.order.service.impl.OrderServiceImpl;
import com.gzasc.aishopping.order.stream.EventPublisher;
import com.gzasc.aishopping.order.stream.FileFallbackDaemon;
import com.gzasc.aishopping.common.dto.order.ShipOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.MockedStatic;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private DeletedOrderMapper deletedOrderMapper;
    @Mock
    private OrderIdSelector orderIdSelector;
    @Mock
    private ProductFeignClient productFeignClient;
    @Mock
    private LogisticsFeignClient logisticsFeignClient;
    @Mock
    private ContactFeignClient contactFeignClient;
    @Mock
    private ShopFeignClient shopFeignClient;
    @Mock
    private OrderConverter orderConverter;
    @Mock
    private FileFallbackDaemon fileFallbackDaemon;
    @Mock
    private EventPublisher eventPublisher;

    private OrderServiceImpl orderService;

    @Captor
    private ArgumentCaptor<StockReserveRequest> stockReserveCaptor;
    @Captor
    private ArgumentCaptor<Order> orderCaptor;
    @Captor
    private ArgumentCaptor<DeletedOrder> deletedOrderCaptor;

    private void mockContact(Integer contactId) {
        ContactDTO dto = new ContactDTO();
        dto.setId(contactId);
        dto.setName("测试");
        dto.setPhone("13800138001");
        dto.setAddress("测试地址");
        when(contactFeignClient.getContactById(contactId)).thenReturn(ApiResponse.success(dto));
    }

    private void mockValidContact(Integer contactId, Long userId) {
        when(contactFeignClient.validateContactOwner(contactId, userId)).thenReturn(ApiResponse.success(true));
    }

    private Order createOrder(String orderId, Long userId, String shopId, String status) {
        Order o = new Order();
        o.setOrderId(orderId);
        o.setUserId(userId);
        o.setShopId(shopId);
        o.setProductId("1");
        o.setQuantity(2);
        o.setTotalPrice(BigDecimal.valueOf(100));
        o.setOrderStatus(status);
        o.setOrderDate(new Timestamp(System.currentTimeMillis()));
        o.setContactId(1);
        return o;
    }

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderMapper, deletedOrderMapper, orderIdSelector,
                productFeignClient, logisticsFeignClient, contactFeignClient, shopFeignClient,
                orderConverter, fileFallbackDaemon, eventPublisher);
    }

    // ==================== 下单 (OR-001 ~ OR-005) ====================

    @Test
    @DisplayName("OR-001 正常下单 - 所有参数合法")
    void createOrder_success() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(2);
        request.setContactId(1);

        when(orderIdSelector.generate()).thenReturn("2026052800001ABCDE");
        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        mockValidContact(1, 100L);
        when(orderMapper.insertOrder(any(Order.class))).thenReturn(1);
        when(productFeignClient.reserveStock(any(StockReserveRequest.class)))
                .thenReturn(ApiResponse.success(null));

        String orderId = orderService.createOrder(request, 100L);

        assertEquals("2026052800001ABCDE", orderId);
        verify(orderMapper).insertOrder(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertEquals("PENDING", saved.getOrderStatus());
        assertEquals(100L, saved.getUserId());
        assertEquals("100", saved.getShopId());
        verify(productFeignClient).reserveStock(stockReserveCaptor.capture());
        assertEquals("2026052800001ABCDE", stockReserveCaptor.getValue().getOrderId());
        assertEquals(1L, stockReserveCaptor.getValue().getProductId());
        assertEquals(2, stockReserveCaptor.getValue().getQuantity());
    }

    @Test
    @DisplayName("OR-002 下单 - 商品不存在")
    void createOrder_productNotFound() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(999L);
        request.setQuantity(1);
        request.setContactId(1);

        when(productFeignClient.getProductById(999L)).thenReturn(ApiResponse.success(null));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder(request, 100L));
        assertTrue(ex.getMessage().contains("商品不存在"));
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    @DisplayName("OR-003 下单 - 库存不足")
    void createOrder_insufficientStock() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(5);
        request.setContactId(1);

        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 3, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        mockValidContact(1, 100L);

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder(request, 100L));
        assertTrue(ex.getMessage().contains("库存不足"));
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    @DisplayName("OR-004 下单 - 合法联系人ID允许创建")
    void createOrder_arbitraryContactId() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(1);
        request.setContactId(999);

        when(orderIdSelector.generate()).thenReturn("2026052800002ABCDE");
        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        mockValidContact(999, 100L);
        when(orderMapper.insertOrder(any(Order.class))).thenReturn(1);
        when(productFeignClient.reserveStock(any(StockReserveRequest.class)))
                .thenReturn(ApiResponse.success(null));

        String orderId = orderService.createOrder(request, 100L);
        assertNotNull(orderId);
        verify(orderMapper).insertOrder(orderCaptor.capture());
        assertEquals(999, orderCaptor.getValue().getContactId().intValue());
    }

    @Test
    @DisplayName("OR-005 下单 - 预占库存后下游失败")
    void createOrder_reserveStockFeignFailure() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(1);
        request.setContactId(1);

        when(orderIdSelector.generate()).thenReturn("2026052800003ABCDE");
        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        mockValidContact(1, 100L);
        when(orderMapper.insertOrder(any(Order.class))).thenReturn(1);
        when(productFeignClient.reserveStock(any(StockReserveRequest.class)))
                .thenThrow(new RuntimeException("Feign调用失败"));

        assertThrows(RuntimeException.class,
                () -> orderService.createOrder(request, 100L));
        verify(orderMapper).insertOrder(any(Order.class));
    }

    @Test
    @DisplayName("OR-002 下单 - shopId为null")
    void createOrder_shopIdNull() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(1);
        request.setContactId(1);

        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, null, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        mockValidContact(1, 100L);

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder(request, 100L));
        assertTrue(ex.getMessage().contains("店铺信息"));
    }

    // ==================== 并发场景 (OR-080 ~ OR-083) ====================

    @Test
    @DisplayName("OR-080 并发下单 - 商品/联系人查询都成功")
    void createOrder_concurrent_bothSuccess() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(1);
        request.setContactId(1);

        when(orderIdSelector.generate()).thenReturn("ORDER_CONCURRENT_001");
        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        mockValidContact(1, 100L);
        when(orderMapper.insertOrder(any(Order.class))).thenReturn(1);
        when(productFeignClient.reserveStock(any(StockReserveRequest.class)))
                .thenReturn(ApiResponse.success(null));

        String orderId = orderService.createOrder(request, 100L);

        assertEquals("ORDER_CONCURRENT_001", orderId);
        verify(productFeignClient).getProductById(1L);
        verify(contactFeignClient).validateContactOwner(1, 100L);
    }

    @Test
    @DisplayName("OR-081 并发下单 - 商品查询返回 null 时抛 O-003 或 O-006")
    void createOrder_concurrent_productFailed() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(1);
        request.setContactId(1);

        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(null));
        when(contactFeignClient.validateContactOwner(1, 100L)).thenReturn(ApiResponse.success(false));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder(request, 100L));
        assertTrue(ex.getMessage().contains("商品不存在") || ex.getMessage().contains("联系人不存在"),
                "expected O-003 or O-006 message, got: " + ex.getMessage());
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    @DisplayName("OR-082 并发下单 - 联系人查询失败时对外抛 O-006")
    void createOrder_concurrent_contactFailed() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(1);
        request.setContactId(1);

        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        when(contactFeignClient.validateContactOwner(1, 100L)).thenReturn(ApiResponse.success(false));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder(request, 100L));
        assertTrue(ex.getMessage().contains("联系人不存在或无权限使用"));
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    @DisplayName("OR-083 并发下单 - Feign 抛 RuntimeException 时包装为通用 OrderException")
    void createOrder_concurrent_feignRuntimeException() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(1);
        request.setContactId(1);

        when(productFeignClient.getProductById(1L)).thenThrow(new RuntimeException("网络抖动"));
        when(contactFeignClient.validateContactOwner(1, 100L)).thenThrow(new RuntimeException("网络抖动"));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder(request, 100L));
        assertTrue(ex.getMessage().contains("系统繁忙"));
        verify(orderMapper, never()).insertOrder(any());
    }

    // ==================== 支付 (OR-008 ~ OR-013) ====================

    @Test
    @DisplayName("OR-008 正常支付 - PENDING→PAID")
    void payOrder_success() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PENDING");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "PAID", "PENDING")).thenReturn(1);

        try (MockedStatic<TransactionSynchronizationManager> tm = mockStatic(TransactionSynchronizationManager.class)) {
            tm.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
              .then(invocation -> null);
            orderService.payOrder(100L, "ORDER001");
        }

        verify(orderMapper).updateOrderStatusCas("ORDER001", "PAID", "PENDING");
    }

    @Test
    @DisplayName("OR-009 支付 - 订单不属于当前用户")
    void payOrder_notOwner() {
        when(orderMapper.selectOrderDetailByUser(999L, "ORDER001")).thenReturn(null);

        assertThrows(OrderException.class,
                () -> orderService.payOrder(999L, "ORDER001"));
        verify(orderMapper, never()).updateOrderStatusCas(any(), any(), any());
    }

    @Test
    @DisplayName("OR-010 支付 - 非PENDING状态（CAS返回0）")
    void payOrder_wrongStatus() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "PAID", "PENDING")).thenReturn(0);

        assertThrows(OrderException.class,
                () -> orderService.payOrder(100L, "ORDER001"));
    }

    @Test
    @DisplayName("OR-011 支付 - 订单不存在")
    void payOrder_notFound() {
        when(orderMapper.selectOrderDetailByUser(100L, "NONEXISTENT")).thenReturn(null);

        assertThrows(OrderException.class,
                () -> orderService.payOrder(100L, "NONEXISTENT"));
    }

    // ==================== 取消 (OR-014 ~ OR-018) ====================

    @Test
    @DisplayName("OR-014 取消 PENDING 订单 - 释放预占库存")
    void cancelOrder_pending() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PENDING");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "CANCELLED", "PAID")).thenReturn(0);
        when(orderMapper.updateOrderStatusCas("ORDER001", "CANCELLED", "PENDING")).thenReturn(1);

        orderService.cancelOrder(100L, "ORDER001");

        verify(eventPublisher).publishAfterCommit(eq("RESERVATION_RELEASE"), eq("ORDER001"), isNull());
        verify(productFeignClient, never()).releaseReservation(any());
        verify(productFeignClient, never()).restoreStock(any());
    }

    @Test
    @DisplayName("OR-015 取消 PAID 订单 - 异步恢复库存")
    void cancelOrder_paid() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "CANCELLED", "PAID")).thenReturn(1);

        orderService.cancelOrder(100L, "ORDER001");

        verify(eventPublisher).publishAfterCommit(eq("STOCK_RESTORE"), eq("ORDER001"), isNull());
        verify(productFeignClient, never()).restoreStock(any());
    }

    @Test
    @DisplayName("OR-016 取消 - 不允许的状态（两个CAS均返回0）")
    void cancelOrder_disallowedStatus() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "SHIPPED");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "CANCELLED", "PAID")).thenReturn(0);
        when(orderMapper.updateOrderStatusCas("ORDER001", "CANCELLED", "PENDING")).thenReturn(0);

        assertThrows(OrderException.class,
                () -> orderService.cancelOrder(100L, "ORDER001"));
    }

    @Test
    @DisplayName("OR-017 取消 - 订单不属于当前用户")
    void cancelOrder_notOwner() {
        when(orderMapper.selectOrderDetailByUser(999L, "ORDER001")).thenReturn(null);

        assertThrows(OrderException.class,
                () -> orderService.cancelOrder(999L, "ORDER001"));
    }

    // ==================== 发货 (OR-019 ~ OR-024) ====================

    @Test
    @DisplayName("OR-019 正常发货 - PAID→SHIPPED")
    void shipOrder_success() {
        ShipOrderRequest request = new ShipOrderRequest();
        request.setTrackingNumber("SF1234567890");
        request.setContactId(1);

        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        when(orderMapper.selectOrderDetailByShop("SHOP001", "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "SHIPPED", "PAID")).thenReturn(1);

        try (MockedStatic<TransactionSynchronizationManager> tm = mockStatic(TransactionSynchronizationManager.class)) {
            tm.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
              .then(invocation -> null);
            orderService.shipOrder("SHOP001", "ORDER001", request);
        }

        verify(orderMapper).updateOrderStatusCas("ORDER001", "SHIPPED", "PAID");
    }

    @Test
    @DisplayName("OR-020 发货 - 非PAID状态")
    void shipOrder_wrongStatus() {
        ShipOrderRequest request = new ShipOrderRequest();
        request.setTrackingNumber("SF1234567890");
        request.setContactId(1);

        Order order = createOrder("ORDER001", 100L, "SHOP001", "PENDING");
        when(orderMapper.selectOrderDetailByShop("SHOP001", "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "SHIPPED", "PAID")).thenReturn(0);

        assertThrows(OrderException.class,
                () -> orderService.shipOrder("SHOP001", "ORDER001", request));
    }

    @Test
    @DisplayName("OR-021 发货 - 商家不属于该店铺")
    void shipOrder_wrongShop() {
        ShipOrderRequest request = new ShipOrderRequest();
        request.setTrackingNumber("SF1234567890");
        request.setContactId(1);

        when(orderMapper.selectOrderDetailByShop("WRONG_SHOP", "ORDER001")).thenReturn(null);

        assertThrows(OrderException.class,
                () -> orderService.shipOrder("WRONG_SHOP", "ORDER001", request));
    }

    // ==================== 确认收货 (OR-025 ~ OR-027) ====================

    @Test
    @DisplayName("OR-025 正常确认收货 - SHIPPED→DELIVERED")
    void deliverOrder_success() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "SHIPPED");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "DELIVERED", "SHIPPED")).thenReturn(1);

        orderService.deliverOrder(100L, "ORDER001");

        verify(orderMapper).updateOrderStatusCas("ORDER001", "DELIVERED", "SHIPPED");
    }

    @Test
    @DisplayName("OR-026 确认收货 - 非SHIPPED状态")
    void deliverOrder_wrongStatus() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "DELIVERED", "SHIPPED")).thenReturn(0);

        assertThrows(OrderException.class,
                () -> orderService.deliverOrder(100L, "ORDER001"));
    }

    @Test
    @DisplayName("OR-027 确认收货 - 订单不属于当前用户")
    void deliverOrder_notOwner() {
        when(orderMapper.selectOrderDetailByUser(999L, "ORDER001")).thenReturn(null);

        assertThrows(OrderException.class,
                () -> orderService.deliverOrder(999L, "ORDER001"));
    }

    // ==================== 退货 ====================

    @Test
    @DisplayName("商家同意退货 - SHIPPED/DELIVERED→RETURN_PENDING")
    void agreeReturnRequest_success() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", Order.SHIPPED);
        when(orderMapper.selectOrderDetailByShop("SHOP001", "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCasMulti("ORDER001", Order.RETURN_PENDING,
                List.of(Order.SHIPPED, Order.DELIVERED))).thenReturn(1);

        orderService.agreeReturnRequest("SHOP001", "ORDER001");

        verify(orderMapper).updateOrderStatusCasMulti("ORDER001", Order.RETURN_PENDING,
                List.of(Order.SHIPPED, Order.DELIVERED));
    }

    @Test
    @DisplayName("商家同意退货 - 订单不存在")
    void agreeReturnRequest_orderNotFound() {
        when(orderMapper.selectOrderDetailByShop("SHOP001", "ORDER001")).thenReturn(null);

        assertThrows(OrderException.class,
                () -> orderService.agreeReturnRequest("SHOP001", "ORDER001"));
    }

    @Test
    @DisplayName("提交退货物流状态 - RETURN_PENDING→RETURNING")
    void submitReturnLogisticsStatus_success() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", Order.RETURN_PENDING);
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", Order.RETURNING, Order.RETURN_PENDING)).thenReturn(1);

        orderService.submitReturnLogisticsStatus(100L, "ORDER001");

        verify(orderMapper).updateOrderStatusCas("ORDER001", Order.RETURNING, Order.RETURN_PENDING);
    }

    @Test
    @DisplayName("提交退货物流状态 - 订单不存在")
    void submitReturnLogisticsStatus_orderNotFound() {
        when(orderMapper.selectOrderDetailByUser(999L, "ORDER001")).thenReturn(null);

        assertThrows(OrderException.class,
                () -> orderService.submitReturnLogisticsStatus(999L, "ORDER001"));
    }

    @Test
    @DisplayName("提交退货物流状态 - CAS失败")
    void submitReturnLogisticsStatus_casFailed() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", Order.RETURN_PENDING);
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", Order.RETURNING, Order.RETURN_PENDING)).thenReturn(0);

        assertThrows(OrderException.class,
                () -> orderService.submitReturnLogisticsStatus(100L, "ORDER001"));
    }

    @Test
    @DisplayName("OR-033 确认退货 - RETURNING→RETURNED")
    void confirmReturn_success() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "RETURNING");
        when(orderMapper.selectOrderDetailByShop("SHOP001", "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "RETURNED", "RETURNING")).thenReturn(1);

        try (MockedStatic<TransactionSynchronizationManager> tm = mockStatic(TransactionSynchronizationManager.class)) {
            tm.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
              .then(invocation -> null);
            orderService.confirmReturn("SHOP001", "ORDER001");
        }

        verify(orderMapper).updateOrderStatusCas("ORDER001", "RETURNED", "RETURNING");
    }

    @Test
    @DisplayName("OR-033 确认退货 - CAS失败")
    void confirmReturn_failure() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PENDING");
        when(orderMapper.selectOrderDetailByShop("SHOP001", "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "RETURNED", "RETURNING")).thenReturn(0);

        assertThrows(OrderException.class,
                () -> orderService.confirmReturn("SHOP001", "ORDER001"));
    }

    // ==================== 删除 (OR-036 ~ OR-039) ====================

    @Test
    @DisplayName("OR-036 删除 DELIVERED 订单 - 备份后删除")
    void deleteOrder_delivered() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "DELIVERED");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(deletedOrderMapper.insertDeletedOrder(any(DeletedOrder.class))).thenReturn(1);
        when(orderMapper.deleteOrderById("ORDER001")).thenReturn(1);

        orderService.deleteOrder(100L, "ORDER001");

        verify(deletedOrderMapper).insertDeletedOrder(deletedOrderCaptor.capture());
        assertEquals("ORDER001", deletedOrderCaptor.getValue().getOrderId());
        assertEquals("DELIVERED", deletedOrderCaptor.getValue().getOrderStatus());
        verify(orderMapper).deleteOrderById("ORDER001");
    }

    @Test
    @DisplayName("OR-037 删除 CANCELLED 订单")
    void deleteOrder_cancelled() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "CANCELLED");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(deletedOrderMapper.insertDeletedOrder(any(DeletedOrder.class))).thenReturn(1);
        when(orderMapper.deleteOrderById("ORDER001")).thenReturn(1);

        orderService.deleteOrder(100L, "ORDER001");

        verify(deletedOrderMapper).insertDeletedOrder(any(DeletedOrder.class));
        verify(orderMapper).deleteOrderById("ORDER001");
    }

    @Test
    @DisplayName("OR-038 删除 - 不允许的状态（PAID）")
    void deleteOrder_wrongStatus() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);

        assertThrows(OrderException.class,
                () -> orderService.deleteOrder(100L, "ORDER001"));
        verify(deletedOrderMapper, never()).insertDeletedOrder(any());
        verify(orderMapper, never()).deleteOrderById(any());
    }

    @Test
    @DisplayName("OR-039 删除 - 订单不属于当前用户")
    void deleteOrder_notOwner() {
        when(orderMapper.selectOrderDetailByUser(999L, "ORDER001")).thenReturn(null);

        assertThrows(OrderException.class,
                () -> orderService.deleteOrder(999L, "ORDER001"));
    }

    // ==================== 查询 (OR-046 ~ OR-055) ====================

    @Test
    @DisplayName("OR-046 用户查询订单列表")
    void getOrdersByUserId() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        when(orderMapper.selectAbstractOrdersByUserId(100L)).thenReturn(List.of(order));
        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        when(orderConverter.toUserCardDTOList(anyList(), anyMap(), anyMap())).thenReturn(
                List.of(new UserOrderCardDTO())
        );

        List<UserOrderCardDTO> result = orderService.getOrdersByUserId(100L);
        assertEquals(1, result.size());
        verify(orderMapper).selectAbstractOrdersByUserId(100L);
    }

    @Test
    @DisplayName("OR-055 用户查询空列表")
    void getOrdersByUserId_empty() {
        when(orderMapper.selectAbstractOrdersByUserId(100L)).thenReturn(List.of());

        List<UserOrderCardDTO> result = orderService.getOrdersByUserId(100L);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("OR-047 用户查询订单详情 - 含联系人和物流信息")
    void getOrderDetailByUser_withContactAndLogistics() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);

        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setOrderId("ORDER001");
        when(orderConverter.toDetailDTO(order)).thenReturn(dto);

        ContactDTO contactDTO = new ContactDTO();
        contactDTO.setId(1);
        contactDTO.setName("张三");
        contactDTO.setPhone("13800138001");
        contactDTO.setAddress("北京市");
        when(contactFeignClient.getContactById(1)).thenReturn(
                ApiResponse.success(contactDTO)
        );
        when(logisticsFeignClient.getLatestLogistics("ORDER001", "DELIVERY"))
                .thenReturn(ApiResponse.success(Map.of("trackingNumber", "SF1234567890")));

        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));

        when(orderConverter.enrichDetailDTO(any(), any(ProductDTO.class), any(), any(ContactDTO.class), any())).thenReturn(dto);

        OrderDetailDTO result = orderService.getOrderDetailByUser(100L, "ORDER001");
        assertNotNull(result);
        verify(contactFeignClient).getContactById(1);
        verify(logisticsFeignClient).getLatestLogistics("ORDER001", "DELIVERY");
    }

    @Test
    @DisplayName("OR-048 用户查询详情 - 订单不属于当前用户")
    void getOrderDetailByUser_notOwner() {
        when(orderMapper.selectOrderDetailByUser(999L, "ORDER001")).thenReturn(null);

        assertThrows(OrderException.class,
                () -> orderService.getOrderDetailByUser(999L, "ORDER001"));
    }

    @Test
    @DisplayName("OR-050 商家查询店铺订单列表")
    void getOrdersByShopId() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        when(orderMapper.selectAbstractOrdersByShopId("SHOP001")).thenReturn(List.of(order));
        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        mockContact(1);
        when(orderConverter.toSellerCardDTOList(anyList(), anyMap(), anyMap())).thenReturn(
                List.of(new SellerOrderCardDTO())
        );

        List<SellerOrderCardDTO> result = orderService.getOrdersByShopId("SHOP001");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("OR-051 商家查询订单详情")
    void getOrderDetailByShop() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        when(orderMapper.selectOrderDetailByShop("SHOP001", "ORDER001")).thenReturn(order);
        OrderDetailDTO dto = new OrderDetailDTO();
        when(orderConverter.toDetailDTO(order)).thenReturn(dto);
        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        when(orderConverter.enrichDetailDTO(any(), any(ProductDTO.class), any(), any(), any())).thenReturn(dto);

        OrderDetailDTO result = orderService.getOrderDetailByShop("SHOP001", "ORDER001");
        assertNotNull(result);
    }

    @Test
    @DisplayName("OR-052 商家查询详情 - 订单不属于该店铺")
    void getOrderDetailByShop_wrongShop() {
        when(orderMapper.selectOrderDetailByShop("WRONG_SHOP", "ORDER001")).thenReturn(null);

        assertThrows(OrderException.class,
                () -> orderService.getOrderDetailByShop("WRONG_SHOP", "ORDER001"));
    }

    // ==================== 状态机 (OR-063 ~ OR-065) ====================

    @Test
    @DisplayName("OR-063 非法状态转换 - CAS返回0")
    void casOptimisticLock_failure() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "DELIVERED");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "DELIVERED", "SHIPPED")).thenReturn(0);

        assertThrows(OrderException.class,
                () -> orderService.deliverOrder(100L, "ORDER001"));
    }

    @Test
    @DisplayName("OR-065 CAS乐观锁 - 先尝试PAID→CANCELLED再PENDING→CANCELLED")
    void casCancelOrder_triesPaidFirstThenPending() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "CANCELLED", "PAID")).thenReturn(1);

        orderService.cancelOrder(100L, "ORDER001");

        verify(orderMapper).updateOrderStatusCas("ORDER001", "CANCELLED", "PAID");
        verify(eventPublisher).publishAfterCommit(eq("STOCK_RESTORE"), eq("ORDER001"), isNull());
    }

    @Test
    @DisplayName("OR-064 状态机 - canTransition验证（单元级）")
    void stateMachine_canTransition() {
        Order order = new Order();

        order.setOrderStatus("PENDING");
        assertTrue(order.canTransition("PENDING", "PAID"));
        assertTrue(order.canTransition("PENDING", "CANCELLED"));
        assertFalse(order.canTransition("PENDING", "SHIPPED"));
        assertFalse(order.canTransition("PENDING", "DELIVERED"));

        order.setOrderStatus("PAID");
        assertTrue(order.canTransition("PAID", "SHIPPED"));
        assertTrue(order.canTransition("PAID", "CANCELLED"));
        assertFalse(order.canTransition("PAID", "PENDING"));

        order.setOrderStatus("SHIPPED");
        assertTrue(order.canTransition("SHIPPED", "DELIVERED"));
        assertTrue(order.canTransition("SHIPPED", "RETURN_PENDING"));
        assertFalse(order.canTransition("SHIPPED", "CANCELLED"));

        order.setOrderStatus("DELIVERED");
        assertTrue(order.canTransition("DELIVERED", "RETURN_PENDING"));
        assertTrue(order.canTransition("DELIVERED", "DELETED"));
        assertFalse(order.canTransition("DELIVERED", "PAID"));
    }

    @Test
    @DisplayName("OR-064 状态机 - transitionTo抛出异常")
    void stateMachine_transitionTo_throws() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "DELIVERED");
        assertThrows(IllegalStateException.class,
                () -> order.transitionTo("PAID"));
    }

    @Test
    @DisplayName("OR-064 状态机 - null参数返回false")
    void stateMachine_nullParams() {
        assertFalse(new Order().canTransition(null, "PAID"));
        assertFalse(new Order().canTransition("PENDING", null));
    }

    // ==================== 补充覆盖 (buildDetailDTO 边界) ====================

    @Test
    @DisplayName("OR-066 getOrderDetailByUser - 联系人查询异常时仍能返回详情（容错）")
    void getOrderDetailByUser_contactException() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        order.setContactId(99);
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);

        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setOrderId("ORDER001");
        when(orderConverter.toDetailDTO(order)).thenReturn(dto);
        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        when(contactFeignClient.getContactById(99)).thenThrow(new RuntimeException("Feign 联系人异常"));
        when(logisticsFeignClient.getLatestLogistics("ORDER001", "DELIVERY"))
                .thenReturn(ApiResponse.success(Map.of("trackingNumber", "SF111")));
        when(orderConverter.enrichDetailDTO(any(), any(ProductDTO.class), any(), any(), any())).thenReturn(dto);

        OrderDetailDTO result = orderService.getOrderDetailByUser(100L, "ORDER001");
        assertNotNull(result);
        verify(contactFeignClient).getContactById(99);
        verify(logisticsFeignClient).getLatestLogistics("ORDER001", "DELIVERY");
    }

    @Test
    @DisplayName("OR-067 getOrderDetailByUser - 物流查询异常时仍能返回详情（容错）")
    void getOrderDetailByUser_logisticsException() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        order.setContactId(99);
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);

        OrderDetailDTO dto = new OrderDetailDTO();
        when(orderConverter.toDetailDTO(order)).thenReturn(dto);
        ContactDTO contact = new ContactDTO();
        contact.setName("张三");
        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        when(contactFeignClient.getContactById(99)).thenReturn(ApiResponse.success(contact));
        when(logisticsFeignClient.getLatestLogistics("ORDER001", "DELIVERY"))
                .thenThrow(new RuntimeException("Feign 物流异常"));
        when(orderConverter.enrichDetailDTO(any(), any(ProductDTO.class), any(), any(ContactDTO.class), any())).thenReturn(dto);

        OrderDetailDTO result = orderService.getOrderDetailByUser(100L, "ORDER001");
        assertNotNull(result);
    }

    @Test
    @DisplayName("OR-068 getOrderDetailByUser - contactId 为 null 时跳过联系人查询")
    void getOrderDetailByUser_nullContactId() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        order.setContactId(null);
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);

        OrderDetailDTO dto = new OrderDetailDTO();
        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        when(orderConverter.toDetailDTO(order)).thenReturn(dto);
        when(logisticsFeignClient.getLatestLogistics("ORDER001", "DELIVERY"))
                .thenReturn(ApiResponse.success(Map.of("trackingNumber", "SF222")));
        when(orderConverter.enrichDetailDTO(any(), any(ProductDTO.class), any(), any(), any())).thenReturn(dto);

        OrderDetailDTO result = orderService.getOrderDetailByUser(100L, "ORDER001");
        assertNotNull(result);
        verify(contactFeignClient, never()).getContactById(any());
    }

    @Test
    @DisplayName("OR-069 getOrderDetailByUser - 物流响应 data 为 null 时仍能返回")
    void getOrderDetailByUser_logisticsDataNull() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        order.setContactId(99);
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);

        OrderDetailDTO dto = new OrderDetailDTO();
        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        when(orderConverter.toDetailDTO(order)).thenReturn(dto);
        when(contactFeignClient.getContactById(99))
                .thenReturn(ApiResponse.success(null));
        when(logisticsFeignClient.getLatestLogistics("ORDER001", "DELIVERY"))
                .thenReturn(ApiResponse.success(null));
        when(orderConverter.enrichDetailDTO(any(), any(ProductDTO.class), any(), any(), any())).thenReturn(dto);

        OrderDetailDTO result = orderService.getOrderDetailByUser(100L, "ORDER001");
        assertNotNull(result);
    }

    @Test
    @DisplayName("OR-070 createOrder - 整个 ApiResponse 为 null 时抛商品不存在")
    void createOrder_apiResponseNull() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(1);
        request.setContactId(1);

        when(productFeignClient.getProductById(1L)).thenReturn(null);

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder(request, 100L));
        assertTrue(ex.getMessage().contains("商品不存在"));
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    @DisplayName("OR-071 createOrder - 商品价格刚好等于总额（quantity=1）")
    void createOrder_priceEqualsTotal() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(1);
        request.setContactId(1);

        when(orderIdSelector.generate()).thenReturn("ORDER100");
        ProductDTO mockProduct = new ProductDTO(1L, "Test",
                BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        mockValidContact(1, 100L);
        when(orderMapper.insertOrder(any(Order.class))).thenReturn(1);
        when(productFeignClient.reserveStock(any(StockReserveRequest.class)))
                .thenReturn(ApiResponse.success(null));

        orderService.createOrder(request, 100L);

        verify(orderMapper).insertOrder(orderCaptor.capture());
        assertEquals(0, BigDecimal.valueOf(50).compareTo(orderCaptor.getValue().getTotalPrice()));
    }

    @Test
    @DisplayName("OR-072 createOrder - 商品价格为空时使用 BigDecimal.ZERO（防御性）")
    void createOrder_priceNull() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(3);
        request.setContactId(1);

        when(orderIdSelector.generate()).thenReturn("ORDER101");
        ProductDTO mockProduct = new ProductDTO(1L, "Free", null, null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        mockValidContact(1, 100L);
        when(orderMapper.insertOrder(any(Order.class))).thenReturn(1);
        when(productFeignClient.reserveStock(any(StockReserveRequest.class)))
                .thenReturn(ApiResponse.success(null));

        orderService.createOrder(request, 100L);

        verify(orderMapper).insertOrder(orderCaptor.capture());
        assertEquals(0, BigDecimal.ZERO.compareTo(orderCaptor.getValue().getTotalPrice()));
    }

    @Test
    @DisplayName("OR-075 confirmReturn - 订单不存在抛异常")
    void confirmReturn_notFound() {
        when(orderMapper.selectOrderDetailByShop("SHOP001", "MISSING")).thenReturn(null);

        assertThrows(OrderException.class,
                () -> orderService.confirmReturn("SHOP001", "MISSING"));
    }

    @Test
    @DisplayName("OR-076 cancelOrder - 订单不存在抛异常")
    void cancelOrder_notFound() {
        when(orderMapper.selectOrderDetailByUser(999L, "MISSING")).thenReturn(null);

        assertThrows(OrderException.class,
                () -> orderService.cancelOrder(999L, "MISSING"));
    }

    @Test
    @DisplayName("OR-077 shipOrder - 订单不存在抛异常")
    void shipOrder_notFound() {
        ShipOrderRequest req = new ShipOrderRequest();
        req.setTrackingNumber("SF123");
        req.setContactId(1);

        when(orderMapper.selectOrderDetailByShop("SHOP001", "MISSING")).thenReturn(null);

        assertThrows(OrderException.class,
                () -> orderService.shipOrder("SHOP001", "MISSING", req));
    }
}
