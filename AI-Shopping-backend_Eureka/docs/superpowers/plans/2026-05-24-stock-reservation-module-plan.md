# 库存预占模块实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现下单预占库存、支付确认扣减、取消/超时释放的完整机制

**Architecture:** product-service 新建 `product_reservations` 表 + `ProductReservationService` 独立模块；order-service 3 处调用调整 + 1 个定时任务；common-api 增加 DTO 和 Feign 接口方法

**Tech Stack:** Spring Boot, MyBatis, MySQL, Feign

---

## 文件结构

### 新增文件
| 文件 | 职责 |
|------|------|
| `common-api/.../dto/product/StockReserveRequest.java` | 预占请求 DTO（orderId, productId, quantity） |
| `product-service/.../model/ProductReservation.java` | 预占记录实体 |
| `product-service/.../mapper/ProductReservationMapper.java` | 预占表 + products.stock 的 SQL 操作 |
| `product-service/.../service/ProductReservationService.java` | 预占/确认/释放/清理的业务逻辑 |
| `product-service/.../task/ReservationCleanupTask.java` | 孤儿预占兜底定时任务 |
| `order-service/.../task/OrderTimeoutTask.java` | 超时 PENDING 订单取消定时任务 |

### 修改文件（仅追加，不删不改现有）
| 文件 | 改动 |
|------|------|
| `product-service/.../controller/internal/InternalProductController.java` | +3 内部端点 |
| `common-api/.../feign/product/ProductFeignClient.java` | +3 Feign 方法 |
| `order-service/.../mapper/OrderMapper.java` | +1 查询方法 |
| `order-service/.../service/impl/OrderServiceImpl.java` | 3 处调用调整 |

---

### Task 1: DB 迁移 —— 创建 product_reservations 表

- [ ] **Step 1: 编写 SQL 迁移脚本**

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

放置在 `product-service/src/main/resources/db/migration/` 或直接手动执行，取决于项目使用的 DB 迁移工具。

- [ ] **Step 2: 提交**

```bash
git add -A
git commit -m "feat: add product_reservations table"
```

---

### Task 2: StockReserveRequest DTO

**Create:** `AI-Shopping-backend_Eureka/common-api/src/main/java/com/gzasc/aishopping/common/dto/product/StockReserveRequest.java`

- [ ] **Step 1: 创建 DTO**

```java
package com.gzasc.aishopping.common.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReserveRequest implements Serializable {
    private String orderId;
    private String productId;
    private int quantity;
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl common-api -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "feat: add StockReserveRequest DTO"
```

---

### Task 3: ProductReservation 实体 + Mapper

**Create:** `product-service/src/main/java/com/gzasc/aishopping/product/model/ProductReservation.java`
**Create:** `product-service/src/main/java/com/gzasc/aishopping/product/mapper/ProductReservationMapper.java`

- [ ] **Step 1: 创建实体类 ProductReservation**

```java
package com.gzasc.aishopping.product.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReservation {
    private Long id;
    private String productId;
    private String orderId;
    private int quantity;
    private String status;  // RESERVED / CONFIRMED / RELEASED
    private Date createdAt;
    private Date expiredAt;
}
```

- [ ] **Step 2: 创建枚举常量类（放在 ProductReservation 同一文件或独立类）**

```java
// 在 ProductReservation.java 中追加内部常量
public static final String RESERVED = "RESERVED";
public static final String CONFIRMED = "CONFIRMED";
public static final String RELEASED = "RELEASED";
```

- [ ] **Step 3: 创建 ProductReservationMapper**

```java
package com.gzasc.aishopping.product.mapper;

import com.gzasc.aishopping.product.model.ProductReservation;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

@Mapper
public interface ProductReservationMapper {

    @Insert("INSERT INTO product_reservations (product_id, order_id, quantity, status, created_at, expired_at) " +
            "VALUES (#{productId}, #{orderId}, #{quantity}, #{status}, #{createdAt}, #{expiredAt})")
    int insertReservation(ProductReservation reservation);

    @Select("SELECT * FROM product_reservations WHERE order_id = #{orderId}")
    ProductReservation selectByOrderId(@Param("orderId") String orderId);

    @Update("UPDATE product_reservations SET status = 'CONFIRMED' WHERE order_id = #{orderId} AND status = 'RESERVED'")
    int confirmReservation(@Param("orderId") String orderId);

    @Update("UPDATE product_reservations SET status = 'RELEASED' WHERE order_id = #{orderId} AND status = 'RESERVED'")
    int releaseReservation(@Param("orderId") String orderId);

    @Select("SELECT * FROM product_reservations WHERE status = 'RESERVED' AND expired_at <= #{now}")
    List<ProductReservation> selectExpiredReservations(@Param("now") Date now);

    // 直接操作 products 表
    @Update("UPDATE products SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity}")
    int deductProductStock(@Param("productId") String productId, @Param("quantity") int quantity);

    @Select("SELECT stock FROM products WHERE id = #{productId} FOR UPDATE")
    int selectProductStockForUpdate(@Param("productId") String productId);

    @Select("SELECT COALESCE(SUM(quantity), 0) FROM product_reservations " +
            "WHERE product_id = #{productId} AND status = 'RESERVED' FOR UPDATE")
    int sumReservedQty(@Param("productId") String productId);
}
```

- [ ] **Step 4: 编译验证**

Run: `mvn compile -pl product-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "feat: add ProductReservation model and mapper"
```

---

### Task 4: ProductReservationService

**Create:** `product-service/src/main/java/com/gzasc/aishopping/product/service/ProductReservationService.java`

- [ ] **Step 1: 创建 Service**

```java
package com.gzasc.aishopping.product.service;

import com.gzasc.aishopping.product.mapper.ProductReservationMapper;
import com.gzasc.aishopping.product.model.ProductReservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReservationService {

    private final ProductReservationMapper mapper;

    @Transactional
    public void reserve(String orderId, String productId, int quantity) {
        int stock = mapper.selectProductStockForUpdate(productId);
        int alreadyReserved = mapper.sumReservedQty(productId);
        if (stock - alreadyReserved < quantity) {
            throw new RuntimeException("商品库存不足");
        }

        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.MINUTE, 30);
        Date expiredAt = cal.getTime();

        ProductReservation reservation = new ProductReservation();
        reservation.setProductId(productId);
        reservation.setOrderId(orderId);
        reservation.setQuantity(quantity);
        reservation.setStatus(ProductReservation.RESERVED);
        reservation.setCreatedAt(now);
        reservation.setExpiredAt(expiredAt);

        mapper.insertReservation(reservation);
    }

    @Transactional
    public void confirm(String orderId) {
        ProductReservation reservation = mapper.selectByOrderId(orderId);
        if (reservation == null) {
            throw new RuntimeException("预占记录不存在");
        }
        if (!ProductReservation.RESERVED.equals(reservation.getStatus())) {
            throw new RuntimeException("预占状态已变更，无法确认");
        }

        int rows = mapper.confirmReservation(orderId);
        if (rows <= 0) {
            throw new RuntimeException("确认预占失败");
        }

        rows = mapper.deductProductStock(reservation.getProductId(), reservation.getQuantity());
        if (rows <= 0) {
            // deductProductStock 失败时 @Transactional 会回滚 confirmReservation
            throw new RuntimeException("扣减库存失败");
        }
    }

    @Transactional
    public void release(String orderId) {
        ProductReservation reservation = mapper.selectByOrderId(orderId);
        if (reservation == null) {
            return;  // 不存在则幂等返回
        }
        if (ProductReservation.RELEASED.equals(reservation.getStatus())) {
            return;  // 已释放则幂等返回
        }
        if (!ProductReservation.RESERVED.equals(reservation.getStatus())) {
            throw new RuntimeException("预占状态不允许释放");
        }
        mapper.releaseReservation(orderId);
    }

    @Transactional
    public void releaseExpiredReservations() {
        List<ProductReservation> expiredList = mapper.selectExpiredReservations(new Date());
        for (ProductReservation r : expiredList) {
            release(r.getOrderId());
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl product-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "feat: add ProductReservationService"
```

---

### Task 5: ProductFeignClient 追加 3 个方法

**Modify:** `common-api/src/main/java/com/gzasc/aishopping/common/feign/product/ProductFeignClient.java`

- [ ] **Step 1: 在文件末尾追加方法（不触及现有内容）**

追加在最后一个方法 `deleteProduct` 之后、文件结束之前：

```java
    /**
     * 预占库存
     */
    @PostMapping("/internal/product/reserve-stock")
    Map<String, Object> reserveStock(@RequestBody StockReserveRequest request);

    /**
     * 确认预占（支付时扣库存）
     */
    @PostMapping("/internal/product/confirm-reservation")
    Map<String, Object> confirmReservation(@RequestParam("orderId") String orderId);

    /**
     * 释放预占（取消/超时）
     */
    @PostMapping("/internal/product/release-reservation")
    Map<String, Object> releaseReservation(@RequestParam("orderId") String orderId);
```

需要新增 import：

```java
import com.gzasc.aishopping.common.dto.product.StockReserveRequest;
```

（注意：`StockReserveRequest` 和 `StockDeductRequest` 在同一包下，已有 `import com.gzasc.aishopping.common.dto.product.StockDeductRequest;`）

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl common-api -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "feat: add reservation Feign methods to ProductFeignClient"
```

---

### Task 6: InternalProductController 追加 3 个端点

**Modify:** `product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java`

- [ ] **Step 1: 注入 ProductReservationService**

在 `InternalProductController` 中新增字段：

```java
private final ProductReservationService reservationService;
```

- [ ] **Step 2: 追加 3 个端点（在现有方法之后、文件结束之前）**

```java
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

新增 import：

```java
import com.gzasc.aishopping.common.dto.product.StockReserveRequest;
import com.gzasc.aishopping.product.service.ProductReservationService;
```

- [ ] **Step 3: 编译验证**

Run: `mvn compile -pl product-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "feat: add reservation endpoints to InternalProductController"
```

---

### Task 7: OrderMapper 追加超时查询

**Modify:** `order-service/src/main/java/com/gzasc/aishopping/order/mapper/OrderMapper.java`

- [ ] **Step 1: 追加查询方法**

```java
    @Select("SELECT * FROM t_order WHERE order_status = 'PENDING' AND order_date < NOW() - INTERVAL #{minutes} MINUTE")
    List<Order> selectExpiredPendingOrders(@Param("minutes") int minutes);
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl order-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "feat: add selectExpiredPendingOrders to OrderMapper"
```

---

### Task 8: OrderServiceImpl 3 处调用调整

**Modify:** `order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java`

- [ ] **Step 1: `createOrder` 末尾追加预占调用**

找到 `createOrder` 方法的 `return orderId;` 之前，追加：

```java
        productFeignClient.reserveStock(new StockReserveRequest(orderId, order.getProductId(), request.getQuantity()));
```

注意：此调用在 `@Transactional` 中，远程调用失败会回滚整个事务。

新增 import：

```java
import com.gzasc.aishopping.common.dto.product.StockReserveRequest;
```

- [ ] **Step 2: `payOrder` 修改扣库存逻辑**

将 `payOrder` 方法中的：

```java
        Map<String, Object> result = productFeignClient.deductStock(
                new StockDeductRequest(order.getProductId(), order.getQuantity()));
        Boolean success = (Boolean) result.get("success");
        if (!Boolean.TRUE.equals(success)) {
            throw new OrderException("商品库存不足");
        }
```

改为：

```java
        Map<String, Object> result = productFeignClient.confirmReservation(orderId);
        Boolean success = (Boolean) result.get("success");
        if (!Boolean.TRUE.equals(success)) {
            throw new OrderException((String) result.get("message"));
        }
```

- [ ] **Step 3: `cancelOrder` 中 PENDING 取消时释放预占**

在 `cancelOrder` 方法中找到 `if (Order.PAID.equals(originalStatus))` 块，追加 `else if`：

```java
        String originalStatus = order.getOrderStatus();
        new Order().cancelOrder(order);

        if (Order.PAID.equals(originalStatus)) {
            StockDeductRequest stockReq = new StockDeductRequest(order.getProductId(), order.getQuantity());
            productFeignClient.restoreStock(stockReq);
        } else if (Order.PENDING.equals(originalStatus)) {
            productFeignClient.releaseReservation(orderId);
        }

        orderMapper.updateOrderStatus(orderId, Order.CANCELLED);
```

- [ ] **Step 4: 编译验证**

Run: `mvn compile -pl order-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "feat: integrate reservation into createOrder/payOrder/cancelOrder"
```

---

### Task 9: OrderTimeoutTask 超时取消定时任务

**Create:** `order-service/src/main/java/com/gzasc/aishopping/order/task/OrderTimeoutTask.java`

- [ ] **Step 1: 创建定时任务**

```java
package com.gzasc.aishopping.order.task;

import com.gzasc.aishopping.order.mapper.OrderMapper;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

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

- [ ] **Step 2: 在 `OrderService` 接口中确认 `cancelOrder` 的签名**

```java
void cancelOrder(Long userId, String orderId);
```

已在接口中，无需改动。

- [ ] **Step 3: 启用 Spring 定时任务**

在 `order-service` 的启动类或配置类上加 `@EnableScheduling`。找到主类：

```java
// OrderServiceApplication.java 或类似
@SpringBootApplication
@EnableScheduling  // 追加此行
public class OrderServiceApplication { ... }
```

- [ ] **Step 4: 编译验证**

Run: `mvn compile -pl order-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "feat: add OrderTimeoutTask for auto-cancelling expired PENDING orders"
```

---

### Task 10: ReservationCleanupTask 孤儿预占兜底

**Create:** `product-service/src/main/java/com/gzasc/aishopping/product/task/ReservationCleanupTask.java`

- [ ] **Step 1: 创建定时任务**

```java
package com.gzasc.aishopping.product.task;

import com.gzasc.aishopping.product.service.ProductReservationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReservationCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(ReservationCleanupTask.class);

    private final ProductReservationService reservationService;

    @Scheduled(fixedRate = 120000)
    @Transactional
    public void releaseOrphanedReservations() {
        try {
            reservationService.releaseExpiredReservations();
        } catch (Exception e) {
            log.warn("清理孤儿预占失败", e);
        }
    }
}
```

- [ ] **Step 2: 启用 Spring 定时任务**

在 `product-service` 的启动类上追加 `@EnableScheduling`。

- [ ] **Step 3: 编译验证**

Run: `mvn compile -pl product-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "feat: add ReservationCleanupTask for orphan reservation cleanup"
```

---

## 计划自检

- **Spec 覆盖**: 下单预占 (Task 8.1)、支付确认扣减 (Task 8.2)、取消释放 (Task 8.3)、超时取消 (Task 9)、孤儿清理 (Task 10)、幂等释放 (Task 4 `release()`)、并发控制 `FOR UPDATE` (Task 4 `reserve()`) — 全部覆盖
- **占位符扫描**: 无 TBD/TODO
- **类型一致性**: `StockReserveRequest` 含 `orderId/productId/quantity` 三字段，所有调用处一致；`confirmReservation/releaseReservation` 均为 `@RequestParam("orderId")` 一致
