# 订单双端完整操作设计

## 概述

补齐订单系统中用户端和商家端缺失的状态操作，使所有合法的状态转换都有对应的 API 端点，且全部经过状态机验证。

## 状态机

```
PENDING ──→ PAID, CANCELLED
PAID ────→ SHIPPED, CANCELLED
SHIPPED ─→ DELIVERED, RETURN_PENDING
DELIVERED → RETURN_PENDING, DELETED
RETURN_PENDING → RETURNING
RETURNING → RETURNED
CANCELLED ──→ DELETED
```

## 架构

- Controller 层负责路由，不再包含库存/物流等业务逻辑
- Service 层每个独立方法封装一次完整的状态转换：身份校验 → 状态机验证 → 业务副作用（扣库存/恢复库存/创建物流）→ 持久化
- 所有状态转换通过 `Order.transitionTo()` 统一校验

## 端点设计

### 用户端 OrderUserController

| 方法 | 端点 | Service 方法 | 转换 | 附带逻辑 |
|------|------|-------------|------|---------|
| POST | `/api/user/order/place` | `createOrder()` | →PENDING | — |
| PUT | `/api/user/order/{orderId}/pay` | `payOrder(userId, orderId)` | PENDING→PAID | 扣库存 |
| PUT | `/api/user/order/{orderId}/cancel` | `cancelOrder(userId, orderId)` | PENDING/PAID→CANCELLED | PAID 时恢复库存 |
| PUT | `/api/user/order/{orderId}/deliver` | `deliverOrder(userId, orderId)` | SHIPPED→DELIVERED | — |
| POST | `/api/user/order/{orderId}/return-request` | `requestReturn(userId, orderId)` | SHIPPED/DELIVERED→RETURN_PENDING | — |
| DELETE | `/api/user/order/{orderId}` | `deleteOrder(userId, orderId)` | CANCELLED/DELIVERED→物理删除 | 备份到 deleted_orders |

### 商家端 OrderSellerController

| 方法 | 端点 | Service 方法 | 转换 | 附带逻辑 |
|------|------|-------------|------|---------|
| PUT | `/api/seller/order/{orderId}/ship` | `shipOrder(orderId, request)` | PAID→SHIPPED | 创建物流 |
| PUT | `/api/seller/order/{orderId}/approve-return` | `approveReturn(shopId, orderId)` | RETURN_PENDING→RETURNING | — |
| PUT | `/api/seller/order/{orderId}/confirm-return` | `confirmReturn(shopId, orderId)` | RETURNING→RETURNED | — |

商家不允许取消订单。

## Service 接口

```java
public interface OrderService {
    // 用户端
    String createOrder(PlaceOrderRequest request, Long userId);
    void cancelOrder(Long userId, String orderId);
    void deleteOrder(Long userId, String orderId);
    void payOrder(Long userId, String orderId);
    void deliverOrder(Long userId, String orderId);
    void requestReturn(Long userId, String orderId);

    // 商家端
    void shipOrder(String orderId, ShipOrderRequest request);
    void approveReturn(String shopId, String orderId);
    void confirmReturn(String shopId, String orderId);

    // 查询
    List<OrderAbstractUserDTO> getOrdersByUserId(Long userId);
    OrderDetailDTO getOrderDetailByUser(Long userId, String orderId);
    List<OrderAbstractSellerDTO> getOrdersByShopId(String shopId);
    List<OrderAbstractSellerDTO> getOrdersByShopIdAndStatus(String shopId, String status);
    OrderDetailDTO getOrderDetailByShop(String shopId, String orderId);

    String generateOrderId();
}
```

每个方法内部按顺序执行：
1. 通过 `selectOrderDetailByUser(shopId)` 获取订单并校验身份
2. 调用 `Order.transitionTo()` 校验状态转换
3. 执行业务副作用（扣库存/恢复库存/创建物流等）
4. 调用 `orderMapper.updateOrderStatus()` 持久化

## 约束

- 所有写操作必须携带身份标识（userId 或 shopId）
- 不允许商户端删除订单、取消订单
- 不允许无身份过滤的批量查询
