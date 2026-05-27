# Order 服务后端修复方案 V2

> 日期: 2026-05-24
>
> **说明**: P0/P1 问题已识别但暂不修复，记录在此供后续研究。
> **本次执行**: P2 + P3 问题。

---

## Part 1 — P0/P1 已知问题记录（待研究）

### P0 — 数据一致性问题

| # | 问题 | 文件 | 描述 |
|---|------|------|------|
| 1 | 退货不恢复库存 | `OrderServiceImpl:200-207` | `confirmReturn()` 只改状态 `RETURNED`，没有调 `restoreStock`。已支付退货后 stock 永久丢失 |
| 2 | payOrder 跨服务不一致 | `OrderServiceImpl:171-185` | `confirmReservation`（扣库存）成功 → `updateOrderStatus` 失败 → stock 已扣但订单仍 PENDING。ReservationCleanupTask 不处理 CONFIRMED 状态的记录 |
| 3 | createOrder 孤儿 reservation | `OrderServiceImpl:40-74` | `reserveStock` 成功但 `@Transactional` 后续回滚 → reservation 残留。现有 ReservationCleanupTask 可兜底（2分钟周期） |
| 4 | shipOrder 孤儿物流记录 | `OrderServiceImpl:120-145` | `createLogistics` 成功但 `updateOrderStatus` 失败 → 物流记录存在但订单仍 PAID |

### P1 — 并发安全问题

| # | 问题 | 文件 | 描述 |
|---|------|------|------|
| 5 | 并发取消重复恢复库存 | `OrderServiceImpl:99-116` | 无乐观锁，两线程同时读到 PAID → 各调一次 `restoreStock` → stock 被加两次 |
| 6 | pay/cancel 竞态 | 同上 + `OrderServiceImpl:171` | pay 和 cancel 同时走，谁能先锁定状态不确定；Feign 调用可能因为 reservation 状态已变而失败 |

> **建议解决方案（后续研究用）**:
> - 所有状态变更方法统一加乐观锁：`UPDATE ... WHERE order_id=? AND order_status=?`
> - 跨服务一致性：方案 A = 事务日志 + 重试（轻量），方案 B = Seata AT `@GlobalTransactional`（需部署 Seata Server）
> - 退货恢复库存：`confirmReturn()` 中调用 `restoreStock`

---

## Part 2 — P2/P3 即刻修复执行计划

---

## P2-1: Redis 配置修正

**文件**: `application.yml`

**问题**: Spring Boot 3.2.3 下 `spring.data.redis` 不生效，需改为 `spring.redis`。

**改动**: `order-service/src/main/resources/application.yml` 第 13 行

```yaml
# 修改前
spring:
  data:
    redis:
      host: localhost
      port: 6379

# 修改后
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
```

**验证**: 启动后检查是否成功连接 Redis，订单 ID 生成 `RedisOrderIdGenerator` 能正常 increment。

---

## P2-2: 硬编码超时配置化

**问题**: `ProductReservationServiceImpl` 中 30 分钟 reservation 过期和 `OrderTimeoutTask` 中 30 分钟订单取消超时均为硬编码，修改一处忘改另一处会导致时间窗口错位。

### Step 1: `application.yml` 增加配置项

```yaml
order:
  timeout:
    payment-minutes: 30
```

### Step 2: 改造 `ProductReservationServiceImpl`

```java
@Service
@RequiredArgsConstructor
public class ProductReservationServiceImpl implements ProductReservationService {

    @Value("${order.timeout.payment-minutes:30}")
    private int paymentTimeoutMinutes;

    @Override
    @Transactional
    public void reserve(String orderId, String productId, int quantity) {
        // ... 现有库存检查不变 ...

        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.MINUTE, paymentTimeoutMinutes);  // ← 改为配置
        Date expiredAt = cal.getTime();

        // ... 后续不变 ...
    }
}
```

### Step 3: 改造 `OrderTimeoutTask`

```java
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    @Value("${order.timeout.payment-minutes:30}")
    private int paymentTimeoutMinutes;

    @Scheduled(fixedRate = 60000)
    public void cancelExpiredOrders() {
        List<Order> expired = orderMapper.selectExpiredPendingOrders(paymentTimeoutMinutes);
        // ... 不变 ...
    }
}
```

**验证**: 改 `application.yml` 中 `order.timeout.payment-minutes` 值，观察两处是否同步生效。

---

## P2-3: `shipOrder` Service 层权限校验

**问题**: `OrderServiceImpl.shipOrder()` 内部不校验 shopId，权限检查仅由 Controller 层调用 `getOrderDetailByShop` 完成（返回值还被丢弃）。任何绕过 Controller 的调用都能跨店铺发货。

### Step 1: 修改接口 `OrderService`

```java
// OrderService.java
// 修改前
void shipOrder(String orderId, ShipOrderRequest request);

// 修改后
void shipOrder(String shopId, String orderId, ShipOrderRequest request);
```

### Step 2: 修改实现 `OrderServiceImpl`

```java
@Override
@Transactional
public void shipOrder(String shopId, String orderId, ShipOrderRequest request) {
    Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
    if (order == null) {
        throw new OrderException("订单不存在或无权限发货");
    }

    order.transitionTo(Order.SHIPPED);

    // SOLID 原则强调: 不允许同一方法中同时出现注释和代码。
    // 注释应解释 WHY，而非 WHAT。
    LogisticsRequest logisticsRequest = new LogisticsRequest();
    logisticsRequest.setOrderId(orderId);
    logisticsRequest.setType("DELIVERY");
    logisticsRequest.setContactId(request.getContactId());
    logisticsRequest.setTrackingNumber(request.getTrackingNumber());

    ApiResponse<Map<String, Object>> logisticsResponse =
            logisticsFeignClient.createLogistics(logisticsRequest);
    if (logisticsResponse == null || logisticsResponse.getData() == null) {
        throw new OrderException("创建物流记录失败");
    }

    int result = orderMapper.updateOrderStatus(orderId, Order.SHIPPED);
    if (result <= 0) {
        throw new OrderException("更新订单状态失败");
    }
}
```

### Step 3: 修改 Controller `OrderSellerController`

```java
@PutMapping("/{orderId}/ship")
public ApiResponse<Void> shipOrder(
        @PathVariable("orderId") String orderId,
        @RequestBody @Valid ShipOrderRequest request,
        @RequestParam("shopId") String shopId) {
    // 权限校验已下沉到 Service, controller 不再需要单独调 getOrderDetailByShop
    orderService.shipOrder(shopId, orderId, request);
    return ApiResponse.success("发货成功", null);
}
```

### Step 4: 修改 Feign 客户端 `OrderFeignClient`

如果 Feign 客户端也调用了 `shipOrder`，需要同步修改：

```java
// 仅当存在这样的调用时才需要修改
```

**验证**: 用不匹配的 `shopId` 调用 `shipOrder`，应返回 "订单不存在或无权限发货"。

---

## P3-1: `new Order().cancelOrder(order)` 反模式修复

**文件**: `OrderServiceImpl.java:106`、`OrderServiceImpl.java:126`、`Order.java:77-83`

**问题**: `new Order().shipOrder(order)` 和 `new Order().cancelOrder(order)` 创建临时对象调用实例方法，但方法内部实际修改的是传入的 `order` 参数。反模式，应直接调用 `order.transitionTo(...)`。

### 删除 `Order.java` 中多余的实例方法

```java
// 删除以下两个方法（第 77-83 行）
// public Order shipOrder(Order order) {
//     return order.transitionTo(SHIPPED);
// }
// public Order cancelOrder(Order order) {
//     return order.transitionTo(CANCELLED);
// }
```

### 修改 `OrderServiceImpl.java` 中调用处

```java
// 第 106 行: 修改前
new Order().cancelOrder(order);
// 修改后
order.transitionTo(Order.CANCELLED);

// 第 126 行: 修改前
new Order().shipOrder(order);
// 修改后
order.transitionTo(Order.SHIPPED);
```

**验证**: 编译通过，下单 → 支付 → 发货 → 取消等流程的状态转换正常。

---

## P3-2: `System.out.println` 改为 Logger

**文件**: `Order.java:73`

```java
// 修改前
System.out.println("订单创建成功时间: " + order.orderDate);

// 修改后
log.info("订单创建成功时间: {}", order.orderDate);
```

在 `Order.java` 中增加 Logger 声明：

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 类中增加
private static final Logger log = LoggerFactory.getLogger(Order.class);
```

---

## P3-3: `canTransition` 空安全加固（可选）

**文件**: `Order.java:49-52`

```java
// 修改前
public boolean canTransition(String fromStatus, String toStatus) {
    if (fromStatus == null) return true;
    return TRANSITIONS.getOrDefault(fromStatus, Set.of()).contains(toStatus);
}

// 修改后
public boolean canTransition(String fromStatus, String toStatus) {
    if (fromStatus == null || toStatus == null) return false;
    return TRANSITIONS.getOrDefault(fromStatus, Set.of()).contains(toStatus);
}
```

> **安全说明**: `buildInitOrder` 始终将 `orderStatus` 初始化为 `PENDING`，不存在 null 状态流转的正常路径。此修改仅防止代码 bug 导致 null 传播时产生意外的状态转换。

---

## 执行顺序

| 顺序 | 任务 | 文件数 | 风险 | 预计时间 |
|------|------|--------|------|---------|
| 1 | P3-1 反模式修复 | 2 | 低 | 2min |
| 2 | P3-2 Logger 替换 | 1 | 低 | 1min |
| 3 | P3-3 空安全加固 | 1 | 低 | 1min |
| 4 | P2-1 Redis 配置 | 1 | 低 | 1min |
| 5 | P2-2 硬编码超时配置化 | 3 | 低 | 5min |
| 6 | P2-3 shipOrder 权限下沉 | 3~4 | 中 | 10min |

总计约 **20 分钟**。

---

## 附录：相关文件完整清单

| 文件 | 作用 |
|------|------|
| `order-service/.../service/OrderService.java` | 接口 |
| `order-service/.../service/impl/OrderServiceImpl.java` | 核心实现 |
| `order-service/.../controller/OrderSellerController.java` | 卖家控制器 |
| `order-service/.../mapper/OrderMapper.java` | 数据访问 |
| `order-service/.../model/Order.java` | 实体 + 状态机 |
| `order-service/.../task/OrderTimeoutTask.java` | 超时任务 |
| `order-service/src/main/resources/application.yml` | 配置 |
| `common-api/.../feign/order/OrderFeignClient.java` | Feign 客户端 |
| `product-service/.../service/impl/ProductReservationServiceImpl.java` | reservation 逻辑 |
