package com.gzasc.aishopping.order.stream;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.dto.product.StockDeductRequest;
import com.gzasc.aishopping.common.feign.logistics.LogisticsFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.mapper.OrderMapper;
import com.gzasc.aishopping.order.mapper.ReturnRequestMapper;
import com.gzasc.aishopping.order.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final OrderMapper orderMapper;
    private final ReturnRequestMapper returnRequestMapper;
    private final ProductFeignClient productFeignClient;
    private final LogisticsFeignClient logisticsFeignClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisStreamConfig redisStreamConfig;

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        Map<String, String> msg = record.getValue();
        String eventType = msg.get("eventType");
        String orderId = msg.get("orderId");
        log.info("消费消息 eventType={}, orderId={}, msgId={}", eventType, orderId, record.getId());

        try {
            switch (OrderEventType.valueOf(eventType)) {
                case STOCK_CONFIRM      -> handleStockConfirm(msg);
                case STOCK_RESTORE      -> handleStockRestore(msg);
                case LOGISTICS_CREATE   -> handleLogistics(msg);
                case RESERVATION_RELEASE -> handleReservationRelease(msg);
                case RETURN_REQUEST_CLEANUP -> handleReturnRequestCleanup(msg);
            }
            redisTemplate.opsForStream().acknowledge(
                    redisStreamConfig.getStreamKey(),
                    redisStreamConfig.getGroupName(),
                    record.getId());
            log.debug("消息确认完成, msgId={}", record.getId());
        } catch (Exception e) {
            log.error("消费失败 eventType={}, orderId={}, 消息留在Pending列表重试",
                    eventType, orderId, e);
        }
    }

    private void handleStockConfirm(Map<String, String> msg) {
        String orderId = msg.get("orderId");
        Order o = orderMapper.selectOrderById(orderId);
        if (o == null) {
            log.warn("订单不存在, orderId={}", orderId);
            return;
        }
        if ("PAID".equals(o.getOrderStatus())) {
            ApiResponse<Void> result = productFeignClient.confirmReservation(orderId);
            if (result == null || result.getCode() != 200) {
                log.warn("confirmReservation失败, orderId={}, msg={}", orderId, result != null ? result.getMessage() : "null");
                throw new RuntimeException("confirmReservation failed: " + (result != null ? result.getMessage() : "null"));
            }
            log.info("库存确认成功, orderId={}", orderId);
        } else {
            productFeignClient.releaseReservation(orderId);
            log.info("订单已取消, 释放预占, orderId={}", orderId);
        }
    }

    private void handleStockRestore(Map<String, String> msg) {
        String orderId = msg.get("orderId");
        String idempotentKey = "restore:done:" + orderId;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", Duration.ofDays(7));
        if (Boolean.FALSE.equals(first)) {
            log.info("SKIP restoreStock 已执行过, orderId={}", orderId);
            return;
        }
        Order o = orderMapper.selectOrderById(orderId);
        if (o == null) {
            log.warn("订单不存在, orderId={}", orderId);
            return;
        }
        productFeignClient.restoreStock(new StockDeductRequest(Long.valueOf(o.getProductId()), o.getQuantity()));
        log.info("库存恢复成功, orderId={}", orderId);
    }

    private void handleReservationRelease(Map<String, String> msg) {
        String orderId = msg.get("orderId");
        String idempotentKey = "release:done:" + orderId;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", Duration.ofDays(7));
        if (Boolean.FALSE.equals(first)) {
            log.info("SKIP releaseReservation 已执行过, orderId={}", orderId);
            return;
        }
        productFeignClient.releaseReservation(orderId);
        log.info("预占库存释放成功, orderId={}", orderId);
    }

    private void handleLogistics(Map<String, String> msg) {
        String orderId = msg.get("orderId");
        try {
            ApiResponse<Map<String, Object>> existing = logisticsFeignClient.getLatestLogistics(orderId, "DELIVERY");
            if (existing != null && existing.getData() != null) {
                log.info("SKIP logistics 已创建, orderId={}", orderId);
                return;
            }
        } catch (Exception e) {
            log.warn("查询物流信息异常, 继续创建物流记录, orderId={}", orderId, e);
        }
        LogisticsRequest req = new LogisticsRequest();
        req.setOrderId(orderId);
        req.setType("DELIVERY");
        req.setContactId(Integer.valueOf(msg.get("contactId")));
        req.setTrackingNumber(msg.get("trackingNumber"));
        logisticsFeignClient.createLogistics(req);
        log.info("物流记录创建成功, orderId={}", orderId);
    }

    private void handleReturnRequestCleanup(Map<String, String> msg) {
        String orderId = msg.get("orderId");
        String userIdStr = msg.get("userId");
        if (userIdStr == null) {
            log.warn("userId 为空, 跳过删除退货申请, orderId={}", orderId);
            return;
        }
        Long userId = Long.valueOf(userIdStr);
        returnRequestMapper.deleteByOrderIdAndUser(orderId, userId);
        log.info("异步删除退货申请成功, orderId={}, userId={}", orderId, userId);
    }
}
