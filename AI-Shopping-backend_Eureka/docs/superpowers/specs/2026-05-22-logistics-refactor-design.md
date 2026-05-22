# 物流重构设计方案

## 概述

将订单与物流的关系从 `订单.logisticsId → 物流.id` 改为 `物流.orderId → 订单.orderId`，物流新增 `type` 字段区分发货/退货，删除订单侧的 `logistics_id`。

## 数据库变更

### logistics 表（04-logistics-init.sql）

| 变更 | 说明 |
|------|------|
| 新增 `order_id VARCHAR(20) NOT NULL` | 关联订单号 |
| 新增 `type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY'` | DELIVERY-发货, RETURN-退货 |
| `shipping_date` → `created_at` | 改为创建时间 |
| 新增 `INDEX idx_order_type (order_id, type)` | 按订单+类型查询优化 |

```sql
CREATE TABLE IF NOT EXISTS logistics (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '物流ID',
    order_id VARCHAR(20) NOT NULL COMMENT '订单号',
    type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY' COMMENT '类型: DELIVERY-发货, RETURN-退货',
    contact_id INT NOT NULL COMMENT '联系人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    tracking_number VARCHAR(50) NOT NULL COMMENT '快递单号',
    INDEX idx_order_type (order_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流表';
```

### t_order 表（02-order-init.sql）

- 删除 `logistics_id` 列

### deleted_orders 表（02-order-init.sql）

- 删除 `logistics_id` 列

## 物流层变更

### Logistics.java
- 新增 `String orderId`
- 新增 `String type`
- `shippingDate` → `createdAt`

### CreateLogisticsRequest.java
- 新增 `String orderId`
- 新增 `String type`（默认 DELIVERY）
- 保留 `Integer contactId`
- 保留 `String trackingNumber`

### LogisticsResponse.java
- 同步新增 `orderId`、`type`
- `shippingDate` → `createdAt`

### 删除
- `UpdateLogisticsRequest.java`（不再需要更新接口）

### LogisticsServiceImpl.java
- `createLogistics()` 写入 `orderId` 和 `type`
- 新增 `getLogisticsByOrder(String orderId)` → 查某订单所有物流
- 新增 `getLatestLogistic(String orderId, String type)` → 查最新一条指定类型的物流
- 删除按 `id` 查询和更新的方法（内部调用仍保留按 `id` 删除）

## 物流 API

| 方法 | 路径 | 变更 |
|------|------|------|
| `POST` | `/logistics/create` | body 加 `orderId`、`type` |
| `PUT` | `/logistics/update` | **删除** |
| `GET` | `/logistics/get/{id}` | **删除** |
| `GET` | `/logistics/list` | 保留 |
| `GET` | `/logistics/search/tracking` | 保留 |
| `DELETE` | `/logistics/delete/{id}` | 保留 |
| `GET` | `/logistics/order/{orderId}` | **新增** — 查某订单所有物流 |
| `GET` | `/logistics/order/{orderId}/latest?type=DELIVERY\|RETURN` | **新增** — 查最新一条 |

内部（InternalLogisticsController）同步：

| 方法 | 路径 | 变更 |
|------|------|------|
| `POST` | `/internal/logistics/create` | body 同步 |
| `GET` | `/internal/logistics/get/{id}` | **删除** |
| `PUT` | `/internal/logistics/close/{id}` | **删除** |
| `GET` | `/internal/logistics/order/{orderId}` | **新增** |
| `GET` | `/internal/logistics/order/{orderId}/latest?type=` | **新增** |

## 订单层变更

### Order.java
- 删除 `Integer logisticsId`

### OrderDTO.java（common-api）
- 删除 `logisticsId`

### DeletedOrder.java
- 删除 `Integer logisticsId`

### PlaceOrderRequest.java、ShipOrderRequest.java
- 删除 `logisticsId` 相关引用

### OrderServiceImpl.java

**发货流程（shipOrder）：**
1. 验证订单存在 & 状态 = PAID
2. 调用 `logisticsFeignClient.createLogistics()`，传入 `{ orderId, contactId, trackingNumber, type=DELIVERY }`
3. 订单状态改为 SHIPPED
4. 不再回写 `logisticsId`，不再需要补偿逻辑

**查订单物流：**
- 从 `order.logisticsId → GET /logistics/get/{id}` 改为 `order.orderId → GET /internal/logistics/order/{orderId}/latest?type=DELIVERY`

## Common API 变更

### LogisticsFeignClient.java
```java
@FeignClient(name = "logistics-service")
public interface LogisticsFeignClient {
    ApiResponse<Map<String, Object>> createLogistics(LogisticsRequest request);

    ApiResponse<List<Map<String, Object>>> getLogisticsByOrder(@RequestParam("orderId") String orderId);

    ApiResponse<Map<String, Object>> getLatestLogistics(@RequestParam("orderId") String orderId,
                                                        @RequestParam("type") String type);
}
```

### LogisticsRequest.java
```java
public class LogisticsRequest {
    private String orderId;
    private String type;
    private Integer contactId;
    private String trackingNumber;
}
```

## 影响范围

| 模块 | 改动量 | 说明 |
|------|--------|------|
| `sql/init/04-logistics-init.sql` | 小 | 表结构变更 |
| `sql/init/02-order-init.sql` | 小 | 删两列 |
| `logistics-service` | 中 | 实体、DTO、Service、Controller 改动 |
| `order-service` | 中 | 实体、DTO、Service、Controller 改动 |
| `common-api` | 小 | Feign 接口和 DTO 改动 |
| 前端 | **不改** | 用户声明后续大改 |
