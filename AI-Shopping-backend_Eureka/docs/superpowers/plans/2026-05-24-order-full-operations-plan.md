# 订单双端完整操作实施计划

> **代理说明：** 需要使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐个实施。步骤用 `- [ ]` 标记进度。

**目标：** 补齐用户端和商家端所有缺失的状态操作 API，移除通用的 `updateOrderStatus`，所有转换强制经过状态机验证。

**架构：** Controller 负责路由，Service 方法内完成身份校验 → 状态机验证 → 业务副作用 → 持久化。每个方法封装一次完整的状态转换。

**技术栈：** Spring Boot, MyBatis, OpenFeign

---

## 文件改动清单

| 文件 | 操作 |
|------|------|
| `order-service/.../service/OrderService.java` | 修改：+deliverOrder, +requestReturn, +payOrder, +approveReturn, +confirmReturn, -updateOrderStatus |
| `order-service/.../service/impl/OrderServiceImpl.java` | 修改：实现 5 个新方法，移除 updateOrderStatus |
| `order-service/.../controller/OrderUserController.java` | 修改：+deliver, +return-request 端点 |
| `order-service/.../controller/OrderSellerController.java` | 修改：+pay, +approve-return, +confirm-return 端点，-/status |

---

### 任务 1：更新 OrderService 接口

**文件：**
- 修改：`order-service/src/main/java/com/gzasc/aishopping/order/service/OrderService.java`

- [ ] **步骤 1：替换为完整接口声明**

```java
package com.gzasc.aishopping.order.service;

import com.gzasc.aishopping.order.dto.OrderAbstractSellerDTO;
import com.gzasc.aishopping.order.dto.OrderAbstractUserDTO;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.PlaceOrderRequest;
import com.gzasc.aishopping.order.dto.ShipOrderRequest;

import java.util.List;

public interface OrderService {
    // 用户端操作
    String createOrder(PlaceOrderRequest request, Long userId);
    void cancelOrder(Long userId, String orderId);
    void deleteOrder(Long userId, String orderId);
    void deliverOrder(Long userId, String orderId);
    void requestReturn(Long userId, String orderId);

    // 商家端操作
    void payOrder(String shopId, String orderId);
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

- [ ] **步骤 2：提交**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/service/OrderService.java
git commit -m "refactor: 更新 OrderService 接口，补全双端操作"
```

---

### 任务 2：实现 Service 层新增方法

**文件：**
- 修改：`order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java`

- [ ] **步骤 1：移除 `updateOrderStatus`**

删除以下代码块：
```java
    @Override
    @Transactional
    public void updateOrderStatus(String orderId, String status) {
        Order order = orderMapper.selectOrderById(orderId);
        if (order == null) {
            throw new OrderException("订单不存在");
        }
        if (!order.canTransition(order.getOrderStatus(), status)) {
            throw new OrderException("订单状态不允许从 " + order.getOrderStatus() + " 转换为 " + status);
        }
        if (Order.CANCELLED.equals(status) && Order.PAID.equals(order.getOrderStatus())) {
            StockDeductRequest stockReq =
                    new StockDeductRequest(order.getProductId(), order.getQuantity());
            productFeignClient.restoreStock(stockReq);
        }
        orderMapper.updateOrderStatus(orderId, status);
    }
```

- [ ] **步骤 2：添加 `deliverOrder`**（SHIPPED→DELIVERED）

```java
    @Override
    @Transactional
    public void deliverOrder(Long userId, String orderId) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        order.transitionTo(Order.DELIVERED);
        orderMapper.updateOrderStatus(orderId, Order.DELIVERED);
    }
```

- [ ] **步骤 3：添加 `requestReturn`**（SHIPPED/DELIVERED→RETURN_PENDING）

```java
    @Override
    @Transactional
    public void requestReturn(Long userId, String orderId) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        order.transitionTo(Order.RETURN_PENDING);
        orderMapper.updateOrderStatus(orderId, Order.RETURN_PENDING);
    }
```

- [ ] **步骤 4：添加 `payOrder`**（PENDING→PAID）

```java
    @Override
    @Transactional
    public void payOrder(String shopId, String orderId) {
        Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        if (!order.canTransition(order.getOrderStatus(), Order.PAID)) {
            throw new OrderException("当前订单状态不允许支付");
        }
        Map<String, Object> result = productFeignClient.deductStock(
                new StockDeductRequest(order.getProductId(), order.getQuantity()));
        Boolean success = (Boolean) result.get("success");
        if (!Boolean.TRUE.equals(success)) {
            throw new OrderException("商品库存不足");
        }
        orderMapper.updateOrderStatus(orderId, Order.PAID);
    }
```

- [ ] **步骤 5：添加 `approveReturn`**（RETURN_PENDING→RETURNING）

```java
    @Override
    @Transactional
    public void approveReturn(String shopId, String orderId) {
        Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        order.transitionTo(Order.RETURNING);
        orderMapper.updateOrderStatus(orderId, Order.RETURNING);
    }
```

- [ ] **步骤 6：添加 `confirmReturn`**（RETURNING→RETURNED）

```java
    @Override
    @Transactional
    public void confirmReturn(String shopId, String orderId) {
        Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        order.transitionTo(Order.RETURNED);
        orderMapper.updateOrderStatus(orderId, Order.RETURNED);
    }
```

- [ ] **步骤 7：验证编译**

运行：`mvn compile -pl order-service -am -q`
预期：通过

- [ ] **步骤 8：提交**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java
git commit -m "feat: 实现 deliverOrder/requestReturn/payOrder/approveReturn/confirmReturn"
```

---

### 任务 3：更新用户端 Controller

**文件：**
- 修改：`order-service/src/main/java/.../controller/OrderUserController.java`

- [ ] **步骤 1：添加确认收货和申请退货端点**

```java
    @PutMapping("/{orderId}/deliver")
    public ApiResponse<Void> deliverOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        orderService.deliverOrder(userId, orderId);
        return ApiResponse.success("确认收货成功", null);
    }

    @PostMapping("/{orderId}/return-request")
    public ApiResponse<Void> requestReturn(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        orderService.requestReturn(userId, orderId);
        return ApiResponse.success("退货申请已提交", null);
    }
```

- [ ] **步骤 2：验证编译**

运行：`mvn compile -pl order-service -am -q`
预期：通过

- [ ] **步骤 3：提交**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderUserController.java
git commit -m "feat: 用户端添加确认收货和申请退货接口"
```

---

### 任务 4：更新商家端 Controller

**文件：**
- 修改：`order-service/src/main/java/.../controller/OrderSellerController.java`

- [ ] **步骤 1：替换为完整 Controller**

移除 `PUT /{orderId}/status` 和 `ProductFeignClient` 依赖，添加 `/pay`、`/approve-return`、`/confirm-return` 端点：

```java
package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.dto.OrderAbstractSellerDTO;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.ShipOrderRequest;
import com.gzasc.aishopping.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/order")
@RequiredArgsConstructor
public class OrderSellerController {

    private final OrderService orderService;

    @GetMapping("/shop/{shopId}/list")
    public ApiResponse<List<OrderAbstractSellerDTO>> listShopOrders(
            @PathVariable("shopId") String shopId) {
        List<OrderAbstractSellerDTO> orders = orderService.getOrdersByShopId(shopId);
        return ApiResponse.success(orders);
    }

    @GetMapping("/shop/{shopId}/list/status")
    public ApiResponse<List<OrderAbstractSellerDTO>> listShopOrdersByStatus(
            @PathVariable("shopId") String shopId,
            @RequestParam("status") String status) {
        List<OrderAbstractSellerDTO> orders = orderService.getOrdersByShopIdAndStatus(shopId, status);
        return ApiResponse.success(orders);
    }

    @GetMapping("/shop/{shopId}/{orderId}")
    public ApiResponse<OrderDetailDTO> getShopOrderDetail(
            @PathVariable("shopId") String shopId,
            @PathVariable("orderId") String orderId) {
        OrderDetailDTO detail = orderService.getOrderDetailByShop(shopId, orderId);
        return ApiResponse.success(detail);
    }

    @PutMapping("/{orderId}/pay")
    public ApiResponse<Void> payOrder(
            @PathVariable("orderId") String orderId,
            @RequestParam("shopId") String shopId) {
        orderService.payOrder(shopId, orderId);
        return ApiResponse.success("支付成功", null);
    }

    @PutMapping("/{orderId}/ship")
    public ApiResponse<Void> shipOrder(
            @PathVariable("orderId") String orderId,
            @RequestBody @Valid ShipOrderRequest request,
            @RequestParam("shopId") String shopId) {
        orderService.getOrderDetailByShop(shopId, orderId);
        orderService.shipOrder(orderId, request);
        return ApiResponse.success("发货成功", null);
    }

    @PutMapping("/{orderId}/approve-return")
    public ApiResponse<Void> approveReturn(
            @PathVariable("orderId") String orderId,
            @RequestParam("shopId") String shopId) {
        orderService.approveReturn(shopId, orderId);
        return ApiResponse.success("退货审核通过", null);
    }

    @PutMapping("/{orderId}/confirm-return")
    public ApiResponse<Void> confirmReturn(
            @PathVariable("orderId") String orderId,
            @RequestParam("shopId") String shopId) {
        orderService.confirmReturn(shopId, orderId);
        return ApiResponse.success("退货已确认", null);
    }
}
```

- [ ] **步骤 2：验证编译**

运行：`mvn compile -pl order-service -am -q`
预期：通过

- [ ] **步骤 3：提交**

```bash
git add order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderSellerController.java
git commit -m "feat: 商家端添加支付/退货审核/退货确认接口，移除通用 /status"
```

---

### 任务 5：最终验证

**文件：** 无

- [ ] **步骤 1：全量编译**

运行：`mvn compile -pl order-service,common-api -am -q`
预期：通过

- [ ] **步骤 2：确认无遗留引用**

运行：`rg "updateOrderStatus" order-service/src/` 
预期：仅在 OrderMapper 中匹配（SQL 映射方法仍被其他状态更新使用）
