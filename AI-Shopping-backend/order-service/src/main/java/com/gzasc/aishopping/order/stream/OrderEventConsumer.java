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

/**
 * Redis Stream 消息消费者。
 * 根据 eventType 将消息分发到对应的处理方法，处理成功后手动 ACK，
 * 失败时异常抛出，消息留在 Pending 列表等待重新投递。
 */
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

    /**
     * Redis Stream 消息回调入口。解析消息中的 eventType 和 orderId，
     * 按事件类型分发处理，成功后发送 ACK 确认。
     */
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

    /**
     * 处理库存确认。根据订单当前状态决定调用 confirmReservation 还是 releaseReservation。
     * - 已支付(PAID)：确认预占库存完成实际扣减
     * - 其他状态：取消订单，释放预占库存
     */
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

    /**
     * 处理库存恢复。先通过 Redis setIfAbsent 做幂等控制，
     * 确保同一订单的库存恢复只执行一次，然后通过 Feign 调用商品服务恢复库存。
     */
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

    /**
     * 处理预占库存释放。通过 Redis 幂等键防止重复释放，
     * 调用商品服务的 releaseReservation 接口完成释放。
     */
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

    /**
     * 处理物流创建。先查询该订单是否已存在物流记录避免重复创建，
     * 若不存在则组装 LogisticsRequest 通过 Feign 调用物流服务创建。
     */
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

    /**
     * 处理退货申请异步清理。根据订单号和用户 ID 删除关联的退货申请记录，
     * 用于订单取消或售后流程完成后的数据清理。
     */
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
