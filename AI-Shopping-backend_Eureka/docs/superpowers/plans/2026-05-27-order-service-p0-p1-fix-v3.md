# Order 服务 P0/P1 修复 — 实现计划 V3

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 解决 Order 服务 P0 数据一致性和 P1 并发安全问题

**架构:** 在现有 Redis 基础上引入 Redis Streams 作为异步消息队列，本地 txt 文件兜底 Redis 不可用时的消息投递，CAS 乐观锁解决并发竞态

**Tech Stack:** Spring Boot 3.2.3, MyBatis, Redis Streams, Jackson

**基础路径:** `order-service/src/main/java/com/gzasc/aishopping/order/`

---

### Task 1: OrderMapper — 新增 2 个 CAS 方法

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/mapper/OrderMapper.java`

- [ ] **Step 1: 新增 updateOrderStatusCas 和 updateOrderStatusCasMulti**

```java
// 追加到 OrderMapper.java 中现有方法之后

@Update("UPDATE t_order SET order_status = #{newStatus} WHERE order_id = #{orderId} AND order_status = #{oldStatus}")
int updateOrderStatusCas(@Param("orderId") String orderId,
                         @Param("newStatus") String newStatus,
                         @Param("oldStatus") String oldStatus);

@Update({"<script>",
         "UPDATE t_order SET order_status = #{newStatus}",
         "WHERE order_id = #{orderId} AND order_status IN",
         "<foreach collection='expectedStatuses' item='s' open='(' separator=',' close=')'>#{s}</foreach>",
         "</script>"})
int updateOrderStatusCasMulti(@Param("orderId") String orderId,
                              @Param("newStatus") String newStatus,
                              @Param("expectedStatuses") List<String> expectedStatuses);
```

- [ ] **Step 2: 编译确认**

Run: `mvn compile -pl order-service -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/mapper/OrderMapper.java
git commit -m "feat(order): add CAS update methods for optimistic locking"
```

---

### Task 2: OrderEventType — 事件类型枚举

**Files:**
- Create: `order-service/src/main/java/com/gzasc/aishopping/order/stream/OrderEventType.java`

- [ ] **Step 1: 创建枚举**

```java
package com.gzasc.aishopping.order.stream;

public enum OrderEventType {
    STOCK_CONFIRM,
    STOCK_RESTORE,
    LOGISTICS_CREATE
}
```

- [ ] **Step 2: Commit**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/stream/OrderEventType.java
git commit -m "feat(order): add OrderEventType enum for Redis Stream events"
```

---

### Task 3: RedisStreamConfig — Stream + Consumer Group 初始化

**Files:**
- Create: `order-service/src/main/java/com/gzasc/aishopping/order/stream/RedisStreamConfig.java`

- [ ] **Step 1: 创建配置类**

```java
package com.gzasc.aishopping.order.stream;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationUtils;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisStreamConfig {

    private final RedisTemplate<String, String> redisTemplate;

    static final String STREAM_KEY = "order:events";
    static final String GROUP_NAME = "order:processors";

    @PostConstruct
    public void init() {
        try {
            redisTemplate.execute((RedisCallback<String>) conn -> {
                conn.xGroupCreate(
                        SerializationUtils.serialize(STREAM_KEY),
                        SerializationUtils.serialize(GROUP_NAME),
                        org.springframework.data.redis.connection.RedisStreamCommands.ReadOffset.latest(),
                        true
                );
                return null;
            });
        } catch (Exception e) {
            log.info("Redis Stream group already exists, skip creation", e);
        }
    }
}
```

- [ ] **Step 2: 编译确认**

Run: `mvn compile -pl order-service -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/stream/RedisStreamConfig.java
git commit -m "feat(order): init Redis Stream and consumer group"
```

---

### Task 4: StreamListenerContainerConfig — Consumer 容器

**Files:**
- Create: `order-service/src/main/java/com/gzasc/aishopping/order/stream/StreamListenerContainerConfig.java`

- [ ] **Step 1: 创建容器配置**

```java
package com.gzasc.aishopping.order.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;

@Configuration
@Slf4j
public class StreamListenerContainerConfig {

    @Bean(destroyMethod = "stop")
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> container(
            RedisConnectionFactory cf,
            OrderEventConsumer consumer) {

        var opts = StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions
                .<String, MapRecord<String, String, String>>builder()
                .pollTimeout(Duration.ofMillis(100))
                .errorHandler(e -> log.error("Stream consumer error", e))
                .build();

        var container = StreamMessageListenerContainer.create(cf, opts);

        container.receive(
                Consumer.from(RedisStreamConfig.GROUP_NAME, "consumer-1"),
                StreamOffset.create(RedisStreamConfig.STREAM_KEY, ReadOffset.lastConsumed()),
                consumer
        );

        container.start();
        return container;
    }
}
```

- [ ] **Step 2: 编译确认**

Run: `mvn compile -pl order-service -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/stream/StreamListenerContainerConfig.java
git commit -m "feat(order): create StreamMessageListenerContainer for async consumer"
```

---

### Task 5: FileFallbackDaemon — 本地文件兜底

**Files:**
- Create: `order-service/src/main/java/com/gzasc/aishopping/order/stream/FileFallbackDaemon.java`

- [ ] **Step 1: 创建兜底工具类**

```java
package com.gzasc.aishopping.order.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileFallbackDaemon {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Path FALLBACK_DIR = Paths.get("data/failover");

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(FALLBACK_DIR);
        retryFailed();
    }

    public void sendOrFallback(String eventType, String orderId, Map<String, String> extra) {
        try {
            Map<String, String> msg = new LinkedHashMap<>();
            msg.put("eventType", eventType);
            msg.put("orderId", orderId);
            if (extra != null) {
                msg.putAll(extra);
            }
            redisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .ofMap(msg)
                            .withStreamKey(RedisStreamConfig.STREAM_KEY)
            );
            log.debug("消息发送成功 eventType={}, orderId={}", eventType, orderId);
        } catch (Exception e) {
            log.warn("Redis不可用，写入本地文件兜底 eventType={}, orderId={}", eventType, orderId);
            writeFallback(eventType, orderId, extra);
        }
    }

    private void writeFallback(String eventType, String orderId, Map<String, String> extra) {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("eventType", eventType);
            data.put("orderId", orderId);
            if (extra != null) {
                data.putAll(extra);
            }
            String json = objectMapper.writeValueAsString(data);
            String filename = "failover-" + System.currentTimeMillis() + "-" + orderId + ".txt";
            Files.writeString(FALLBACK_DIR.resolve(filename), json);
        } catch (IOException e) {
            log.error("写入本地兜底文件失败 eventType={}, orderId={}", eventType, orderId, e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void retryFailed() throws IOException {
        List<Path> files;
        try (var stream = Files.list(FALLBACK_DIR)) {
            files = stream.filter(p -> p.getFileName().toString().startsWith("failover-")).toList();
        }
        for (Path file : files) {
            try {
                String content = Files.readString(file);
                @SuppressWarnings("unchecked")
                Map<String, String> msg = objectMapper.readValue(content, Map.class);
                redisTemplate.opsForStream().add(
                        StreamRecords.newRecord().ofMap(msg).withStreamKey(RedisStreamConfig.STREAM_KEY));
                Files.delete(file);
                log.info("补发成功, 文件已清理, file={}", file);
            } catch (Exception e) {
                log.warn("补发失败, 文件保留等待下次重试, file={}", file);
            }
        }
    }
}
```

- [ ] **Step 2: 编译确认**

Run: `mvn compile -pl order-service -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/stream/FileFallbackDaemon.java
git commit -m "feat(order): add file fallback for Redis Stream message delivery"
```

---

### Task 6: OrderEventConsumer — 消息消费者

**Files:**
- Create: `order-service/src/main/java/com/gzasc/aishopping/order/stream/OrderEventConsumer.java`

- [ ] **Step 1: 创建消费者**

```java
package com.gzasc.aishopping.order.stream;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.feign.logistics.LogisticsFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.mapper.OrderMapper;
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
    private final ProductFeignClient productFeignClient;
    private final LogisticsFeignClient logisticsFeignClient;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        Map<String, String> msg = record.getValue();
        String eventType = msg.get("eventType");
        String orderId = msg.get("orderId");
        log.info("消费消息 eventType={}, orderId={}, msgId={}", eventType, orderId, record.getId());

        try {
            switch (OrderEventType.valueOf(eventType)) {
                case STOCK_CONFIRM    -> handleStockConfirm(msg);
                case STOCK_RESTORE    -> handleStockRestore(msg);
                case LOGISTICS_CREATE -> handleLogistics(msg);
            }
            redisTemplate.opsForStream().acknowledge(
                    RedisStreamConfig.STREAM_KEY,
                    RedisStreamConfig.GROUP_NAME,
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
        // 订单已取消 → 释放预占，否则确认预占
        if ("PAID".equals(o.getOrderStatus())) {
            Map<String, Object> result = productFeignClient.confirmReservation(orderId);
            Boolean success = (Boolean) result.get("success");
            if (!Boolean.TRUE.equals(success)) {
                log.warn("confirmReservation失败, orderId={}, msg={}", orderId, result.get("message"));
                throw new RuntimeException("confirmReservation failed: " + result.get("message"));
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
        com.gzasc.aishopping.common.dto.product.StockDeductRequest req =
                new com.gzasc.aishopping.common.dto.product.StockDeductRequest(
                        o.getProductId(), o.getQuantity());
        productFeignClient.restoreStock(req);
        log.info("库存恢复成功, orderId={}", orderId);
    }

    private void handleLogistics(Map<String, String> msg) {
        String orderId = msg.get("orderId");
        ApiResponse<Map<String, Object>> existing = logisticsFeignClient.getLatestLogistics(orderId, "DELIVERY");
        if (existing != null && existing.getData() != null) {
            log.info("SKIP logistics 已创建, orderId={}", orderId);
            return;
        }
        LogisticsRequest req = new LogisticsRequest();
        req.setOrderId(orderId);
        req.setType("DELIVERY");
        req.setContactId(Integer.valueOf(msg.get("contactId")));
        req.setTrackingNumber(msg.get("trackingNumber"));
        logisticsFeignClient.createLogistics(req);
        log.info("物流记录创建成功, orderId={}", orderId);
    }
}
```

- [ ] **Step 2: 编译确认**

Run: `mvn compile -pl order-service -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/stream/OrderEventConsumer.java
git commit -m "feat(order): implement Redis Stream consumer with idempotent handlers"
```

---

### Task 7: OrderServiceImpl — 改造 payOrder

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java:169-183`

- [ ] **Step 1: 注入 FileFallbackDaemon**

在 `OrderServiceImpl` 类开头追加注入（L36 之后）：

```java
private final FileFallbackDaemon fileFallbackDaemon;
```

- [ ] **Step 2: 改造 payOrder 方法**

替换 `payOrder` 方法体（L169-183）：

```java
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
```

添加 import：

```java
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.gzasc.aishopping.order.stream.FileFallbackDaemon;
import com.gzasc.aishopping.order.stream.OrderEventType;
import java.util.Map;
```

- [ ] **Step 3: 编译确认**

Run: `mvn compile -pl order-service -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java
git commit -m "fix(order): refactor payOrder to use CAS + async Redis Stream"
```

---

### Task 8: OrderServiceImpl — 改造 cancelOrder

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java:98-115`

- [ ] **Step 1: 改造 cancelOrder 方法**

替换 `cancelOrder` 方法体（L98-115）：

```java
@Override
@Transactional
public void cancelOrder(Long userId, String orderId) {
    Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
    if (order == null) {
        throw new OrderException("订单不存在或无权限取消");
    }

    String originalStatus = order.getOrderStatus();
    int updated = orderMapper.updateOrderStatusCasMulti(
            orderId, Order.CANCELLED, List.of(Order.PENDING, Order.PAID));
    if (updated <= 0) {
        log.warn("取消订单失败，状态已变更, orderId={}", orderId);
        throw new OrderException("订单状态已变更，取消失败");
    }

    if (Order.PAID.equals(originalStatus)) {
        StockDeductRequest stockReq = new StockDeductRequest(order.getProductId(), order.getQuantity());
        productFeignClient.restoreStock(stockReq);
        log.info("已支付订单取消，恢复库存, orderId={}", orderId);
    } else if (Order.PENDING.equals(originalStatus)) {
        productFeignClient.releaseReservation(orderId);
        log.info("未支付订单取消，释放预占, orderId={}", orderId);
    }
}
```

添加 import：

```java
import java.util.List;
```

- [ ] **Step 2: 编译确认**

Run: `mvn compile -pl order-service -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java
git commit -m "fix(order): refactor cancelOrder to use CAS optimistic locking"
```

---

### Task 9: OrderServiceImpl — 改造 shipOrder

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java:118-143`

- [ ] **Step 1: 改造 shipOrder 方法**

替换 `shipOrder` 方法体（L118-143）：

```java
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
```

- [ ] **Step 2: 编译确认**

Run: `mvn compile -pl order-service -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java
git commit -m "fix(order): refactor shipOrder to use CAS + async Redis Stream"
```

---

### Task 10: OrderServiceImpl — 改造 confirmReturn

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java:197-205`

- [ ] **Step 1: 改造 confirmReturn 方法**

替换 `confirmReturn` 方法体（L197-205）：

```java
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
```

- [ ] **Step 2: 编译确认**

Run: `mvn compile -pl order-service -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java
git commit -m "fix(order): refactor confirmReturn to use CAS + async Redis Stream"
```

---

### Task 11: 启用 @EnableScheduling

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/OrderServiceApplication.java`

- [ ] **Step 1: 添加 @EnableScheduling 注解**

```java
// 在类上添加
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
public class OrderServiceApplication { ... }
```

- [ ] **Step 2: 编译确认**

Run: `mvn compile -pl order-service -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/OrderServiceApplication.java
git commit -m "chore(order): enable scheduling for FileFallbackDaemon retry task"
```

---

### Task 12: 最终编译验证

- [ ] **Step 1: 全量编译**

Run: `mvn compile -pl order-service -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 确认新增文件清单**

```bash
git status --short
```
Expected: 5 new stream files + 2 modified files (OrderMapper + OrderServiceImpl + OrderServiceApplication)
