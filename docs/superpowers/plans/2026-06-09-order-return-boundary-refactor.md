# 订单退货服务边界重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 消除 `ReturnRequestService` 对 `OrderMapper` 的直接依赖，订单状态变更统一走 `OrderService`。

**Architecture:** `OrderService` 新增 `agreeReturnRequest` 和 `submitReturnLogisticsStatus` 方法承担订单状态 CAS；`OrderServiceImpl` 删除 `requestReturn/approveReturn` 方法和对 `ReturnRequestService` 的依赖；`ReturnRequestServiceImpl` 用 `OrderService` 替代 `OrderMapper`。依赖方向：`ReturnRequestService → OrderService`（单向）。

**Tech Stack:** Java 17, Spring Boot 3.2.3, MyBatis, JUnit 5, Mockito。

---

## File Structure

- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/OrderService.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/impl/ReturnRequestServiceImpl.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderSellerController.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/test/java/com/gzasc/aishopping/order/service/OrderServiceImplTest.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/test/java/com/gzasc/aishopping/order/service/ReturnRequestServiceImplTest.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/test/java/com/gzasc/aishopping/order/controller/OrderSellerControllerTest.java`

---

### Task 1: OrderService 接口

**Files:**
- Modify: `OrderService.java`

- [ ] **Step 1: 更新接口**

```java
package com.gzasc.aishopping.order.service;

import com.gzasc.aishopping.order.dto.*;
import java.util.List;

public interface OrderService {
    // 用户端操作
    String createOrder(PlaceOrderRequest request, Long userId);
    void cancelOrder(Long userId, String orderId);
    void deleteOrder(Long userId, String orderId);
    void payOrder(Long userId, String orderId);
    void deliverOrder(Long userId, String orderId);
    // 删除：void requestReturn(Long userId, String orderId);

    // 商家端操作
    void shipOrder(String shopId, String orderId, ShipOrderRequest request);
    // 删除：void approveReturn(String shopId, String orderId);
    void confirmReturn(String shopId, String orderId);

    // 新增：退货相关订单状态变更
    void agreeReturnRequest(String shopId, String orderId);
    void submitReturnLogisticsStatus(Long userId, String orderId);

    // 查询
    List<OrderAbstractUserDTO> getOrdersByUserId(Long userId);
    OrderDetailDTO getOrderDetailByUser(Long userId, String orderId);
    List<OrderAbstractSellerDTO> getOrdersByShopId(String shopId);
    OrderDetailDTO getOrderDetailByShop(String shopId, String orderId);
}
```

- [ ] **Step 2: 编译验证接口**

Run: `mvn -pl order-service -am -DskipTests compile`
Expected: `BUILD SUCCESS`（但 OrderServiceImpl 和测试还没改，实际上编译会失败，继续下一任务）

---

### Task 2: OrderServiceImpl 实现

**Files:**
- Modify: `OrderServiceImpl.java`

- [ ] **Step 1: 删除 `returnRequestService` 字段，更新构造器**

```java
// 删除：private final ReturnRequestService returnRequestService;
// OrderServiceImpl 不再依赖 ReturnRequestService
```

- [ ] **Step 2: 删除 `requestReturn` 方法**

删除 Lines 180-186 方法：
```java
// 整段删除
@Override
@Transactional
public void requestReturn(Long userId, String orderId) {
    CreateReturnRequest req = new CreateReturnRequest();
    req.setReturnReason("用户申请退货");
    returnRequestService.createReturnRequest(userId, orderId, req);
}
```

- [ ] **Step 3: 删除 `approveReturn`，改为新增 `agreeReturnRequest` 和 `submitReturnLogisticsStatus`**

删除 Lines 218-229 的 `approveReturn` 方法。

新增：
```java
@Override
@Transactional
public void agreeReturnRequest(String shopId, String orderId) {
    Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
    if (order == null) {
        throw new OrderException("订单不存在或无权限操作");
    }
    int updated = orderMapper.updateOrderStatusCasMulti(
            orderId, Order.RETURN_PENDING, List.of(Order.SHIPPED, Order.DELIVERED));
    if (updated <= 0) {
        throw new OrderException("订单状态变更失败，请重试");
    }
    log.info("退货审核通过, orderId={}", orderId);
}

@Override
@Transactional
public void submitReturnLogisticsStatus(Long userId, String orderId) {
    Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
    if (order == null) {
        throw new OrderException("订单不存在或无权限操作");
    }
    int updated = orderMapper.updateOrderStatusCas(orderId, Order.RETURNING, Order.RETURN_PENDING);
    if (updated <= 0) {
        throw new OrderException("订单状态变更失败，请重试");
    }
    log.info("退货物流已提交, orderId={}", orderId);
}
```

删除 import 行：
```java
// 删除：import com.gzasc.aishopping.order.service.ReturnRequestService;
// 删除：import com.gzasc.aishopping.order.dto.CreateReturnRequest;
// 保留需要的 import
```

- [ ] **Step 4: 编译验证**

Run: `mvn -pl order-service -am -DskipTests compile`
Expected: `BUILD SUCCESS`（测试未改仍会失败）

---

### Task 3: OrderServiceImplTest 更新

**Files:**
- Modify: `OrderServiceImplTest.java`

- [ ] **Step 1: 删除 `returnRequestService` mock 和构造参数**

```java
// 删除 Line 61-62：@Mock private ReturnRequestService returnRequestService;

// setUp 改为：
@BeforeEach
void setUp() {
    orderService = new OrderServiceImpl(orderMapper, deletedOrderMapper, orderIdSelector,
            productFeignClient, logisticsFeignClient, contactFeignClient,
            orderConverter, fileFallbackDaemon);
}
```

- [ ] **Step 2: 删除 requestReturn 测试（Lines 411-435）**

删除 OR-028 ~ OR-030 三个测试方法。

- [ ] **Step 3: 替换 approveReturn 测试为 agreeReturnRequest 测试**

将 `approveReturn_success` 和 `approveReturn_wrongStatus` 替换为：

```java
@Test
@DisplayName("商家同意退货 - SHIPPED/DELIVERED→RETURN_PENDING")
void agreeReturnRequest_success() {
    Order order = createOrder("ORDER001", 100L, "SHOP001", Order.SHIPPED);
    when(orderMapper.selectOrderDetailByShop("SHOP001", "ORDER001")).thenReturn(order);
    when(orderMapper.updateOrderStatusCasMulti("ORDER001", Order.RETURN_PENDING,
            List.of(Order.SHIPPED, Order.DELIVERED))).thenReturn(1);

    orderService.agreeReturnRequest("SHOP001", "ORDER001");

    verify(orderMapper).updateOrderStatusCasMulti("ORDER001", Order.RETURN_PENDING,
            List.of(Order.SHIPPED, Order.DELIVERED));
}

@Test
@DisplayName("商家同意退货 - 订单不存在")
void agreeReturnRequest_orderNotFound() {
    when(orderMapper.selectOrderDetailByShop("SHOP001", "ORDER001")).thenReturn(null);

    assertThrows(OrderException.class,
            () -> orderService.agreeReturnRequest("SHOP001", "ORDER001"));
}
```

- [ ] **Step 4: 新增 submitReturnLogisticsStatus 测试**

```java
@Test
@DisplayName("提交退货物流状态 - RETURN_PENDING→RETURNING")
void submitReturnLogisticsStatus_success() {
    Order order = createOrder("ORDER001", 100L, "SHOP001", Order.RETURN_PENDING);
    when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
    when(orderMapper.updateOrderStatusCas("ORDER001", Order.RETURNING, Order.RETURN_PENDING)).thenReturn(1);

    orderService.submitReturnLogisticsStatus(100L, "ORDER001");

    verify(orderMapper).updateOrderStatusCas("ORDER001", Order.RETURNING, Order.RETURN_PENDING);
}

@Test
@DisplayName("提交退货物流状态 - 订单不存在")
void submitReturnLogisticsStatus_orderNotFound() {
    when(orderMapper.selectOrderDetailByUser(999L, "ORDER001")).thenReturn(null);

    assertThrows(OrderException.class,
            () -> orderService.submitReturnLogisticsStatus(999L, "ORDER001"));
}

@Test
@DisplayName("提交退货物流状态 - CAS失败")
void submitReturnLogisticsStatus_casFailed() {
    Order order = createOrder("ORDER001", 100L, "SHOP001", Order.RETURN_PENDING);
    when(orderMapper.selectOrderDetailByUser(100L, "ORDER001")).thenReturn(order);
    when(orderMapper.updateOrderStatusCas("ORDER001", Order.RETURNING, Order.RETURN_PENDING)).thenReturn(0);

    assertThrows(OrderException.class,
            () -> orderService.submitReturnLogisticsStatus(100L, "ORDER001"));
}
```

- [ ] **Step 5: 运行测试**

Run: `mvn -pl order-service "-Dtest=OrderServiceImplTest" test`
Expected: `BUILD SUCCESS`

---

### Task 4: ReturnRequestServiceImpl 重构

**Files:**
- Modify: `ReturnRequestServiceImpl.java`

- [ ] **Step 1: 替换 `OrderMapper` 为 `OrderService`**

```java
// 删除：private final OrderMapper orderMapper;
// 新增：
private final OrderService orderService;

// 构造器不变（Lombok @RequiredArgsConstructor 自动处理）
```

- [ ] **Step 2: 改造 `createReturnRequest`**

```java
@Override
@Transactional
public void createReturnRequest(Long userId, String orderId, CreateReturnRequest request) {
    Order order = orderService.getOrderDetailByUser(userId, orderId);
    if (order == null) {
        throw new OrderException("订单不存在或无权限操作");
    }
    if (!Order.SHIPPED.equals(order.getOrderStatus()) && !Order.DELIVERED.equals(order.getOrderStatus())) {
        throw new OrderException("当前订单状态不允许申请退货");
    }
    if (returnRequestMapper.selectByOrderId(orderId) != null) {
        throw new OrderException("该订单已存在退货申请");
    }

    ReturnRequest returnRequest = new ReturnRequest();
    returnRequest.setOrderId(orderId);
    returnRequest.setUserId(userId);
    returnRequest.setShopId(order.getShopId());
    returnRequest.setReturnReason(request.getReturnReason());
    returnRequest.setStatus(ReturnRequest.APPLYING);
    returnRequest.setCreatedDate(new Timestamp(System.currentTimeMillis()));

    int inserted;
    try {
        inserted = returnRequestMapper.insert(returnRequest);
    } catch (RuntimeException e) {
        throw new OrderException("创建退货申请失败");
    }
    if (inserted <= 0) {
        throw new OrderException("创建退货申请失败");
    }
    log.info("退货申请已提交, orderId={}", orderId);
}
```

- [ ] **Step 3: 改造 `reviewReturnRequest`**

```java
@Override
@Transactional
public void reviewReturnRequest(String shopId, String orderId, ReviewReturnRequest request) {
    if (!ReturnRequest.AGREED.equals(request.getStatus()) && !ReturnRequest.REJECTED.equals(request.getStatus())) {
        throw new OrderException("审核状态无效");
    }

    ReturnRequest returnRequest = returnRequestMapper.selectByOrderIdAndShop(orderId, shopId);
    if (returnRequest == null || !returnRequest.isApplying()) {
        throw new OrderException("退货申请不存在或已被处理");
    }

    int updated = returnRequestMapper.updateStatus(orderId, request.getStatus());
    if (updated <= 0) {
        throw new OrderException("审核失败");
    }

    if (ReturnRequest.AGREED.equals(request.getStatus())) {
        orderService.agreeReturnRequest(shopId, orderId);
    } else {
        log.info("退货申请已拒绝, orderId={}", orderId);
    }
}
```

- [ ] **Step 4: 改造 `submitReturnLogistics`**

```java
@Override
@Transactional
public void submitReturnLogistics(Long userId, String orderId, SubmitReturnLogisticsRequest request) {
    ReturnRequest returnRequest = returnRequestMapper.selectByOrderIdAndUser(orderId, userId);
    if (returnRequest == null) {
        throw new OrderException("退货申请不存在");
    }
    if (!returnRequest.isAgreed()) {
        throw new OrderException("退货申请未通过审核");
    }
    if (returnRequest.getLogisticsId() != null) {
        throw new OrderException("已提交过退货物流信息");
    }

    Order order = orderService.getOrderDetailByUser(userId, orderId);
    if (order == null) {
        throw new OrderException("订单不存在");
    }
    if (!Order.RETURN_PENDING.equals(order.getOrderStatus())) {
        throw new OrderException("订单状态不允许提交退货物流");
    }

    LogisticsRequest logisticsReq = new LogisticsRequest();
    logisticsReq.setOrderId(orderId);
    logisticsReq.setType("RETURN");
    logisticsReq.setContactId(request.getContactId());
    logisticsReq.setTrackingNumber(request.getTrackingNumber());

    // ... logistics创建逻辑保持不变 ...
    ApiResponse<Map<String, Object>> response;
    try {
        response = logisticsFeignClient.createLogistics(logisticsReq);
    } catch (RuntimeException e) {
        throw new OrderException("创建退货物流失败");
    }
    if (response == null || response.getCode() != 200 || response.getData() == null) {
        throw new OrderException("创建退货物流失败");
    }
    Object id = response.getData().get("id");
    int logisticsId = parseLogisticsId(id);

    int updated = returnRequestMapper.updateLogisticsId(orderId, logisticsId);
    if (updated <= 0) {
        throw new OrderException("更新退货物流信息失败");
    }

    orderService.submitReturnLogisticsStatus(userId, orderId);
    log.info("退货物流已提交, orderId={}, logisticsId={}", orderId, logisticsId);
}
```

删除 import 行：`import com.gzasc.aishopping.order.mapper.OrderMapper;`。

- [ ] **Step 5: 编译验证**

Run: `mvn -pl order-service -am -DskipTests compile`
Expected: `BUILD SUCCESS`

---

### Task 5: ReturnRequestServiceImplTest 更新

**Files:**
- Modify: `ReturnRequestServiceImplTest.java`

- [ ] **Step 1: 替换 mock 声明**

```java
// 删除：@Mock private OrderMapper orderMapper;
// 新增：
@Mock private OrderService orderService;
```

删除 import：`import com.gzasc.aishopping.order.mapper.OrderMapper;`。
新增 import：`import com.gzasc.aishopping.order.service.OrderService;`。
删除 import：`import com.gzasc.aishopping.order.model.Order;`（仅用于测试 helper）。

确保保留：`import com.gzasc.aishopping.order.exception.OrderException;`。

- [ ] **Step 2: 更新构造器**

```java
@BeforeEach
void setUp() {
    returnRequestService = new ReturnRequestServiceImpl(returnRequestMapper, orderService, logisticsFeignClient);
}
```

- [ ] **Step 3: 移除 `order()` 方法（不再需要），使用内联构造**

```java
private ReturnRequest returnRequest(String status) {
    ReturnRequest request = new ReturnRequest();
    request.setOrderId(orderId);
    request.setUserId(userId);
    request.setShopId(shopId);
    request.setReturnReason("商品有瑕疵");
    request.setStatus(status);
    request.setCreatedDate(new Timestamp(System.currentTimeMillis()));
    request.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
    return request;
}
```

- [ ] **Step 4: 更新 createReturnRequest 测试**

```java
@Test
void createReturnRequest_createsApplyingRequestForShippedOrder() {
    CreateReturnRequest req = new CreateReturnRequest();
    req.setReturnReason("商品有瑕疵");

    OrderDetailDTO orderDto = new OrderDetailDTO();
    orderDto.setOrderId(orderId);
    orderDto.setUserId(userId);
    orderDto.setShopId(shopId);
    orderDto.setOrderStatus(Order.SHIPPED);

    when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(orderDto);
    when(returnRequestMapper.selectByOrderId(orderId)).thenReturn(null);
    when(returnRequestMapper.insert(any())).thenReturn(1);

    returnRequestService.createReturnRequest(userId, orderId, req);

    ArgumentCaptor<ReturnRequest> captor = ArgumentCaptor.forClass(ReturnRequest.class);
    verify(returnRequestMapper).insert(captor.capture());
    assertEquals(orderId, captor.getValue().getOrderId());
    assertEquals(userId, captor.getValue().getUserId());
    assertEquals(shopId, captor.getValue().getShopId());
    assertEquals(ReturnRequest.APPLYING, captor.getValue().getStatus());
}

@Test
void createReturnRequest_rejectsMissingInvalidOrDuplicateOrder() {
    CreateReturnRequest req = new CreateReturnRequest();
    req.setReturnReason("原因");

    when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(null);
    assertThrows(OrderException.class, () -> returnRequestService.createReturnRequest(userId, orderId, req));

    OrderDetailDTO pendingOrder = new OrderDetailDTO();
    pendingOrder.setOrderStatus(Order.PENDING);
    when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(pendingOrder);
    assertThrows(OrderException.class, () -> returnRequestService.createReturnRequest(userId, orderId, req));

    OrderDetailDTO deliveredOrder = new OrderDetailDTO();
    deliveredOrder.setUserId(userId);
    deliveredOrder.setShopId(shopId);
    deliveredOrder.setOrderStatus(Order.DELIVERED);
    deliveredOrder.setOrderId(orderId);
    when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(deliveredOrder);
    when(returnRequestMapper.selectByOrderId(orderId)).thenReturn(returnRequest(ReturnRequest.APPLYING));
    assertThrows(OrderException.class, () -> returnRequestService.createReturnRequest(userId, orderId, req));
}
```

- [ ] **Step 5: 更新 reviewReturnRequest 测试**

```java
@Test
void reviewReturnRequest_agreeUpdatesRequestAndOrderToReturnPending() {
    ReviewReturnRequest req = new ReviewReturnRequest();
    req.setStatus(ReturnRequest.AGREED);
    when(returnRequestMapper.selectByOrderIdAndShop(orderId, shopId)).thenReturn(returnRequest(ReturnRequest.APPLYING));
    when(returnRequestMapper.updateStatus(orderId, ReturnRequest.AGREED)).thenReturn(1);
    doNothing().when(orderService).agreeReturnRequest(shopId, orderId);

    returnRequestService.reviewReturnRequest(shopId, orderId, req);

    verify(returnRequestMapper).updateStatus(orderId, ReturnRequest.AGREED);
    verify(orderService).agreeReturnRequest(shopId, orderId);
}

@Test
void reviewReturnRequest_rejectDoesNotChangeOrder() {
    ReviewReturnRequest req = new ReviewReturnRequest();
    req.setStatus(ReturnRequest.REJECTED);
    when(returnRequestMapper.selectByOrderIdAndShop(orderId, shopId)).thenReturn(returnRequest(ReturnRequest.APPLYING));
    when(returnRequestMapper.updateStatus(orderId, ReturnRequest.REJECTED)).thenReturn(1);

    returnRequestService.reviewReturnRequest(shopId, orderId, req);

    verify(returnRequestMapper).updateStatus(orderId, ReturnRequest.REJECTED);
    verify(orderService, never()).agreeReturnRequest(anyString(), anyString());
}

@Test
void reviewReturnRequest_rejectsInvalidMissingProcessedOrCasFailure() {
    ReviewReturnRequest req = new ReviewReturnRequest();
    req.setStatus("invalid");
    assertThrows(OrderException.class, () -> returnRequestService.reviewReturnRequest(shopId, orderId, req));

    req.setStatus(ReturnRequest.AGREED);
    when(returnRequestMapper.selectByOrderIdAndShop(orderId, shopId)).thenReturn(null);
    assertThrows(OrderException.class, () -> returnRequestService.reviewReturnRequest(shopId, orderId, req));

    when(returnRequestMapper.selectByOrderIdAndShop(orderId, shopId)).thenReturn(returnRequest(ReturnRequest.REJECTED));
    assertThrows(OrderException.class, () -> returnRequestService.reviewReturnRequest(shopId, orderId, req));

    when(returnRequestMapper.selectByOrderIdAndShop(orderId, shopId)).thenReturn(returnRequest(ReturnRequest.APPLYING));
    when(returnRequestMapper.updateStatus(orderId, ReturnRequest.AGREED)).thenReturn(1);
    doThrow(new OrderException("订单状态变更失败，请重试"))
            .when(orderService).agreeReturnRequest(shopId, orderId);
    assertThrows(OrderException.class, () -> returnRequestService.reviewReturnRequest(shopId, orderId, req));
}
```

- [ ] **Step 6: 更新 submitReturnLogistics 测试**

```java
@Test
void submitReturnLogistics_createsLogisticsStoresIdAndMovesOrderToReturning() {
    SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
    req.setTrackingNumber("SF123456789");
    req.setContactId(1);
    when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(returnRequest(ReturnRequest.AGREED));

    OrderDetailDTO orderDto = new OrderDetailDTO();
    orderDto.setOrderId(orderId);
    orderDto.setUserId(userId);
    orderDto.setOrderStatus(Order.RETURN_PENDING);
    when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(orderDto);

    when(logisticsFeignClient.createLogistics(any(LogisticsRequest.class)))
            .thenReturn(ApiResponse.success(Map.of("id", 42)));
    when(returnRequestMapper.updateLogisticsId(orderId, 42)).thenReturn(1);
    doNothing().when(orderService).submitReturnLogisticsStatus(userId, orderId);

    returnRequestService.submitReturnLogistics(userId, orderId, req);

    ArgumentCaptor<LogisticsRequest> captor = ArgumentCaptor.forClass(LogisticsRequest.class);
    verify(logisticsFeignClient).createLogistics(captor.capture());
    assertEquals(orderId, captor.getValue().getOrderId());
    assertEquals("RETURN", captor.getValue().getType());
    verify(returnRequestMapper).updateLogisticsId(orderId, 42);
    verify(orderService).submitReturnLogisticsStatus(userId, orderId);
}

@Test
void submitReturnLogistics_rejectsInvalidStatesAndFailures() {
    SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
    req.setTrackingNumber("SF123");
    req.setContactId(1);

    when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(null);
    assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

    when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(returnRequest(ReturnRequest.APPLYING));
    assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

    ReturnRequest withLogistics = returnRequest(ReturnRequest.AGREED);
    withLogistics.setLogisticsId(99);
    when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(withLogistics);
    assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

    when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(returnRequest(ReturnRequest.AGREED));
    OrderDetailDTO shippedOrder = new OrderDetailDTO();
    shippedOrder.setOrderStatus(Order.SHIPPED);
    when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(shippedOrder);
    assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

    OrderDetailDTO returnPendingOrder = new OrderDetailDTO();
    returnPendingOrder.setOrderStatus(Order.RETURN_PENDING);
    when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(returnPendingOrder);
    when(logisticsFeignClient.createLogistics(any())).thenReturn(ApiResponse.error(500, "创建失败"));
    assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

    when(logisticsFeignClient.createLogistics(any())).thenReturn(ApiResponse.success(Map.of("id", 42)));
    when(returnRequestMapper.updateLogisticsId(orderId, 42)).thenReturn(0);
    assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

    when(returnRequestMapper.updateLogisticsId(orderId, 42)).thenReturn(1);
    doThrow(new OrderException("订单状态变更失败，请重试"))
            .when(orderService).submitReturnLogisticsStatus(userId, orderId);
    assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));
}
```

- [ ] **Step 7: 运行测试确认通过**

Run: `mvn -pl order-service "-Dtest=ReturnRequestServiceImplTest" test`
Expected: `BUILD SUCCESS`

---

### Task 6: OrderSellerController 改造

**Files:**
- Modify: `OrderSellerController.java`
- Modify: `OrderSellerControllerTest.java`

- [ ] **Step 1: 删除 `/approve-return` 端点**

```java
// 删除 Lines 51-57：
//@PutMapping("/{orderId}/approve-return")
//public ApiResponse<Void> approveReturn(...)
```

删除 import：`import com.gzasc.aishopping.order.dto.OrderAbstractSellerDTO;`
（这个还被别处用到，不能删）

- [ ] **Step 2: 删除 OrderSellerControllerTest 中的 approveReturn 测试**

删除 Lines 137-161 的 OR-031/OR-032 approveReturn 测试。

- [ ] **Step 3: 运行测试**

Run: `mvn -pl order-service "-Dtest=OrderSellerControllerTest" test`
Expected: `BUILD SUCCESS`

---

### Task 7: 全量验证

**Files:**
- All files changed above.

- [ ] **Step 1: 全量测试**

Run: `mvn -pl order-service -am test`
Expected: `BUILD SUCCESS`（基线失败 `DeletedOrderMapperTest.selectAllDeletedOrders_shouldReturnAll` 不计）

- [ ] **Step 2: 确认依赖方向**

`ReturnRequestService` → `OrderService`，无循环依赖。验证方式：`OrderServiceImpl` 不引用 `ReturnRequestService`。

---

## Self-Review

- Spec coverage: 覆盖接口、实现、Controller、测试的全链路变更
- Placeholder scan: 无 TBD/TODO
- Type consistency: `agreeReturnRequest(shopId, orderId)` / `submitReturnLogisticsStatus(userId, orderId)` 在所有文件中一致
- 依赖方向：单向 `ReturnRequestService → OrderService`
