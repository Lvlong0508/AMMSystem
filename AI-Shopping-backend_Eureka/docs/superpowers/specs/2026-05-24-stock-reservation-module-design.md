# 库存预占模块设计

## 问题

当前下单不锁库存，支付时才扣减，导致下单到支付之间产生超卖：同一商品可被多个用户下单，支付时库存不足。

## 设计原则

- **开闭原则**：不修改现有 `ProductMapper`、`ProductService`、`products` 表
- **单一职责**：预占逻辑独立为单独模块
- 所有库存原子操作由新模块的 mapper 自行处理

## 新表

```sql
CREATE TABLE product_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(50) NOT NULL UNIQUE,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    created_at DATETIME NOT NULL DEFAULT NOW(),
    expired_at DATETIME NOT NULL,
    INDEX idx_product_status (product_id, status),
    INDEX idx_expired (status, expired_at)
);
```

## 状态机

```
RESERVED ──→ CONFIRMED（支付成功）
RESERVED ──→ RELEASED（取消/超时）
```

## 新增文件

```
product-service/src/main/java/com/gzasc/aishopping/product/
├── model/ProductReservation.java
├── mapper/ProductReservationMapper.java
└── service/ProductReservationService.java
```

### ProductReservation.java

实体类，字段对应新表，含枚举 `ReservationStatus { RESERVED, CONFIRMED, RELEASED }`。

### ProductReservationMapper.java

| 方法 | SQL |
|------|-----|
| `insertReservation(reservation)` | `INSERT INTO product_reservations ...` |
| `selectByOrderId(orderId)` | `SELECT * FROM product_reservations WHERE order_id = #{orderId}` |
| `confirmReservation(orderId)` | `UPDATE product_reservations SET status = 'CONFIRMED' WHERE order_id = #{orderId} AND status = 'RESERVED'` |
| `releaseReservation(orderId)` | `UPDATE product_reservations SET status = 'RELEASED' WHERE order_id = #{orderId} AND status = 'RESERVED'` |
| `selectExpiredReservations(now)` | `SELECT * FROM product_reservations WHERE status = 'RESERVED' AND expired_at <= #{now}` |
| `deductProductStock(productId, qty)` | `UPDATE products SET stock = stock - #{qty} WHERE id = #{productId} AND stock >= #{qty}` |
| `selectProductStockForUpdate(productId)` | `SELECT stock FROM products WHERE id = #{productId} FOR UPDATE` |
| `sumReservedQty(productId)` | `SELECT COALESCE(SUM(quantity), 0) FROM product_reservations WHERE product_id = #{productId} AND status = 'RESERVED' FOR UPDATE` |

最后两条直接写 `products` 表，不经过 `ProductMapper`，实现零耦合。

### ProductReservationService.java

```java
@Service
@RequiredArgsConstructor
public class ProductReservationService {

    @Transactional
    void reserve(String orderId, String productId, int quantity);
        // 1. SELECT stock FROM products WHERE id = #{productId} FOR UPDATE
        // 2. SELECT COALESCE(SUM(quantity), 0) FROM product_reservations
        //    WHERE product_id = #{productId} AND status = 'RESERVED' FOR UPDATE
        // 3. 校验 stock - alreadyReserved >= quantity，不满足则抛异常
        // 4. INSERT INTO product_reservations (..., expired_at = NOW() + 30min)

    @Transactional
    void confirm(String orderId);
        // 1. selectByOrderId(orderId) → 获取 productId, quantity
        // 2. 校验 status = RESERVED，否则抛异常
        // 3. confirmReservation(orderId) → 检查影响行数，0则抛异常
        // 4. deductProductStock(productId, quantity) → 检查影响行数，0则抛异常

    @Transactional
    void release(String orderId);
        // 1. selectByOrderId(orderId) → 获取 reservation
        // 2. 如果记录不存在或已是 RELEASED，直接返回（幂等）
        // 3. 校验 status = RESERVED，否则抛异常
        // 4. releaseReservation(orderId) → 状态变 RELEASED
        // （stock 在 reserve 时未动，release 只需改状态）

    @Transactional
    void releaseExpiredReservations();
        // selectExpiredReservations(NOW()) → 逐条 release
}
```

每个方法内部带 `@Transactional`，通过 `SELECT ... FOR UPDATE` 保证并发安全。

### 新 DTO

```
common-api/.../common/dto/product/StockReserveRequest.java
```

```java
@Data @NoArgsConstructor @AllArgsConstructor
public class StockReserveRequest implements Serializable {
    private String orderId;
    private String productId;
    private int quantity;
}
```

## 现有代码改动

### OrderServiceImpl

| 方法 | 改动 |
|------|------|
| `createOrder` | 最后 +1 行：`productFeignClient.reserveStock(new StockReserveRequest(orderId, productId, quantity))` |
| `payOrder` | 去掉原有的 `deductStock`，改为 `productFeignClient.confirmReservation(orderId)` |
| `cancelOrder` | PENDING 取消时 +1 行：`productFeignClient.releaseReservation(orderId)`（PAID/已确认取消仍走现有 `productFeignClient.restoreStock`，与新模块无关） |

### OrderMapper

新增方法：

```java
List<Order> selectExpiredPendingOrders(@Param("minutes") int minutes);
```

对应 SQL 在 `OrderMapper.xml`（或用 `@Select` 注解）中追加。

### InternalProductController

在现有 controller 中追加 3 个端点（不删不改现有方法）：

```java
// 已有: 不动
@PostMapping("/deduct-stock")  public Map<String, Object> deductStock(...)
@PostMapping("/restore-stock") public Map<String, Object> restoreStock(...)

// 新增
@PostMapping("/reserve-stock")
public Map<String, Object> reserveStock(@RequestBody StockReserveRequest req) {
    try {
        reservationService.reserve(req.getOrderId(), req.getProductId(), req.getQuantity());
        return Map.of("success", true, "message", "预占成功");
    } catch (Exception e) {
        return Map.of("success", false, "message", e.getMessage());
    }
}

@PostMapping("/confirm-reservation")
public Map<String, Object> confirmReservation(@RequestParam String orderId) {
    try {
        reservationService.confirm(orderId);
        return Map.of("success", true, "message", "确认成功");
    } catch (Exception e) {
        return Map.of("success", false, "message", e.getMessage());
    }
}

@PostMapping("/release-reservation")
public Map<String, Object> releaseReservation(@RequestParam String orderId) {
    try {
        reservationService.release(orderId);
        return Map.of("success", true, "message", "释放成功");
    } catch (Exception e) {
        return Map.of("success", false, "message", e.getMessage());
    }
}
```

### ProductFeignClient

在现有 `ProductFeignClient` 中追加 3 个方法（只追加不修改现有方法）：

```java
// common-api/.../feign/product/ProductFeignClient.java

// 已有方法不动 ...

// 新增 ↓
@PostMapping("/reserve-stock")
Map<String, Object> reserveStock(@RequestBody StockReserveRequest request);

@PostMapping("/confirm-reservation")
Map<String, Object> confirmReservation(@RequestParam("orderId") String orderId);

@PostMapping("/release-reservation")
Map<String, Object> releaseReservation(@RequestParam("orderId") String orderId);
```

### 定时任务（order-service）

新增 `OrderMapper.selectExpiredPendingOrders(minutes)`：

```sql
SELECT * FROM orders WHERE order_status = 'PENDING' AND created_at < NOW() - INTERVAL #{minutes} MINUTE
```

订单超时释放逻辑：

```java
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @Scheduled(fixedRate = 60000)
    public void cancelExpiredOrders() {
        List<Order> expired = orderMapper.selectExpiredPendingOrders(30);
        for (Order order : expired) {
            try {
                orderService.cancelOrder(order.getUserId(), order.getOrderId());
            } catch (Exception e) {
                log.warn("系统取消订单失败: {}", order.getOrderId(), e);
            }
        }
    }
}
```

### 定时任务（product-service）—— 孤儿预占兜底

网络超时场景：order-service 调用 `reserveStock` 成功但响应丢失 → 预占已创建但订单回滚。
此任务扫描过期 RESERVED 记录并释放，作为兜底清理。

```java
@Component
@RequiredArgsConstructor
public class ReservationCleanupTask {

    private final ProductReservationService reservationService;

    @Scheduled(fixedRate = 120000)  // 每 2 分钟
    @Transactional
    public void releaseOrphanedReservations() {
        reservationService.releaseExpiredReservations();
    }
}
```

## 调用关系

```
createOrder ──→ reservationService.reserve() ──→ Mapper.insertReservation()
                                                     ↓
payOrder    ──→ reservationService.confirm() ──→ Mapper.confirmReservation()
                                                ──→ Mapper.deductProductStock()

cancelOrder ──→ reservationService.release() ──→ Mapper.releaseReservation()

timeout     ──→ reservationService.release() ──→ Mapper.releaseReservation()
```

## 边界情况

| 场景 | 行为 |
|------|------|
| 下单时库存不足 | `reserve()` 校验 `stock - alreadyReserved < quantity` → 抛异常回滚 |
| 支付时预占已过期 | `confirm()` 中 status 非 RESERVED → 抛异常提示重新下单 |
| 取消已支付的订单 | 不走预占释放，走现有 `restoreStock` 流程 |
| 重复释放 | `release()` 服务层先查记录，已释放/不存在则直接返回，幂等安全 |
| 并发下单同商品 | `SELECT FOR UPDATE` 行锁排队 |
