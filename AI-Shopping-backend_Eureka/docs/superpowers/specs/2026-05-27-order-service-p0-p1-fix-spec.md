# Order 服务 P0/P1 修复方案 Spec

> 日期: 2026-05-27
> 状态: 待实现
> 基于: `docs/superpowers/plans/2026-05-24-order-service-fix-plan-v2.md`

---

## 1. 问题概览

### P0 — 数据一致性

| # | 问题 | 根因 |
|---|------|------|
| P0-1 | 退货不恢复库存 | `confirmReturn()` 只改状态，没调 `restoreStock` |
| P0-2 | payOrder 跨服务不一致 | `confirmReservation` 扣库存成功 → `updateOrderStatus` 失败 |
| P0-3 | createOrder 孤儿 reservation | `reserveStock` 成功但本地事务回滚（已有 ReservationCleanupTask 兜底，不做改动） |
| P0-4 | shipOrder 孤儿物流记录 | `createLogistics` 成功但 `updateOrderStatus` 失败 |

### P1 — 并发安全

| # | 问题 | 根因 |
|---|------|------|
| P1-5 | 并发取消重复恢复库存 | 无双检锁定，两线程读到 PAID 各调一次 `restoreStock` |
| P1-6 | pay/cancel 竞态 | 状态检查与 Feign 调用非原子 |

---

## 2. 方案架构

### 技术选型

| 组件 | 选型 | 理由 |
|------|------|------|
| 消息队列 | Redis Streams | 项目已有 Redis，零新依赖，支持 Consumer Group + 消息持久化 |
| 并发防护 | CAS 乐观锁 | `UPDATE ... WHERE order_id=? AND order_status=?` 最轻量的原子防护 |
| 消息兜底 | 本地 txt 文件 | afterCommit 发消息失败时写本地文件，启动/定时任务补发 |

### 核心流程

```
┌─ OrderServiceImpl ──────────────────────────────────────┐
│  @Transactional                                          │
│  ① 校验 + CAS 更新订单状态                                 │
│  ② afterCommit → 发布事件到 Redis Stream                   │
│  ③ Redis 不可用 → 写本地 txt 兜底                          │
└──────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─ Redis Stream: order:events (Consumer Group: order:processors)
│  STOCK_CONFIRM    → confirmReservation (天然 CAS 幂等)
│  STOCK_RESTORE    → restoreStock (Redis SET 幂等 + DB 唯一约束)
│  LOGISTICS_CREATE → createLogistics (查重幂等)
└──────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─ FileFallbackDaemon ───────────────────────────────────┐
│  启动时 + 每分钟扫描 failover-*.txt                      │
│  补发到 Redis Stream，发送成功则删除文件                   │
└──────────────────────────────────────────────────────────┘
```

---

## 3. 新增文件设计

### 3.1 `OrderEventType.java` — 事件类型枚举

```java
public enum OrderEventType {
    STOCK_CONFIRM,      // payOrder → 确认预占 + 扣减库存
    STOCK_RESTORE,      // confirmReturn → 恢复库存
    LOGISTICS_CREATE    // shipOrder → 创建物流记录
}
```

### 3.2 `RedisStreamConfig.java` — Stream 初始化

```java
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisStreamConfig {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String STREAM_KEY = "order:events";
    private static final String GROUP_NAME = "order:processors";

    @PostConstruct
    public void init() {
        try {
            redisTemplate.execute((RedisCallback<String>) conn -> {
                conn.xGroupCreate(
                        SerializationUtils.serialize(STREAM_KEY),
                        SerializationUtils.serialize(GROUP_NAME),
                        ReadOffset.latest(),
                        true  // MKSTREAM: 流不存在则自动创建
                );
                return null;
            });
        } catch (Exception e) {
            log.info("Redis Stream group already exists, skip creation");
        }
    }
}
```

### 3.3 `OrderEventConsumer.java` — 消息消费者

```java
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
            redisTemplate.opsForStream().acknowledge("order:events", "order:processors", record.getId());
            log.debug("消息确认完成, msgId={}", record.getId());
        } catch (Exception e) {
            log.error("消费失败 eventType={}, orderId={}", eventType, orderId, e);
        }
    }

    private void handleStockConfirm(Map<String, String> msg) {
        String orderId = msg.get("orderId");
        Order o = orderMapper.selectByOrderId(orderId);
        if (o == null) return;
        if (!"PAID".equals(o.getOrderStatus())) {
            log.info("订单已取消，释放预占, orderId={}", orderId);
            productFeignClient.releaseReservation(orderId);
            return;
        }
        // 天然幂等: confirmReservation 是 CAS (RESERVED → CONFIRMED)
        productFeignClient.confirmReservation(orderId);
    }

    private void handleStockRestore(Map<String, String> msg) {
        String orderId = msg.get("orderId");
        // Redis SET 幂等键，7天有效期
        String idempotentKey = "restore:done:" + orderId;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", Duration.ofDays(7));
        if (Boolean.FALSE.equals(first)) {
            log.info("SKIP restoreStock 已执行过, orderId={}", orderId);
            return;
        }
        Order o = orderMapper.selectByOrderId(orderId);
        productFeignClient.restoreStock(o.getProductId(), o.getQuantity());
    }

    private void handleLogistics(Map<String, String> msg) {
        String orderId = msg.get("orderId");
        var existing = logisticsFeignClient.getByOrderId(orderId);
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
    }
}
```

### 3.4 `FileFallbackDaemon.java` — 本地文件兜底

```java
@Component
@Slf4j
public class FileFallbackDaemon {

    private static final Path FALLBACK_DIR = Path.of("data/failover");

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(FALLBACK_DIR);
        // 启动时补发遗留消息
        retryFailed();
    }

    // 正常调用：写入 Redis Stream，失败则写文件
    public void sendOrFallback(String eventType, String orderId, Map<String, String> extra) {
        try {
            Map<String, String> msg = new HashMap<>();
            msg.put("eventType", eventType);
            msg.put("orderId", orderId);
            if (extra != null) msg.putAll(extra);
            redisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .ofMap(msg)
                            .withStreamKey("order:events")
            );
            log.debug("消息发送成功 eventType={}, orderId={}", eventType, orderId);
        } catch (Exception e) {
            log.warn("Redis 不可用，写入本地文件兜底 eventType={}, orderId={}", eventType, orderId);
            writeFallback(eventType, orderId, extra);
        }
    }

    private void writeFallback(String eventType, String orderId, Map<String, String> extra) {
        String filename = "failover-" + System.currentTimeMillis() + "-" + orderId + ".txt";
        Map<String, String> data = new LinkedHashMap<>();
        data.put("eventType", eventType);
        data.put("orderId", orderId);
        if (extra != null) data.putAll(extra);
        Files.writeString(FALLBACK_DIR.resolve(filename), new JSONObject(data).toString());
    }

    // 定时重试（每分钟），补发成功自动删除文件
    @Scheduled(fixedRate = 60000)
    public void retryFailed() throws IOException {
        List<Path> files = Files.list(FALLBACK_DIR)
                .filter(p -> p.getFileName().toString().startsWith("failover-"))
                .toList();
        for (Path file : files) {
            try {
                String content = Files.readString(file);
                Map<String, String> msg = new JSONObject(content).toMap();
                redisTemplate.opsForStream().add(
                        StreamRecords.newRecord().ofMap(msg).withStreamKey("order:events"));
                // 补发成功 → 删除文件，避免重复处理
                Files.delete(file);
                log.info("补发成功, 文件已清理, file={}", file);
            } catch (Exception e) {
                log.warn("补发失败, 文件保留等待下次重试, file={}", file);
            }
        }
    }
}
```

### 3.5 `StreamListenerContainerConfig.java` — Consumer 容器配置

```java
@Configuration
public class StreamListenerContainerConfig {

    @Bean(destroyMethod = "stop")
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> container(
            RedisConnectionFactory cf, OrderEventConsumer consumer) {
        var opts = StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofMillis(100))
                .errorHandler(e -> log.error("Stream consumer error", e))
                .build();
        var container = StreamMessageListenerContainer.create(cf, opts);
        container.receive(
                Consumer.from("order:processors", "consumer-1"),
                StreamOffset.create("order:events", ReadOffset.lastConsumed()),
                consumer
        );
        return container;
    }
}
```

---

## 4. 改造文件清单

### 4.1 `OrderMapper.xml` — +2 个 CAS 方法

```xml
<!-- 单状态 CAS -->
<update id="updateOrderStatusCas">
    UPDATE t_order SET order_status = #{newStatus}
    WHERE order_id = #{orderId} AND order_status = #{oldStatus}
</update>

<!-- 多状态 CAS (用于 cancel: 允许 PENDING 或 PAID) -->
<update id="updateOrderStatusCasMulti">
    UPDATE t_order SET order_status = #{newStatus}
    WHERE order_id = #{orderId} AND order_status IN
    <foreach collection="expectedStatuses" item="s" open="(" separator="," close=")">#{s}</foreach>
</update>
```

`OrderMapper.java` 对应新增两个接口方法。

### 4.2 `OrderServiceImpl.java` — 四个方法改造

#### `payOrder`
```java
@Transactional
public void payOrder(String userId, String orderId) {
    Order order = orderMapper.selectByOrderId(orderId);
    if (order == null || !order.getUserId().equals(userId))
        throw new OrderException("订单不存在");

    int updated = orderMapper.updateOrderStatusCas(orderId, "PAID", "PENDING");
    if (updated <= 0) throw new OrderException("订单状态异常，支付失败");

    log.info("订单支付成功, orderId={}, productId={}, quantity={}",
             orderId, order.getProductId(), order.getQuantity());

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override public void afterCommit() {
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

#### `cancelOrder`
```java
@Transactional
public void cancelOrder(String userId, String orderId) {
    Order order = orderMapper.selectByOrderId(orderId);
    // ...校验归属...

    int updated = orderMapper.updateOrderStatusCasMulti(
        orderId, "CANCELLED", List.of("PENDING", "PAID"));
    if (updated <= 0) {
        log.warn("取消订单失败，状态已变更, orderId={}", orderId);
        throw new OrderException("订单状态已变更，取消失败");
    }

    if ("PAID".equals(order.getOrderStatus())) {
        log.info("已支付订单取消，恢复库存, orderId={}", orderId);
        feignRetry(() -> productFeignClient.restoreStock(
            order.getProductId(), order.getQuantity()));
    } else {
        feignRetry(() -> productFeignClient.releaseReservation(orderId));
    }
}
```

#### `shipOrder`
```java
@Transactional
public void shipOrder(String shopId, String orderId, ShipOrderRequest request) {
    Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
    if (order == null) throw new OrderException("订单不存在或无权限");

    int updated = orderMapper.updateOrderStatusCas(orderId, "SHIPPED", "PAID");
    if (updated <= 0) throw new OrderException("订单状态异常，发货失败");

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override public void afterCommit() {
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

#### `confirmReturn`
```java
@Transactional
public void confirmReturn(String shopId, String orderId) {
    Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
    if (order == null) throw new OrderException("订单不存在或无权限");

    int updated = orderMapper.updateOrderStatusCas(orderId, "RETURNED", "RETURNING");
    if (updated <= 0) throw new OrderException("退货确认失败");

    log.info("退货确认成功, orderId={}", orderId);

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override public void afterCommit() {
                fileFallbackDaemon.sendOrFallback(
                    OrderEventType.STOCK_RESTORE.name(), orderId, null);
            }
        }
    );
}
```

### 4.3 `application.yml` — 无新增配置

Redis Stream 不依赖额外配置项，`spring.redis` 已有的连接配置即可复用。

---

## 5. 汇总：改动文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `OrderEventType.java` | 新增 | 事件类型枚举 |
| `RedisStreamConfig.java` | 新增 | Stream + Group 初始化 |
| `OrderEventConsumer.java` | 新增 | Stream 消息消费 + 幂等处理 |
| `FileFallbackDaemon.java` | 新增 | Redis 不可用时本地文件兜底 + 定时补发 |
| `StreamListenerContainerConfig.java` | 新增 | Consumer 容器配置 |
| `OrderMapper.java` | 修改 | +2 CAS 方法声明 |
| `OrderMapper.xml` | 修改 | +2 CAS SQL |
| `OrderServiceImpl.java` | 修改 | 4 个方法改造 |
| 其他现有文件 | 不动 | Controller、Order.java、Feign 接口等不变 |

---

## 6. 边界情况与防护

| 场景 | 防护措施 |
|------|---------|
| afterCommit 时 Redis 不可用 | `FileFallbackDaemon.sendOrFallback` 写本地 txt |
| JVM 在 afterCommit 前崩溃 | 订单状态已改，Consumer 未收到 → FileFallbackDaemon 启动时补发 |
| Consumer 处理失败 | 不 ack → Pending 列表留存，下次启动先处理 Pending |
| STOCK_CONFIRM 重复消费 | confirmReservation CAS 自然幂等 (RESERVED → CONFIRMED) |
| STOCK_RESTORE 重复消费 | Redis SET 幂等键 (7天 TTL) 做第一道防线 |
| LOGISTICS_CREATE 重复消费 | 消费前查 logistics 是否已创建 |
| pay/cancel 并发 | CAS 互斥，谁先提交谁赢 |
| cancel 并发双恢复 | CAS `WHERE order_status IN ('PENDING','PAID')` + Feign 后校验 |
