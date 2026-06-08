# 订单退货流程设计

> 简化版：单表、无 WebSocket、无复制场景

## 流程

```
User    → 申请退货 (return_reason)            → return_requests(status=applying)，不改订单
Seller  → 审核同意                             → UPDATE status=agreed + CAS 订单→RETURN_PENDING
User    → 填写退货物流 (trackingNumber)        → 创建物流记录 + CAS 订单→RETURNING
Seller  → 确认退货                             → 已有接口 confirmReturn: RETURNING→RETURNED
Seller  → 审核驳回                             → UPDATE status=rejected，不改订单
```

## 数据表

在 `02-order-init.sql` 追加：

```sql
CREATE TABLE IF NOT EXISTS return_requests (
    order_id      VARCHAR(20) PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    shop_id       VARCHAR(32)  NOT NULL,
    return_reason VARCHAR(500) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'applying',
    logistics_id  INT          NULL,
    created_date  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date  TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_return_status CHECK (status IN ('applying','agreed','rejected')),
    INDEX idx_shop_status (shop_id, status),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 文件清单

### 新增文件

| 层 | 文件 |
|----|------|
| Model | `model/ReturnRequest.java` — orderId, userId, shopId, returnReason, status, logisticsId, createdDate, updatedDate |
| DTO | `dto/CreateReturnRequest.java` — `@NotBlank @Size(max=500) String returnReason` |
| DTO | `dto/ReviewReturnRequest.java` — `@NotBlank String status` |
| DTO | `dto/SubmitReturnLogisticsRequest.java` — `@NotBlank String trackingNumber`, `@NotNull Integer contactId` |
| DTO | `dto/ReturnRequestDTO.java` — 镜像 ReturnRequest |
| Mapper | `mapper/ReturnRequestMapper.java` — 注解 SQL，风格同 OrderMapper |
| Service | `service/ReturnRequestService.java` — 接口 |
| Service | `service/impl/ReturnRequestServiceImpl.java` — 实现，含 `@Transactional` |

### 修改文件

| 文件 | 改动 |
|------|------|
| `sql/init/02-order-init.sql` | 追加 return_requests 表 |
| `service/impl/OrderServiceImpl.java` | 替换 `requestReturn` 实现为插 applying 记录 |
| `controller/OrderUserController.java` | 新增 `POST /{orderId}/return-request`(带body)、`POST /{orderId}/return-logistics` |
| `controller/OrderSellerController.java` | 新增 `GET /return-requests/pending`、`GET /return-requests/processed`、`PUT /{orderId}/review` |

## Service 逻辑

### `createReturnRequest(userId, orderId, req)`

1. `selectOrderDetailByUser` — 校验订单存在
2. 只允许 `SHIPPED` / `DELIVERED` 状态
3. 查 `return_requests`，该订单无已有记录
4. 插入 `status=applying`
5. 不改订单状态

### `reviewReturnRequest(shopId, orderId, req)`

1. `req.status` 只允许 `agreed` / `rejected`
2. 按 `orderId + shopId` 查 pending 记录，必须是 `applying`
3. 事务内：
   - `UPDATE return_requests SET status=?`
   - 如果 `agreed` → `updateOrderStatusCasMulti(orderId, RETURN_PENDING, [SHIPPED, DELIVERED])`，失败抛异常回滚
   - 如果 `rejected` → 不改订单
4. CAS 时保留 `created_date`

### `submitReturnLogistics(userId, orderId, req)`

1. 查 `return_requests` 按 `orderId + userId` 记录存在且 `status=agreed`
2. 校验该订单状态为 `RETURN_PENDING`
3. 校验 `logistics_id` 为空（不重复提交）
4. 调 `LogisticsFeignClient.createLogistics(orderId, "RETURN", contactId, trackingNumber)`
5. 校验返回值，失败抛异常
6. 提取 `data.id`
7. 事务内：
   - `UPDATE return_requests SET logistics_id=?`
   - `updateOrderStatusCas(orderId, RETURNING, RETURN_PENDING)`
8. CAS 失败抛异常回滚

## 错误处理

- 所有业务校验失败 → `throw OrderException("描述")` → Spring 事务回滚
- Feign 调用失败 → 检查 `ApiResponse.code != 200` → `throw OrderException("物流创建失败")` → 回滚
- 不引入分布式事务方案，物流侧偶发脏数据可接受

## 不变部分

- `approveReturn` 接口保留不动，新流程不再调用它
- `confirmReturn` 接口不变
- 不引入 WebSocket
- 不考虑一单多商品复制场景
