# 订单服务代码重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复代码审查报告中 6 个技术债务项（#1, #2, #3, #9, #11, #13）

**Architecture:** 本次重构不改变整体架构，仅做局部代码改进：
- DTO 跨模块统一（common-api 作为唯一来源）
- MyBatis 注解复用（@ResultMap）
- Feign 同步调用改为 Stream 异步（与现有 payOrder/shipOrder 模式一致）
- 提取事务事件发布工具类消除重复

**Tech Stack:** Java 17+, Spring Boot, MyBatis, Redis Stream, JUnit 5 + Mockito

---

## Task 依赖关系

```
Task 1 (OrderDTO) → 无依赖
Task 2 (ShipOrderRequest) → 无依赖
Task 3 (DeletedOrderMapper) → 无依赖
Task 4 (定时任务 Profile) → 无依赖
Task 5 (EventPublisher) → 无依赖
Task 6 (cancelOrder 异步化) → 依赖 Task 5
```

> 执行顺序：1 → 2 → 3 → 4 → 5 → 6（不互相阻塞，也可并行执行 1-4）

---

## 文件变更总览

| 文件 | 操作 | 所属 Task |
|---|---|---|
| `common-api/.../dto/order/OrderDTO.java` | 修改：新增 3 状态常量 | 1 |
| `common-api/.../dto/order/ShipOrderRequest.java` | 修改：删除 orderId 字段，增加 validation 注解 | 2 |
| `order-service/.../dto/ShipOrderRequest.java` | 删除（不再需要） | 2 |
| `order-service/.../controller/OrderSellerController.java` | 修改：import 路径变更 | 2 |
| `order-service/.../service/impl/OrderServiceImpl.java` | 修改：import 路径变更 + cancelOrder 异步化 + TransactionSynchronization 替换为 EventPublisher | 2, 5, 6 |
| `order-service/.../mapper/DeletedOrderMapper.java` | 修改：@Results → @ResultMap | 3 |
| `order-service/.../task/OrderTimeoutTask.java` | 修改：添加 @ConditionalOnProperty | 4 |
| `order-service/.../stream/OrderEventType.java` | 修改：新增 RESERVATION_RELEASE | 6 |
| `order-service/.../stream/OrderEventConsumer.java` | 修改：新增 handleReservationRelease | 6 |
| `order-service/.../stream/EventPublisher.java` | 新建：工具类 | 5 |
| 测试文件（5 个，具体见各 Task） | 修改：适配变更 | 各 Task |

---

## Task 1: OrderDTO 补充 3 个缺失状态

**Files:**
- Modify: `common-api/src/main/java/com/gzasc/aishopping/common/dto/order/OrderDTO.java:4-11`

**分析：** `Order.java` 定义了 9 个状态常量，`OrderDTO` 只有 6 个。缺少 `DELETED`、`RETURN_PENDING`、`RETURNING`。chat-service 下游通过 Feign 获取订单时无法识别这三个状态。

- [ ] **Step 1: 修改 OrderDTO.java，新增 3 个状态常量**

在 `public static final String RETURNED = "RETURNED";` 之后添加：

```java
public static final String DELETED = "DELETED";
public static final String RETURN_PENDING = "RETURN_PENDING";
public static final String RETURNING = "RETURNING";
```

- [ ] **Step 2: 验证编译通过**

Run: `cd F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka && mvn compile -pl common-api -am -q`
Expected: BUILD SUCCESS

---

## Task 2: ShipOrderRequest 统一到 common-api

**Files:**
- Modify: `common-api/src/main/java/com/gzasc/aishopping/common/dto/order/ShipOrderRequest.java`
- Delete: `order-service/src/main/java/com/gzasc/aishopping/order/dto/ShipOrderRequest.java`
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderSellerController.java`
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java`
- Modify: `order-service/src/test/java/.../controller/OrderSellerControllerTest.java`
- Modify: `order-service/src/test/java/.../service/OrderServiceImplTest.java`

**分析：** order-service 内部版本有 validation 注解但无 `orderId`，common-api 版本有 `orderId` 但无 validation。实际发货时 `orderId` 通过 URL 路径传入不需在 DTO 中重复。统一到 common-api，保留 validation 注解。

- [ ] **Step 1: 修改 common-api ShipOrderRequest**

删除 `orderId` 字段，增加 `@NotBlank` / `@NotNull` validation 注解（与 order-service 内部版本一致）。

最终代码：

```java
package com.gzasc.aishopping.common.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShipOrderRequest {
    @NotBlank(message = "物流单号不能为空")
    private String trackingNumber;

    @NotNull(message = "联系人ID不能为空")
    private Integer contactId;

    private String shippingDate;
}
```

- [ ] **Step 2: 删除 order-service 内部 ShipOrderRequest**

删除文件 `order-service/src/main/java/com/gzasc/aishopping/order/dto/ShipOrderRequest.java`

- [ ] **Step 3: 修改 OrderSellerController import 路径**

将 `import com.gzasc.aishopping.order.dto.ShipOrderRequest;` 改为 `import com.gzasc.aishopping.common.dto.order.ShipOrderRequest;`

- [ ] **Step 4: 修改 OrderServiceImpl import 路径**

将 `import com.gzasc.aishopping.order.dto.ShipOrderRequest;` 改为 `import com.gzasc.aishopping.common.dto.order.ShipOrderRequest;`

- [ ] **Step 5: 修改测试文件 import 路径**

修改 `OrderSellerControllerTest.java` 和 `OrderServiceImplTest.java` 中的 import 路径。

- [ ] **Step 6: 验证编译通过**

Run: `cd F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka && mvn compile -pl order-service -am -q`
Expected: BUILD SUCCESS

---

## Task 3: DeletedOrderMapper @Results 抽取为 @ResultMap

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/mapper/DeletedOrderMapper.java`

**分析：** 3 个查询方法有完全相同的 11 行 `@Results` 注解，字段变更需同步修改 3 处。

- [ ] **Step 1: 修改 DeletedOrderMapper，抽取 @ResultMap**

保留第一个方法的完整 `@Results` 并添加 `id = "DeletedOrderResultMap"`，其余 2 个方法改为 `@ResultMap("com.gzasc.aishopping.order.mapper.DeletedOrderMapper.DeletedOrderResultMap")`。

最终代码：

```java
@Mapper
public interface DeletedOrderMapper {

    @Insert("INSERT INTO deleted_orders (...) VALUES (...)")
    int insertDeletedOrder(DeletedOrder deletedOrder);

    @Select("SELECT * FROM deleted_orders WHERE id = #{id}")
    @Results(id = "DeletedOrderResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "shopId", column = "shop_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "totalPrice", column = "total_price"),
            @Result(property = "orderStatus", column = "order_status"),
            @Result(property = "orderDate", column = "order_date"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "deletedAt", column = "deleted_at")
    })
    DeletedOrder selectDeletedOrderById(Integer id);

    @Select("SELECT * FROM deleted_orders ORDER BY deleted_at DESC")
    @ResultMap("com.gzasc.aishopping.order.mapper.DeletedOrderMapper.DeletedOrderResultMap")
    List<DeletedOrder> selectAllDeletedOrders();

    @Select("SELECT * FROM deleted_orders WHERE order_id = #{orderId}")
    @ResultMap("com.gzasc.aishopping.order.mapper.DeletedOrderMapper.DeletedOrderResultMap")
    DeletedOrder selectDeletedOrderByOrderId(String orderId);
}
```

- [ ] **Step 2: 运行测试验证**

Run: `cd F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka && mvn test -pl order-service -Dtest=DeletedOrderMapperTest -am -q`
Expected: Tests PASS

---

## Task 4: 定时任务添加 Profile 开关

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/task/OrderTimeoutTask.java:8`
- No test changes needed（测试默认 profile 不影响，且 task 已有 `@ConditionalOnProperty` 后自动关闭）

**分析：** `@Scheduled(fixedRate = 60000)` 定时任务在测试运行时可能触发 Feign 调用到不存在的服务。

- [ ] **Step 1: 在 OrderTimeoutTask 类上添加 @ConditionalOnProperty**

```java
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// ...
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "order.task.timeout.enabled", havingValue = "true", matchIfMissing = true)
public class OrderTimeoutTask {
```

`matchIfMissing = true` 确保生产环境不受影响（未配置时默认启用）。

- [ ] **Step 2: 在 test application.yml 中关闭**

在 `order-service/src/test/resources/application.yml` 末尾添加：

```yaml
order:
  task:
    timeout:
      enabled: false
```

---

## Task 5: 抽取 EventPublisher 工具类消除 TransactionSynchronization 重复

**Files:**
- Create: `order-service/src/main/java/com/gzasc/aishopping/order/stream/EventPublisher.java`
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java`（3 处注册替换）

**分析：** `shipOrder()`、`payOrder()`、`confirmReturn()` 三处都在 `@Transactional` 方法中注册匿名 `TransactionSynchronization`，`afterCommit` 回调调用 `fileFallbackDaemon.sendOrFallback()`。提取为 `EventPublisher` 后每处只需一行调用。

- [ ] **Step 1: 创建 EventPublisher.java**

```java
package com.gzasc.aishopping.order.stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

/**
 * 事务事件发布工具类。
 * 在 @Transactional 方法中调用，将事件发送注册到事务提交后的回调中。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final FileFallbackDaemon fileFallbackDaemon;

    /**
     * 在当前事务提交后发送事件到 Redis Stream。
     * 若当前无事务则直接发送。
     *
     * @param eventType 事件类型
     * @param orderId   订单 ID
     * @param extra     额外参数（可为 null）
     */
    public void publishAfterCommit(String eventType, String orderId, Map<String, String> extra) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            fileFallbackDaemon.sendOrFallback(eventType, orderId, extra);
                        }
                    }
            );
        } else {
            // 无事务时直接发送（防御性兜底）
            fileFallbackDaemon.sendOrFallback(eventType, orderId, extra);
        }
    }
}
```

- [ ] **Step 2: 在 OrderServiceImpl 中注入 EventPublisher**

在 `OrderServiceImpl` 的字段区添加：

```java
private final EventPublisher eventPublisher;
```

并在 `@RequiredArgsConstructor` 构造器中增加 `EventPublisher` 参数（Lombok 自动处理）。

同时删除 `import org.springframework.transaction.support.TransactionSynchronization;` 和 `import org.springframework.transaction.support.TransactionSynchronizationManager;`（如果不再被其他代码使用）。

- [ ] **Step 3: 替换 shipOrder 中的注册代码**

原代码（~12 行）：
```java
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
```

替换为一行：
```java
eventPublisher.publishAfterCommit(OrderEventType.LOGISTICS_CREATE.name(), orderId,
        Map.of("contactId", String.valueOf(request.getContactId()),
                "trackingNumber", request.getTrackingNumber()));
```

- [ ] **Step 4: 替换 payOrder 中的注册代码**

原代码替换为：
```java
eventPublisher.publishAfterCommit(OrderEventType.STOCK_CONFIRM.name(), orderId,
        Map.of("productId", order.getProductId(),
                "quantity", String.valueOf(order.getQuantity())));
```

- [ ] **Step 5: 替换 confirmReturn 中的注册代码**

原代码替换为：
```java
eventPublisher.publishAfterCommit(OrderEventType.STOCK_RESTORE.name(), orderId, null);
```

- [ ] **Step 6: 更新 OrderServiceImplTest 构造器**

`OrderServiceImplTest.setUp()` 中创建 `OrderServiceImpl` 时增加 `eventPublisher` 参数：

```java
@Mock
private EventPublisher eventPublisher;

// 在 setUp 中：
orderService = new OrderServiceImpl(orderMapper, deletedOrderMapper, orderIdSelector,
        productFeignClient, logisticsFeignClient, contactFeignClient, shopFeignClient,
        orderConverter, fileFallbackDaemon, eventPublisher);
```

- [ ] **Step 7: 运行测试验证**

Run: `cd F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka && mvn test -pl order-service -am -q`
Expected: 原有 176 个 @Test 全部通过

---

## Task 6: cancelOrder 改为异步 Stream 模式（最复杂）

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/stream/OrderEventType.java`
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/stream/OrderEventConsumer.java`
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java`
- Modify: `order-service/src/test/java/.../service/OrderServiceImplTest.java`
- Modify: `order-service/src/test/java/.../stream/OrderEventConsumerTest.java`

**分析：** 当前 `cancelOrder` 在 `@Transactional` 内同步调用 Feign（`restoreStock` / `releaseReservation`），与 `payOrder`/`shipOrder` 的异步模式不一致。改为事务提交后发送事件，由 `OrderEventConsumer` 异步处理。

- [ ] **Step 1: OrderEventType 新增 RESERVATION_RELEASE**

```java
public enum OrderEventType {
    STOCK_CONFIRM,
    STOCK_RESTORE,
    LOGISTICS_CREATE,
    RESERVATION_RELEASE  // 新增：取消未支付订单时释放预占库存
}
```

- [ ] **Step 2: OrderEventConsumer 新增 handleReservationRelease**

在 `onMessage` 的 switch 中添加 `case RESERVATION_RELEASE -> handleReservationRelease(msg);`

新增方法：
```java
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
```

- [ ] **Step 3: 修改 OrderServiceImpl.cancelOrder**

```java
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
        eventPublisher.publishAfterCommit(OrderEventType.STOCK_RESTORE.name(), orderId,
                Map.of("productId", order.getProductId(),
                        "quantity", String.valueOf(order.getQuantity())));
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
```

关键变更：
- 删除 `productFeignClient.restoreStock(stockReq)` 和 `productFeignClient.releaseReservation(orderId)` 的直接调用
- 改为通过 `eventPublisher.publishAfterCommit(...)` 发布事件
- 注意 `STOCK_RESTORE` 需要传入 `productId` 和 `quantity`（现有 consumer 的 `handleStockRestore` 从数据库查询这些信息，所以这里只需要 `orderId`）

**修正：** 查看现有 `handleStockRestore` 实现，它从 `orderMapper.selectOrderById(orderId)` 获取 `productId` 和 `quantity`，所以 publish 时不需要 extra 参数：

```java
eventPublisher.publishAfterCommit(OrderEventType.STOCK_RESTORE.name(), orderId, null);
```

- [ ] **Step 4: 更新 cancelOrder 相关测试**

在 `OrderServiceImplTest` 中，修改 cancelOrder 测试：

对于 `OR-014 取消 PENDING 订单` 和 `OR-015 取消 PAID 订单`：
- 不再验证 `productFeignClient.restoreStock()` / `releaseReservation()` 被直接调用
- 改为验证 `eventPublisher.publishAfterCommit()` 被调用且参数正确

示例（OR-015）：
```java
@Test
@DisplayName("OR-015 取消 PAID 订单 - 异步恢复库存")
void cancelOrder_paid() {
    Order order = createOrder("ORDER001", 100L, "SHOP001", "PAID");
    when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
    when(orderMapper.updateOrderStatusCas("ORDER001", "CANCELLED", "PAID")).thenReturn(1);

    orderService.cancelOrder(100L, "ORDER001");

    verify(orderMapper).updateOrderStatusCas("ORDER001", "CANCELLED", "PAID");
    verify(eventPublisher).publishAfterCommit(eq("STOCK_RESTORE"), eq("ORDER001"), isNull());
    verify(productFeignClient, never()).restoreStock(any());
}
```

示例（OR-014）：
```java
@Test
@DisplayName("OR-014 取消 PENDING 订单 - 异步释放预占")
void cancelOrder_pending() {
    Order order = createOrder("ORDER001", 100L, "SHOP001", "PENDING");
    when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
    when(orderMapper.updateOrderStatusCas("ORDER001", "CANCELLED", "PAID")).thenReturn(0);
    when(orderMapper.updateOrderStatusCas("ORDER001", "CANCELLED", "PENDING")).thenReturn(1);

    orderService.cancelOrder(100L, "ORDER001");

    verify(eventPublisher).publishAfterCommit(eq("RESERVATION_RELEASE"), eq("ORDER001"), isNull());
    verify(productFeignClient, never()).releaseReservation(any());
}
```

- [ ] **Step 5: 更新 OrderEventConsumerTest**

在 `OrderEventConsumerTest` 中增加 `RESERVATION_RELEASE` 事件类型的测试：

```java
@Test
@DisplayName("OR-080 消费 RESERVATION_RELEASE - 释放预占成功")
void handleReservationRelease_success() {
    Order order = createOrder("ORDER001", "PENDING");
    when(orderMapper.selectOrderById("ORDER001")).thenReturn(order);
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    when(productFeignClient.releaseReservation("ORDER001")).thenReturn(ApiResponse.success(null));

    Map<String, String> msg = new java.util.HashMap<>();
    msg.put("eventType", "RESERVATION_RELEASE");
    msg.put("orderId", "ORDER001");
    consumer.onMessage(createRecord(msg));

    verify(productFeignClient).releaseReservation("ORDER001");
}

@Test
@DisplayName("OR-081 消费 RESERVATION_RELEASE - 幂等跳过")
void handleReservationRelease_idempotent() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

    Map<String, String> msg = new java.util.HashMap<>();
    msg.put("eventType", "RESERVATION_RELEASE");
    msg.put("orderId", "ORDER001");
    consumer.onMessage(createRecord(msg));

    verify(productFeignClient, never()).releaseReservation(anyString());
}
```

- [ ] **Step 6: 运行全部测试验证**

Run: `cd F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka && mvn test -pl order-service -am -q`
Expected: 全部测试通过

---

## Task 7: FileFallbackDaemon.FALLBACK_DIR 改为可配置

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/stream/FileFallbackDaemon.java`
- Modify: `order-service/src/main/resources/application.yml`
- Modify: `order-service/src/test/resources/application.yml`
- Modify: `order-service/src/test/java/.../stream/FileFallbackDaemonTest.java`

**分析：** 当前 `FALLBACK_DIR = Paths.get("data/failover")` 硬编码且为相对路径，不便于在不同环境（开发/测试/生产）使用不同目录，也不便于后续接入配置中心。

- [ ] **Step 1: 修改 FileFallbackDaemon，@Value 注入目录路径**

```java
@Value("${order.fallback.dir:data/failover}")
private String fallbackDirPath;

private Path fallbackDir;

@PostConstruct
public void init() throws IOException {
    this.fallbackDir = Paths.get(fallbackDirPath).toAbsolutePath();
    Files.createDirectories(fallbackDir);
    retryFailed();
}
```

同时私有方法中所有 `FALLBACK_DIR` 引用改为 `fallbackDir`。删除原来的 `private static final Path FALLBACK_DIR = Paths.get("data/failover");`

- [ ] **Step 2: 在 application.yml 中添加默认配置**

在生产配置 `order-service/src/main/resources/application.yml` 末尾添加（默认值已在代码中，但显式写出来更清晰）：

```yaml
order:
  fallback:
    dir: ${user.dir}/data/failover
```

- [ ] **Step 3: 在 test application.yml 中设置测试专用路径**

在 `order-service/src/test/resources/application.yml` 末尾添加：

```yaml
order:
  fallback:
    dir: ${java.io.tmpdir}/order-fallback-test
```

- [ ] **Step 4: 更新 FileFallbackDaemonTest**

测试类中需要将 `@Value` 字段注入，但单元测试用 `@ExtendWith(MockitoExtension.class)` 不加载 Spring 上下文。解决方案：通过 `ReflectionTestUtils` 设置私有字段。

在 `FileFallbackDaemonTest.setUp()` 中添加：

```java
import org.springframework.test.util.ReflectionTestUtils;

// 在 setUp 中创建 daemon 后：
ReflectionTestUtils.setField(daemon, "fallbackDirPath", TEST_FALLBACK_DIR.toString());
// 手动执行 init
daemon.init();
```

注意：`daemon.init()` 会调用 `Files.createDirectories` 和 `retryFailed()`，确保测试目录准备就绪。

- [ ] **Step 5: 运行测试验证**

Run: `cd F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka && mvn test -pl order-service -Dtest=FileFallbackDaemonTest -am -q`
Expected: Tests PASS

---

## Task 8: RedisStreamConfig 改为可配置

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/stream/RedisStreamConfig.java`
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/stream/OrderEventConsumer.java`
- Modify: `order-service/src/main/resources/application.yml`
- Modify: `order-service/src/test/resources/application.yml`
- No test changes needed（OrderEventConsumer 通过 RedisTemplate 操作 Stream，key 和 group 由 config 管理）

**分析：** `STREAM_KEY` 和 `GROUP_NAME` 是 `static final` 常量，硬编码在代码中。后续接入配置中心（如 Nacos / Apollo）时需要将这些值改为可动态注入。

- [ ] **Step 1: 修改 RedisStreamConfig，改为 @Value 注入**

```java
package com.gzasc.aishopping.order.stream;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisStreamConfig {

    private final RedisTemplate<String, String> redisTemplate;

    @Getter
    @Value("${order.stream.key:order:events}")
    private String streamKey;

    @Getter
    @Value("${order.stream.group:order:processors}")
    private String groupName;

    @PostConstruct
    public void init() {
        try {
            final String sk = streamKey;
            final String gn = groupName;
            redisTemplate.execute((RedisCallback<String>) conn -> {
                conn.xGroupCreate(
                        redisTemplate.getStringSerializer().serialize(sk),
                        gn,
                        ReadOffset.latest(),
                        true
                );
                return null;
            });
        } catch (Exception e) {
            log.info("Redis Stream group already exists, skip creation");
        }
    }
}
```

- [ ] **Step 2: 修改 OrderEventConsumer 中引用 STREAM_KEY / GROUP_NAME 的方式**

当前 OrderEventConsumer 中通过 `RedisStreamConfig.STREAM_KEY` 和 `RedisStreamConfig.GROUP_NAME` 引用。

将静态引用改为通过构造器注入 `RedisStreamConfig`：

```java
// 在 OrderEventConsumer 中新增字段
private final RedisStreamConfig redisStreamConfig;

// 替换原有引用
// 原：RedisStreamConfig.STREAM_KEY → redisStreamConfig.getStreamKey()
// 原：RedisStreamConfig.GROUP_NAME → redisStreamConfig.getGroupName()
```

Lombok `@RequiredArgsConstructor` 在 `OrderEventConsumer` 中自动包含 `RedisStreamConfig`（需要在字段上声明 `private final`）。

- [ ] **Step 3: 在 application.yml 中添加默认配置**

生产配置 `application.yml`：
```yaml
order:
  stream:
    key: order:events
    group: order:processors
```

测试配置 `src/test/resources/application.yml`：
```yaml
order:
  stream:
    key: order:events
    group: order:processors
```

- [ ] **Step 4: 更新 OrderEventConsumerTest 构造器**

在测试类中为 `OrderEventConsumer` 构造器增加 `redisStreamConfig` 参数（Mock）：

```java
@Mock
private RedisStreamConfig redisStreamConfig;

// 在 setUp 中：
when(redisStreamConfig.getStreamKey()).thenReturn("order:events");
when(redisStreamConfig.getGroupName()).thenReturn("order:processors");

consumer = new OrderEventConsumer(orderMapper, productFeignClient,
        logisticsFeignClient, redisTemplate, redisStreamConfig);
```

- [ ] **Step 5: 运行测试验证**

Run: `cd F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka && mvn test -pl order-service -am -q`
Expected: 全部测试通过
