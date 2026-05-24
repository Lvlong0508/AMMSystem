# Order Service 重构设计文档

## 概述

对 order-service 进行全面重构，解决以下问题：

1. **订单-店铺关联数据分离**：`order_shops` 表在 shop-service 中，下单时通过 Feign 调用 shop-service 关联，但对应端点缺失导致 404
2. **用户订单关联冗余**：`t_user_order` 关联表可以通过 `t_order.user_id` 替代
3. **无分类查询**：存在 `getAllOrders()` 等无身份过滤的接口
4. **响应格式不统一**：手写 `Map.of()` + try-catch，没有统一的响应格式和异常处理

## 核心变更

### 1. 数据库变更

#### 1.1 t_order 表结构调整

```sql
CREATE TABLE IF NOT EXISTS t_order (
    order_id     VARCHAR(20) PRIMARY KEY COMMENT '订单ID',
    user_id      BIGINT       NOT NULL COMMENT '用户ID(Snowflake)',
    shop_id      VARCHAR(32)  NOT NULL COMMENT '店铺ID',
    product_id   VARCHAR(64)  NOT NULL COMMENT '商品ID',
    quantity     INT          NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_price  DECIMAL(10,2)NOT NULL COMMENT '订单总价',
    order_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '订单状态',
    order_date   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    contact_id   INT          COMMENT '联系人ID',
    INDEX idx_user_id (user_id),
    INDEX idx_shop_id (shop_id),
    INDEX idx_status (order_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';
```

- 新增 `user_id BIGINT`（Snowflake 算法生成，对应 auth 服务的 User.id）
- 新增 `shop_id VARCHAR(32)`（对应 shop 服务的 Shop.id）
- 删除 `t_user_order` 表（不再需要关联表）
- 字段按逻辑分组排列

#### 1.2 deleted_orders 表同步新增字段

```sql
CREATE TABLE IF NOT EXISTS deleted_orders (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    order_id     VARCHAR(20)  NOT NULL COMMENT '订单ID',
    user_id      BIGINT       COMMENT '用户ID',
    shop_id      VARCHAR(32)  COMMENT '店铺ID',
    product_id   VARCHAR(64)  NOT NULL COMMENT '商品ID',
    quantity     INT          NOT NULL DEFAULT 1,
    total_price  DECIMAL(10,2)NOT NULL,
    order_status VARCHAR(20)  NOT NULL,
    order_date   TIMESTAMP    NOT NULL,
    contact_id   INT,
    deleted_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已删除订单备份表';
```

### 2. Model 层

#### 2.1 Order.java

新增字段：
```java
private Long userId;      // 用户ID (Snowflake)
private String shopId;    // 店铺ID
```

`buildInitOrder` 方法签名调整：
```java
public Order buildInitOrder(String orderId, Long userId, String shopId,
                            String productId, int quantity, double totalPrice)
```

#### 2.2 DeletedOrder.java

同步新增字段：
```java
private Long userId;
private String shopId;
```

`fromOrder()` 同步复制用户和店铺 ID。

#### 2.3 删除

- `UserOrder.java` — 不再需要

### 3. DTO 层

#### 3.1 OrderAbstractUserDTO — 用户端列表

```java
public class OrderAbstractUserDTO {
    private String orderId;
    private String productId;    // 前端展示商品信息
    private String shopId;       // 前端展示店铺信息
    private BigDecimal totalPrice;
    private int quantity;
    private String orderStatus;
}
```

#### 3.2 OrderAbstractSellerDTO — 商家端列表

```java
public class OrderAbstractSellerDTO {
    private String orderId;
    private String productId;
    private int contactId;       // 发货需要联系人地址
    private int quantity;
    private String orderStatus;
}
```

#### 3.3 OrderDetailDTO — 详情（双端通用）

```java
public class OrderDetailDTO {
    // 全部订单字段
    private String orderId;
    private Long userId;
    private String shopId;
    private String productId;
    private int quantity;
    private BigDecimal totalPrice;
    private String orderStatus;
    private Timestamp orderDate;
    private int contactId;
    // 外部关联数据（Feign 组装）
    private String contactName;
    private String contactPhone;
    private String contactAddress;
    private String trackingNumber;
    private String logisticsStatus;
}
```

### 4. Converter 层（参考 auth-service 模式）

新增 `OrderConverter`：

```java
@Component
public class OrderConverter {
    public OrderAbstractUserDTO toUserAbstractDTO(Order order);
    public List<OrderAbstractUserDTO> toUserAbstractDTOList(List<Order> orders);
    
    public OrderAbstractSellerDTO toSellerAbstractDTO(Order order);
    public List<OrderAbstractSellerDTO> toSellerAbstractDTOList(List<Order> orders);
    
    public OrderDetailDTO toDetailDTO(Order order, ContactDTO contact, Map<String,Object> logistics);
}
```

### 5. Mapper 层

#### 5.1 OrderMapper 变更

删除：
- `selectAllOrders()` — 不支持无用户/店铺过滤
- `selectOrdersByStatus(String)` — 不支持无用户/店铺过滤

新增（两套查询路径，类似 product-service）：

```java
// 用户端抽象查询（跳过 contact_id）
@Select("SELECT order_id, user_id, shop_id, product_id, quantity, total_price, order_status, order_date FROM t_order WHERE user_id = #{userId}")
List<Order> selectAbstractOrdersByUserId(@Param("userId") Long userId);

// 商家端抽象查询（无 total_price）
@Select("SELECT order_id, shop_id, product_id, contact_id, quantity, order_status, order_date FROM t_order WHERE shop_id = #{shopId}")
List<Order> selectAbstractOrdersByShopId(@Param("shopId") String shopId);

// 用户端详情查询（全字段 + 用户鉴权）
@Select("SELECT * FROM t_order WHERE user_id = #{userId} AND order_id = #{orderId}")
Order selectOrderDetailByUser(@Param("userId") Long userId, @Param("orderId") String orderId);

// 商家端详情查询（全字段 + 店铺鉴权）
@Select("SELECT * FROM t_order WHERE shop_id = #{shopId} AND order_id = #{orderId}")
Order selectOrderDetailByShop(@Param("shopId") String shopId, @Param("orderId") String orderId);

// 按状态筛选（限定在用户范围内）
@Select("SELECT order_id, user_id, shop_id, product_id, quantity, total_price, order_status, order_date FROM t_order WHERE user_id = #{userId} AND order_status = #{status}")
List<Order> selectAbstractOrdersByUserAndStatus(@Param("userId") Long userId, @Param("status") String status);

@Select("SELECT order_id, shop_id, product_id, contact_id, quantity, order_status, order_date FROM t_order WHERE shop_id = #{shopId} AND order_status = #{status}")
List<Order> selectAbstractOrdersByShopAndStatus(@Param("shopId") String shopId, @Param("status") String status);
```

保留：
- `insertOrder(Order)`
- `deleteOrderById(String)`
- `updateOrderStatus(String, String)`
- `selectOrdersByIds(List)` — 内部批量查询

#### 5.2 删除

- `UserOrderMapper.java` — 不再需要
- `DeletedOrderMapper.java` — 保留

### 6. Service 层

#### 6.1 OrderService 接口

```java
public interface OrderService {
    // 写操作
    String createOrder(PlaceOrderRequest request, Long userId);
    int deleteOrder(Long userId, String orderId);
    int updateOrderStatus(String orderId, String status);
    void shipOrder(String orderId, ShipOrderRequest request);
    String generateOrderId();
    
    // 用户端列表/详情（必须带 userId）
    List<OrderAbstractUserDTO> getOrdersByUserId(Long userId);
    List<OrderAbstractUserDTO> getOrdersByUserIdAndStatus(Long userId, String status);
    OrderDetailDTO getOrderDetailByUser(Long userId, String orderId);
    
    // 商家端列表/详情（必须带 shopId）
    List<OrderAbstractSellerDTO> getOrdersByShopId(String shopId);
    List<OrderAbstractSellerDTO> getOrdersByShopIdAndStatus(String shopId, String status);
    OrderDetailDTO getOrderDetailByShop(String shopId, String orderId);
    
    // 内部批量（Feign 调用）
    List<Order> getOrdersByIds(List<String> orderIds);
}
```

#### 6.2 下单流程（核心变更）

```
下单请求 (PlaceOrderRequest)
  → 校验库存（ProductFeignClient）
  → 查 shopId（ShopFeignClient.getShopIdByProductId）
     （productId→shopId 映射在 shop-service 的 product_shops 表）
  → 构建 Order（含 userId + shopId）
  → 写入 t_order
  → 扣库存（ProductFeignClient.deductStock）
  → 返回 orderId
  ★ 不再写入 t_user_order
  ★ 不再调用 ShopFeignClient.associateOrder() 做关联
  ★ ShopFeignClient 仅在下单时使用，不在 Controller 注入（改为 Service 层调用）
```

> 注：`ShopFeignClient.getShopIdByProductId` 对应 shop-service 的 `GET /internal/shop/shop-id-by-product/{productId}`，该端点在 shop-service 重构时补全。

### 7. Controller 层

#### 7.1 OrderUserController — 用户端

```java
@RestController
@RequestMapping("/api/user/order")
@RequiredArgsConstructor
public class OrderUserController {
    
    @GetMapping("/list")
    public ApiResponse<List<OrderAbstractUserDTO>> listOrders(
            @RequestHeader("X-User-Id") Long userId) { ... }
    
    @GetMapping("/list/status")
    public ApiResponse<List<OrderAbstractUserDTO>> listOrdersByStatus(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("status") String status) { ... }
    
    @GetMapping("/{orderId}/detail")
    public ApiResponse<OrderDetailDTO> getOrderDetail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) { ... }
    
    @PostMapping("/place")
    public ApiResponse<String> placeOrder(
            @RequestBody @Valid PlaceOrderRequest request,
            @RequestHeader("X-User-Id") Long userId) { ... }
    
    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> cancelOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) { ... }
}
```

- 不再注入 `ShopFeignClient`、`LogisticsFeignClient`
- 物流和联系人数据在 Service 层组装

#### 7.2 OrderSellerController — 商家端

```java
@RestController
@RequestMapping("/api/seller/order")
@RequiredArgsConstructor
public class OrderSellerController {
    
    @GetMapping("/shop/{shopId}/list")
    public ApiResponse<List<OrderAbstractSellerDTO>> listShopOrders(
            @PathVariable("shopId") String shopId) { ... }
    
    @GetMapping("/shop/{shopId}/{orderId}")
    public ApiResponse<OrderDetailDTO> getShopOrderDetail(
            @PathVariable("shopId") String shopId,
            @PathVariable("orderId") String orderId) { ... }
    
    @PutMapping("/{orderId}/status")
    public ApiResponse<Void> updateOrderStatus(
            @PathVariable("orderId") String orderId,
            @RequestParam("status") String status) { ... }
    
    @PutMapping("/{orderId}/ship")
    public ApiResponse<Void> shipOrder(
            @PathVariable("orderId") String orderId,
            @RequestBody @Valid ShipOrderRequest request) { ... }
}
```

#### 7.3 InternalOrderController — 内部 RPC

```java
@RestController
@RequestMapping("/internal/order")
@RequiredArgsConstructor
public class InternalOrderController {
    
    @GetMapping("/batch")
    public List<Order> getOrdersByIds(@RequestParam("orderIds") String orderIds) { ... }
    
    @GetMapping("/shop/{shopId}")
    public List<OrderAbstractSellerDTO> getOrdersByShopId(
            @PathVariable("shopId") String shopId) { ... }
}
```

- `GET /internal/order/shop/{shopId}` 替代 shop-service 的 OrderShop 查询

### 8. 异常处理（参考 auth-service）

新增 `OrderException`：
```java
public class OrderException extends RuntimeException {
    private int code = 400;
    public OrderException(String message) { super(message); }
    public OrderException(int code, String message) { super(message); this.code = code; }
}
```

新增 `GlobalExceptionHandler`：
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(OrderException.class) { ... }       // 业务异常 → 400
    @ExceptionHandler(MethodArgumentNotValidException.class) { ... } // 参数校验 → 400
    @ExceptionHandler(Exception.class) { ... }            // 兜底 → 500
}
```

所有控制器统一返回 `ApiResponse<T>`，不再手写 `Map.of("message", ...)`。

### 9. Feign 变更

**OrderFeignClient.java** 新增：
```java
@FeignClient(name = "order-service")
public interface OrderFeignClient {
    // ... 保留现有方法
    
    @GetMapping("/internal/order/shop/{shopId}")
    List<OrderAbstractSellerDTO> getOrdersByShopId(@PathVariable("shopId") String shopId);
}
```

**OrderUserController** 移除：
- `ShopFeignClient` — 不再需要调 shop-service 做关联

### 10. 状态筛选支持

- 用户端和商家端均支持按 `orderStatus` 筛选
- 通过 Mapper 的 `AND order_status = #{status}` 实现
- 筛选结果仍然返回抽象 DTO

### 11. 删除的文件/类

| 文件 | 原因 |
|------|------|
| `UserOrder.java` | 被 `t_order.user_id` 替代 |
| `UserOrderMapper.java` | 不再需要关联表操作 |
| `OrderService.createUserOrder()` | 被 `Order.userId` 直接写入替代 |
| `OrderService.deleteUserOrder()` | 不再需要 |
| `OrderService.getShopIdByProductId()` | 移至 Controller 下单流程中 |
| `OrderService.getAllOrders()` | 无身份过滤的不安全接口 |
| `OrderService.getOrdersByStatus()` | 无身份过滤的不安全接口 |

### 12. 渐进式迁移步骤

**Phase 1 — 数据库 + Model：**
1. 修改 `02-order-init.sql`，添加 `user_id`、`shop_id` 字段，删除 `t_user_order`
2. 修改 `Order.java`、`DeletedOrder.java`
3. 删除 `UserOrder.java`

**Phase 2 — Mapper + Service：**
1. `OrderMapper` 新增抽象/详情双路径查询
2. `OrderService` 接口精简
3. 删除 `UserOrderMapper`
4. 新增 `OrderConverter`

**Phase 3 — Controller + Exception：**
1. 重构 `OrderUserController`
2. 重构 `OrderSellerController`
3. 新增 `OrderException` + `GlobalExceptionHandler`
4. 删除 `ShopFeignClient` 注入

**Phase 4 — Feign + 清理：**
1. `OrderFeignClient` 新增 `getOrdersByShopId()`
2. 整合验证、清理无用 import
