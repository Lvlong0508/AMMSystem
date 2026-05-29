package com.gzasc.aishopping.order.service;

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
import com.gzasc.aishopping.order.service.impl.OrderServiceImpl;
import com.gzasc.aishopping.order.stream.FileFallbackDaemon;
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
import java.util.List;
import java.util.Map;

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
    private OrderConverter orderConverter;
    @Mock
    private FileFallbackDaemon fileFallbackDaemon;

    private OrderServiceImpl orderService;

    @Captor
    private ArgumentCaptor<StockReserveRequest> stockReserveCaptor;
    @Captor
    private ArgumentCaptor<StockDeductRequest> stockDeductCaptor;
    @Captor
    private ArgumentCaptor<Order> orderCaptor;
    @Captor
    private ArgumentCaptor<DeletedOrder> deletedOrderCaptor;

    private Order createOrder(String orderId, Long userId, String shopId, String status) {
        Order o = new Order();
        o.setOrderId(orderId);
        o.setUserId(userId);
        o.setShopId(shopId);
        o.setProductId("1");
        o.setQuantity(2);
        o.setTotalPrice(100.0);
        o.setOrderStatus(status);
        o.setOrderDate(new Timestamp(System.currentTimeMillis()));
        o.setContactId(1);
        return o;
    }

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderMapper, deletedOrderMapper, orderIdSelector,
                productFeignClient, logisticsFeignClient, contactFeignClient,
                orderConverter, fileFallbackDaemon);
    }

    // ==================== 下单 (OR-001 ~ OR-005) ====================

    @Test
    @DisplayName("OR-001 正常下单 - 所有参数合法")
    void createOrder_success() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId("1");
        request.setQuantity(2);
        request.setContactId(1);

        when(orderIdSelector.generate()).thenReturn("2026052800001ABCDE");
        ProductDTO mockProduct = new ProductDTO(1L, "Test", 50.0, null, null, 10, 100L, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
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
        assertEquals("1", stockReserveCaptor.getValue().getProductId());
        assertEquals(2, stockReserveCaptor.getValue().getQuantity());
    }

    @Test
    @DisplayName("OR-002 下单 - 商品不存在")
    void createOrder_productNotFound() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId("999");
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
        request.setProductId("1");
        request.setQuantity(5);
        request.setContactId(1);

        ProductDTO mockProduct = new ProductDTO(1L, "Test", 50.0, null, null, 3, 100L, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder(request, 100L));
        assertTrue(ex.getMessage().contains("库存不足"));
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    @DisplayName("OR-004 下单 - 联系地址ID任意（当前代码不验证）")
    void createOrder_arbitraryContactId() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId("1");
        request.setQuantity(1);
        request.setContactId(999);

        when(orderIdSelector.generate()).thenReturn("2026052800002ABCDE");
        ProductDTO mockProduct = new ProductDTO(1L, "Test", 50.0, null, null, 10, 100L, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
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
        request.setProductId("1");
        request.setQuantity(1);
        request.setContactId(1);

        when(orderIdSelector.generate()).thenReturn("2026052800003ABCDE");
        ProductDTO mockProduct = new ProductDTO(1L, "Test", 50.0, null, null, 10, 100L, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
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
        request.setProductId("1");
        request.setQuantity(1);
        request.setContactId(1);

        ProductDTO mockProduct = new ProductDTO(1L, "Test", 50.0, null, null, 10, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder(request, 100L));
        assertTrue(ex.getMessage().contains("店铺信息"));
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

        verify(productFeignClient).releaseReservation("ORDER001");
        verify(productFeignClient, never()).restoreStock(any());
    }

    @Test
    @DisplayName("OR-015 取消 PAID 订单 - 恢复实际库存")
    void cancelOrder_paid() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "CANCELLED", "PAID")).thenReturn(1);

        orderService.cancelOrder(100L, "ORDER001");

        verify(productFeignClient).restoreStock(any(StockDeductRequest.class));
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

    // ==================== 退货 (OR-028 ~ OR-035) ====================

    @Test
    @DisplayName("OR-028 申请退货 - SHIPPED→RETURN_PENDING")
    void requestReturn_shipped() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "SHIPPED");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "RETURN_PENDING", "SHIPPED")).thenReturn(1);

        orderService.requestReturn(100L, "ORDER001");

        verify(orderMapper).updateOrderStatusCas("ORDER001", "RETURN_PENDING", "SHIPPED");
    }

    @Test
    @DisplayName("OR-029 申请退货 - DELIVERED→RETURN_PENDING")
    void requestReturn_delivered() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "DELIVERED");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "RETURN_PENDING", "SHIPPED")).thenReturn(0);
        when(orderMapper.updateOrderStatusCas("ORDER001", "RETURN_PENDING", "DELIVERED")).thenReturn(1);

        orderService.requestReturn(100L, "ORDER001");

        verify(orderMapper).updateOrderStatusCas("ORDER001", "RETURN_PENDING", "DELIVERED");
    }

    @Test
    @DisplayName("OR-030 申请退货 - 不支持的状态")
    void requestReturn_wrongStatus() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "RETURN_PENDING", "SHIPPED")).thenReturn(0);
        when(orderMapper.updateOrderStatusCas("ORDER001", "RETURN_PENDING", "DELIVERED")).thenReturn(0);

        assertThrows(OrderException.class,
                () -> orderService.requestReturn(100L, "ORDER001"));
    }

    @Test
    @DisplayName("OR-031 审核退货 - RETURN_PENDING→RETURNING")
    void approveReturn_success() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "RETURN_PENDING");
        when(orderMapper.selectOrderDetailByShop("SHOP001", "ORDER001")).thenReturn(order);
        when(orderMapper.updateOrderStatusCas("ORDER001", "RETURNING", "RETURN_PENDING")).thenReturn(1);

        orderService.approveReturn("SHOP001", "ORDER001");

        verify(orderMapper).updateOrderStatusCas("ORDER001", "RETURNING", "RETURN_PENDING");
    }

    @Test
    @DisplayName("OR-032 审核退货 - 非RETURN_PENDING（订单不存在）")
    void approveReturn_wrongStatus() {
        when(orderMapper.selectOrderDetailByShop("SHOP001", "ORDER001")).thenReturn(null);

        assertThrows(OrderException.class,
                () -> orderService.approveReturn("SHOP001", "ORDER001"));
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
        when(orderConverter.toUserAbstractDTOList(anyList())).thenReturn(
                List.of(new OrderAbstractUserDTO())
        );

        List<OrderAbstractUserDTO> result = orderService.getOrdersByUserId(100L);
        assertEquals(1, result.size());
        verify(orderMapper).selectAbstractOrdersByUserId(100L);
    }

    @Test
    @DisplayName("OR-055 用户查询空列表")
    void getOrdersByUserId_empty() {
        when(orderMapper.selectAbstractOrdersByUserId(100L)).thenReturn(List.of());
        when(orderConverter.toUserAbstractDTOList(List.of())).thenReturn(List.of());

        List<OrderAbstractUserDTO> result = orderService.getOrdersByUserId(100L);
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

        when(orderConverter.enrichDetailDTO(any(), any(ContactDTO.class), any())).thenReturn(dto);

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
        when(orderConverter.toSellerAbstractDTOList(anyList())).thenReturn(
                List.of(new OrderAbstractSellerDTO())
        );

        List<OrderAbstractSellerDTO> result = orderService.getOrdersByShopId("SHOP001");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("OR-051 商家查询订单详情")
    void getOrderDetailByShop() {
        Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
        when(orderMapper.selectOrderDetailByShop("SHOP001", "ORDER001")).thenReturn(order);
        OrderDetailDTO dto = new OrderDetailDTO();
        when(orderConverter.toDetailDTO(order)).thenReturn(dto);

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
        verify(productFeignClient).restoreStock(any(StockDeductRequest.class));
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
}
