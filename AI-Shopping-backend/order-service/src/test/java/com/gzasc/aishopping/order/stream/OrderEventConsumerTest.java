package com.gzasc.aishopping.order.stream;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.dto.product.StockDeductRequest;
import com.gzasc.aishopping.common.feign.logistics.LogisticsFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.mapper.OrderMapper;
import com.gzasc.aishopping.order.mapper.ReturnRequestMapper;
import com.gzasc.aishopping.order.model.Order;
import org.springframework.data.redis.connection.stream.RecordId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private ProductFeignClient productFeignClient;
    @Mock
    private LogisticsFeignClient logisticsFeignClient;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private StreamOperations<String, Object, Object> streamOps;
    @Mock
    private RedisStreamConfig redisStreamConfig;
    @Mock
    private ReturnRequestMapper returnRequestMapper;

    private OrderEventConsumer consumer;

    @Captor
    private ArgumentCaptor<StockDeductRequest> stockDeductCaptor;
    @Captor
    private ArgumentCaptor<LogisticsRequest> logisticsCaptor;

    private Order createOrder(String orderId, String status) {
        Order o = new Order();
        o.setOrderId(orderId);
        o.setUserId(100L);
        o.setShopId("SHOP001");
        o.setProductId("1");
        o.setQuantity(2);
        o.setTotalPrice(BigDecimal.valueOf(100));
        o.setOrderStatus(status);
        o.setOrderDate(new Timestamp(System.currentTimeMillis()));
        o.setContactId(1);
        return o;
    }

    @SuppressWarnings("unchecked")
    private MapRecord<String, String, String> createRecord(Map<String, String> msg) {
        return MapRecord.create("order:events", msg);
    }

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.opsForStream()).thenReturn(streamOps);
        lenient().when(redisStreamConfig.getStreamKey()).thenReturn("order:events");
        lenient().when(redisStreamConfig.getGroupName()).thenReturn("order:processors");
        consumer = new OrderEventConsumer(orderMapper, returnRequestMapper,
                productFeignClient, logisticsFeignClient, redisTemplate, redisStreamConfig);
    }

    // ==================== STOCK_CONFIRM (OR-040 ~ OR-041) ====================

    @Test
    @DisplayName("OR-040 STOCK_CONFIRM - 正常消费（订单PAID）")
    void handleStockConfirm_paid() {
        Order order = createOrder("ORDER001", "PAID");
        when(orderMapper.selectOrderById("ORDER001")).thenReturn(order);
        when(productFeignClient.confirmReservation("ORDER001"))
                .thenReturn(ApiResponse.success(null));

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "STOCK_CONFIRM",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient).confirmReservation("ORDER001");
        verify(productFeignClient, never()).releaseReservation("ORDER001");
        verify(streamOps).acknowledge(anyString(), anyString(), any(RecordId.class));
    }

    @Test
    @DisplayName("OR-041 STOCK_CONFIRM - 订单非PAID时释放预占")
    void handleStockConfirm_notPaid() {
        Order order = createOrder("ORDER001", "CANCELLED");
        when(orderMapper.selectOrderById("ORDER001")).thenReturn(order);
        when(productFeignClient.releaseReservation("ORDER001"))
                .thenReturn(ApiResponse.success(null));

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "STOCK_CONFIRM",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient).releaseReservation("ORDER001");
        verify(productFeignClient, never()).confirmReservation("ORDER001");
    }

    @Test
    @DisplayName("OR-040 STOCK_CONFIRM - 订单不存在")
    void handleStockConfirm_orderNotFound() {
        when(orderMapper.selectOrderById("ORDER001")).thenReturn(null);

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "STOCK_CONFIRM",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient, never()).confirmReservation("ORDER001");
        verify(productFeignClient, never()).releaseReservation("ORDER001");
        verify(streamOps).acknowledge(anyString(), anyString(), any(RecordId.class));
    }

    @Test
    @DisplayName("OR-040 STOCK_CONFIRM - confirm失败抛出异常")
    void handleStockConfirm_failure() {
        Order order = createOrder("ORDER001", "PAID");
        when(orderMapper.selectOrderById("ORDER001")).thenReturn(order);
        when(productFeignClient.confirmReservation("ORDER001"))
                .thenReturn(ApiResponse.error("库存不足"));

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "STOCK_CONFIRM",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(streamOps, never()).acknowledge(anyString(), anyString(), any(RecordId.class));
    }

    // ==================== STOCK_RESTORE (OR-042) ====================

    @Test
    @DisplayName("OR-042 STOCK_RESTORE - 正常消费")
    void handleStockRestore_success() {
        when(valueOps.setIfAbsent("restore:done:ORDER001", "1", Duration.ofDays(7)))
                .thenReturn(true);

        Order order = createOrder("ORDER001", "RETURNED");
        when(orderMapper.selectOrderById("ORDER001")).thenReturn(order);
        when(productFeignClient.restoreStock(any(StockDeductRequest.class)))
                .thenReturn(ApiResponse.success(null));

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "STOCK_RESTORE",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient).restoreStock(stockDeductCaptor.capture());
        assertEquals(1L, stockDeductCaptor.getValue().getProductId());
        assertEquals(2, stockDeductCaptor.getValue().getQuantity());
    }

    @Test
    @DisplayName("OR-042 STOCK_RESTORE - 幂等跳过（key已存在）")
    void handleStockRestore_idempotent() {
        when(valueOps.setIfAbsent("restore:done:ORDER001", "1", Duration.ofDays(7)))
                .thenReturn(false);

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "STOCK_RESTORE",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient, never()).restoreStock(any());
    }

    @Test
    @DisplayName("OR-042 STOCK_RESTORE - 订单不存在仍跳过")
    void handleStockRestore_orderNotFound() {
        when(valueOps.setIfAbsent("restore:done:ORDER001", "1", Duration.ofDays(7)))
                .thenReturn(true);
        when(orderMapper.selectOrderById("ORDER001")).thenReturn(null);

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "STOCK_RESTORE",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient, never()).restoreStock(any());
    }

    // ==================== LOGISTICS_CREATE (OR-043 ~ OR-044) ====================

    @Test
    @DisplayName("OR-043 LOGISTICS_CREATE - 正常消费")
    void handleLogistics_success() {
        when(logisticsFeignClient.getLatestLogistics("ORDER001", "DELIVERY"))
                .thenReturn(null);
        when(logisticsFeignClient.createLogistics(any(LogisticsRequest.class)))
                .thenReturn(ApiResponse.success(Map.of("id", 1)));

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "LOGISTICS_CREATE",
                "orderId", "ORDER001",
                "contactId", "1",
                "trackingNumber", "SF1234567890"
        ));

        consumer.onMessage(record);

        verify(logisticsFeignClient).createLogistics(logisticsCaptor.capture());
        assertEquals("ORDER001", logisticsCaptor.getValue().getOrderId());
        assertEquals("DELIVERY", logisticsCaptor.getValue().getType());
        assertEquals(Integer.valueOf(1), logisticsCaptor.getValue().getContactId());
        assertEquals("SF1234567890", logisticsCaptor.getValue().getTrackingNumber());
    }

    @Test
    @DisplayName("OR-044 LOGISTICS_CREATE - 已有物流则跳过")
    void handleLogistics_skipExisting() {
        when(logisticsFeignClient.getLatestLogistics("ORDER001", "DELIVERY"))
                .thenReturn(ApiResponse.success(Map.of("trackingNumber", "SF1234567890")));

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "LOGISTICS_CREATE",
                "orderId", "ORDER001",
                "contactId", "1",
                "trackingNumber", "SF1234567890"
        ));

        consumer.onMessage(record);

        verify(logisticsFeignClient, never()).createLogistics(any());
    }

    // ==================== RESERVATION_RELEASE (OR-080 ~ OR-081) ====================

    @Test
    @DisplayName("OR-080 消费 RESERVATION_RELEASE - 释放预占成功")
    void handleReservationRelease_success() {
        when(valueOps.setIfAbsent("release:done:ORDER001", "1", Duration.ofDays(7)))
                .thenReturn(true);
        when(productFeignClient.releaseReservation("ORDER001"))
                .thenReturn(ApiResponse.success(null));

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "RESERVATION_RELEASE",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient).releaseReservation("ORDER001");
    }

    @Test
    @DisplayName("OR-081 消费 RESERVATION_RELEASE - 幂等跳过")
    void handleReservationRelease_idempotent() {
        when(valueOps.setIfAbsent("release:done:ORDER001", "1", Duration.ofDays(7)))
                .thenReturn(false);

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "RESERVATION_RELEASE",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient, never()).releaseReservation(anyString());
    }

    // ==================== 未知事件类型 ====================

    @Test
    @DisplayName("未知事件类型 - 异常处理不确认消息")
    void unknownEventType() {
        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "UNKNOWN_EVENT",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(streamOps, never()).acknowledge(anyString(), anyString(), any(RecordId.class));
    }

    // ==================== 补充覆盖 (eventType null、logistics data==null 等) ====================

    @Test
    @DisplayName("OR-EC-01 eventType 为 null - 被异常捕获且不确认消息")
    void handle_nullEventType() {
        MapRecord<String, String, String> record = createRecord(Map.of(
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(streamOps, never()).acknowledge(anyString(), anyString(), any(RecordId.class));
    }

    @Test
    @DisplayName("OR-EC-02 eventType 为 null 时不影响其他业务调用")
    void handle_nullEventType_noSideEffect() {
        MapRecord<String, String, String> record = createRecord(Map.of(
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient, never()).confirmReservation("ORDER001");
        verify(productFeignClient, never()).releaseReservation("ORDER001");
        verify(productFeignClient, never()).restoreStock(any());
        verify(logisticsFeignClient, never()).createLogistics(any());
    }

    @Test
    @DisplayName("OR-EC-03 handleLogistics - 已有物流但 data 为 null 时继续创建")
    void handleLogistics_existingDataNull() {
        when(logisticsFeignClient.getLatestLogistics("ORDER001", "DELIVERY"))
                .thenReturn(ApiResponse.success(null));
        when(logisticsFeignClient.createLogistics(any(LogisticsRequest.class)))
                .thenReturn(ApiResponse.success(Map.of("id", 1)));

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "LOGISTICS_CREATE",
                "orderId", "ORDER001",
                "contactId", "1",
                "trackingNumber", "SF123"
        ));

        consumer.onMessage(record);

        verify(logisticsFeignClient).createLogistics(any(LogisticsRequest.class));
        verify(streamOps).acknowledge(anyString(), anyString(), any(RecordId.class));
    }

    @Test
    @DisplayName("OR-EC-04 handleStockRestore - Redis 返回 null（视为非首次）")
    void handleStockRestore_firstNull() {
        when(valueOps.setIfAbsent("restore:done:ORDER001", "1", Duration.ofDays(7)))
                .thenReturn(null);

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "STOCK_RESTORE",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient, never()).restoreStock(any());
    }

    @Test
    @DisplayName("OR-EC-05 handleStockRestore - 订单存在时调用 restoreStock 但不检查返回值")
    void handleStockRestore_noResponseCheck() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        Order order = createOrder("ORDER001", "RETURNED");
        when(orderMapper.selectOrderById("ORDER001")).thenReturn(order);
        when(productFeignClient.restoreStock(any(StockDeductRequest.class)))
                .thenReturn(ApiResponse.error("失败"));

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "STOCK_RESTORE",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient).restoreStock(any(StockDeductRequest.class));
        verify(streamOps).acknowledge(anyString(), anyString(), any(RecordId.class));
    }

    @Test
    @DisplayName("OR-EC-06 handleStockConfirm - confirmReservation 返回 null 时抛异常不确认")
    void handleStockConfirm_nullResult() {
        Order order = createOrder("ORDER001", "PAID");
        when(orderMapper.selectOrderById("ORDER001")).thenReturn(order);
        when(productFeignClient.confirmReservation("ORDER001")).thenReturn(null);

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "STOCK_CONFIRM",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(streamOps, never()).acknowledge(anyString(), anyString(), any(RecordId.class));
    }

    @Test
    @DisplayName("OR-EC-07 handleStockConfirm - 非 PAID 时 releaseReservation 返回 null 也不抛异常（按原代码）")
    void handleStockConfirm_notPaidReleaseNull() {
        Order order = createOrder("ORDER001", "CANCELLED");
        when(orderMapper.selectOrderById("ORDER001")).thenReturn(order);
        when(productFeignClient.releaseReservation("ORDER001")).thenReturn(null);

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "STOCK_CONFIRM",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient).releaseReservation("ORDER001");
        verify(streamOps).acknowledge(anyString(), anyString(), any(RecordId.class));
    }

    @Test
    @DisplayName("OR-EC-08 handleLogistics - getLatestLogistics 返回 ApiResponse 但 code 非 200")
    void handleLogistics_errorResponse() {
        when(logisticsFeignClient.getLatestLogistics("ORDER001", "DELIVERY"))
                .thenReturn(ApiResponse.error("调用失败"));
        when(logisticsFeignClient.createLogistics(any(LogisticsRequest.class)))
                .thenReturn(ApiResponse.success(Map.of("id", 1)));

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "LOGISTICS_CREATE",
                "orderId", "ORDER001",
                "contactId", "1",
                "trackingNumber", "SF123"
        ));

        consumer.onMessage(record);

        verify(logisticsFeignClient).createLogistics(any());
    }
}
