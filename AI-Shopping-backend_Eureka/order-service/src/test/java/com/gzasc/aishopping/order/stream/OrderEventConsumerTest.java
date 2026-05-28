package com.gzasc.aishopping.order.stream;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.dto.product.StockDeductRequest;
import com.gzasc.aishopping.common.feign.logistics.LogisticsFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.mapper.OrderMapper;
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
        o.setTotalPrice(100.0);
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
        consumer = new OrderEventConsumer(orderMapper, productFeignClient,
                logisticsFeignClient, redisTemplate);
    }

    // ==================== STOCK_CONFIRM (OR-040 ~ OR-041) ====================

    @Test
    @DisplayName("OR-040 STOCK_CONFIRM - 正常消费（订单PAID）")
    void handleStockConfirm_paid() {
        Order order = createOrder("ORDER001", "PAID");
        when(orderMapper.selectOrderById("ORDER001")).thenReturn(order);
        when(productFeignClient.confirmReservation("ORDER001"))
                .thenReturn(Map.of("success", true));

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "STOCK_CONFIRM",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient).confirmReservation("ORDER001");
        verify(productFeignClient, never()).releaseReservation(anyString());
        verify(streamOps).acknowledge(anyString(), anyString(), any(RecordId.class));
    }

    @Test
    @DisplayName("OR-041 STOCK_CONFIRM - 订单非PAID时释放预占")
    void handleStockConfirm_notPaid() {
        Order order = createOrder("ORDER001", "CANCELLED");
        when(orderMapper.selectOrderById("ORDER001")).thenReturn(order);
        when(productFeignClient.releaseReservation("ORDER001"))
                .thenReturn(Map.of("success", true));

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "STOCK_CONFIRM",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient).releaseReservation("ORDER001");
        verify(productFeignClient, never()).confirmReservation(anyString());
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

        verify(productFeignClient, never()).confirmReservation(anyString());
        verify(productFeignClient, never()).releaseReservation(anyString());
        verify(streamOps).acknowledge(anyString(), anyString(), any(RecordId.class));
    }

    @Test
    @DisplayName("OR-040 STOCK_CONFIRM - confirm失败抛出异常")
    void handleStockConfirm_failure() {
        Order order = createOrder("ORDER001", "PAID");
        when(orderMapper.selectOrderById("ORDER001")).thenReturn(order);
        when(productFeignClient.confirmReservation("ORDER001"))
                .thenReturn(Map.of("success", false, "message", "库存不足"));

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
                .thenReturn(Map.of("success", true));

        MapRecord<String, String, String> record = createRecord(Map.of(
                "eventType", "STOCK_RESTORE",
                "orderId", "ORDER001"
        ));

        consumer.onMessage(record);

        verify(productFeignClient).restoreStock(stockDeductCaptor.capture());
        assertEquals("1", stockDeductCaptor.getValue().getProductId());
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
}
