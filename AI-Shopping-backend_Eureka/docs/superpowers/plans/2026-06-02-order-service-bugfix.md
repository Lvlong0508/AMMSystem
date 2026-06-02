# Order 服务 API 集成测试 Bug 修复方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 Order 服务 API 集成测试发现的 4 个 Bug，恢复取消 PENDING 订单功能，补齐联系人校验。

**Architecture:** 
- Bug #1：`InternalProductController` 的 `confirmReservation` / `releaseReservation` 使用 `@RequestBody Map`，但 `ProductFeignClient` 声明为 `@RequestParam`，导致 Feign 调用 500。修复方向：改 Controller 为 `@RequestParam` 与 Feign 一致。
- Bug #2：`OrderServiceImpl.createOrder` 下单时不校验 `contactId` 有效性。在创建前调用 `ContactFeignClient.getContactById` 做存在性校验。
- Bug #3：PAID→CANCELLED 在状态机中显式定义，属于业务设计而非 bug。标记为业务确认项。
- Bug #4：Gateway 已从 token 自动注入 `X-User-Id` 头，功能正常。标记为误报，关闭。

**Tech Stack:** Spring Boot 3.x + Spring Cloud OpenFeign + JUnit 5 + Mockito

---

## 涉及文件

| 文件 | Bug # | 变更类型 |
|------|-------|----------|
| `product-service/.../controller/internal/InternalProductController.java:101-109` | #1 | 修改（`confirmReservation` 参数绑定） |
| `product-service/.../controller/internal/InternalProductController.java:140-148` | #1 | 修改（`releaseReservation` 参数绑定） |
| `order-service/.../service/impl/OrderServiceImpl.java:48-81` | #2 | 修改（在 createOrder 中加 contact 校验） |
| `order-service/.../service/OrderServiceImplTest.java` | #1+#2 | 修改（补测试：contact 不存在 × 2，PAID→CANCEL 验证，PENDING→CANCEL 恢复） |
| `order-service/.../service/OrderService.java` | — | 不变 |
| `common-api/.../feign/product/ProductFeignClient.java:49-53` | #1 | 不变（Controller 改，Feign 不动） |
| `common-api/.../feign/contact/ContactFeignClient.java` | #2 | 不变 |
| `gateway-service/.../filter/SaTokenAuthGlobalFilter.java` | #4 | 不变（误报，无需修改） |
| `order-service/.../model/Order.java:34` | #3 | 不变（但标记业务确认） |

---

### Task 1: 修复 Bug #1 — confirmReservation / releaseReservation 参数绑定不匹配

**问题根因：**
- `ProductFeignClient.java:50,53` 使用 `@RequestParam("orderId") String orderId`，Feign 发出 `POST ?orderId=xxx` 查询参数
- `InternalProductController.java:102,141` 使用 `@RequestBody Map<String, String> body`，期望 JSON 请求体
- 两者不一致 → `body.get("orderId")` 为 null → `reservationService.release(null)` 内部抛异常 → 500

**修复策略：**
将 Controller 两个方法的参数由 `@RequestBody Map<String, String> body` 改为 `@RequestParam("orderId") String orderId`，直接读取查询参数。与 ProductReservationService 的其他邻居方法（`reserveStock`, `deductStock`, `restoreStock` 均为 `@RequestBody`）风格不同，但确保 Feign-Controller 契约一致。

**Files:**
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java:101-109`（confirmReservation）
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java:140-148`（releaseReservation）

- [ ] **Step 1: 修改 confirmReservation**

```java
@PostMapping("/confirm-reservation")
public ApiResponse<Void> confirmReservation(@RequestParam("orderId") String orderId) {
    try {
        reservationService.confirm(orderId);
        return ApiResponse.success(null);
    } catch (Exception e) {
        return ApiResponse.error(e.getMessage());
    }
}
```

- [ ] **Step 2: 修改 releaseReservation**

```java
@PostMapping("/release-reservation")
public ApiResponse<Void> releaseReservation(@RequestParam("orderId") String orderId) {
    try {
        reservationService.release(orderId);
        return ApiResponse.success(null);
    } catch (Exception e) {
        return ApiResponse.error(e.getMessage());
    }
}
```

- [ ] **Step 3: 验证编译**

```powershell
mvn clean compile -pl product-service -am -q; if ($?) { echo "BUILD SUCCESS" } else { echo "BUILD FAILURE" }
```

预期：`BUILD SUCCESS`

---

### Task 2: 修复 Bug #2 — 下单时校验 contactId 存在性

**问题根因：**
`OrderServiceImpl.createOrder()` line 71 直接 `order.setContactId(request.getContactId())`，未调用 ContactFeignClient 验证该联系人是否存在。允许写入不存在的 contactId。

**修复策略：**
在 `createOrder()` 的商品校验之后、订单写入之前，调用 `contactFeignClient.getContactById(contactId)` 校验联系人存在性。

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java:48-81`
- Test: `order-service/src/test/java/com/gzasc/aishopping/order/service/OrderServiceImplTest.java`

- [ ] **Step 1: 在 createOrder 中添加 contact 校验**

在当前 line 60（库存不足校验之后）与 line 62（shopId 校验之前）之间插入：

```java
ApiResponse<ContactDTO> contactResp = contactFeignClient.getContactById(request.getContactId());
if (contactResp == null || contactResp.getData() == null) {
    throw new OrderException("联系人不存在，请重新选择联系人（错误代码：O-006）");
}
```

`ContactDTO` 已在文件顶部第 3 行 import。

修改后的完整 `createOrder` 方法局部（仅展示新增插入位置）：

```java
if (stock < request.getQuantity()) {
    throw new OrderException("商品库存不足，当前库存：" + stock + "（错误代码：O-005）");
}

// ── 新增：校验联系人存在性 ──
ApiResponse<ContactDTO> contactResp = contactFeignClient.getContactById(request.getContactId());
if (contactResp == null || contactResp.getData() == null) {
    throw new OrderException("联系人不存在，请重新选择联系人（错误代码：O-006）");
}

Long shopIdObj = product.getShopId();
```

- [ ] **Step 2: 验证编译**

```powershell
mvn clean compile -pl order-service -am -q; if ($?) { echo "BUILD SUCCESS" } else { echo "BUILD FAILURE" }
```

预期：`BUILD SUCCESS`

---

### Task 3: 补充测试（Bug #1 回归 + Bug #2 验证）

**Files:**
- Modify: `order-service/src/test/java/com/gzasc/aishopping/order/service/OrderServiceImplTest.java`

需要新增的测试用例：

| 编号 | 测试方法 | 描述 | 关联 Bug |
|------|----------|------|----------|
| T1 | `createOrder_contactIdNotExists` | contactId 不存在时抛 OrderException("O-006") | #2 |
| T2 | `createOrder_contactFeignReturnsNull` | contactFeignClient 返回 null 时同样抛异常 | #2 |
| T3 | `cancelOrder_pending_success` | PENDING 订单取消成功（验证 releaseReservation Feign 调用链） | #1 |
| T4 | `cancelOrder_paid_alreadyCancelled` | 已取消再取消抛异常 | #3 (回归) |

- [ ] **Step 1: 编写 T1 — contactId 不存在**

```java
@Test
@DisplayName("创建订单-联系人ID不存在则抛异常 O-006")
void createOrder_contactIdNotExists() {
    PlaceOrderRequest req = new PlaceOrderRequest();
    req.setProductId("1");
    req.setQuantity(1);
    req.setContactId(999);

    when(productFeignClient.getProductById(anyLong()))
            .thenReturn(ApiResponse.success(new ProductDTO(...省略完整参数..., BigDecimal.valueOf(99.9))));
    when(contactFeignClient.getContactById(999))
            .thenReturn(ApiResponse.success(null));

    OrderException ex = assertThrows(OrderException.class,
            () -> orderService.createOrder(req, 1L));
    assertTrue(ex.getMessage().contains("O-006"));
}
```

注意：`ProductDTO` 构造需要传 10 个参数（含 imageUrl），参考已有测试 `createOrder_success` 中的 `ProductDTO` 构造方式。

- [ ] **Step 2: 编写 T2 — contactFeign 返回 null**

```java
@Test
@DisplayName("创建订单-联系人服务返回 null 则抛异常 O-006")
void createOrder_contactFeignReturnsNull() {
    PlaceOrderRequest req = new PlaceOrderRequest();
    req.setProductId("1");
    req.setQuantity(1);
    req.setContactId(1);

    when(productFeignClient.getProductById(anyLong()))
            .thenReturn(ApiResponse.success(new ProductDTO(...完整参数..., BigDecimal.valueOf(99.9))));
    when(contactFeignClient.getContactById(anyInt()))
            .thenReturn(null);

    OrderException ex = assertThrows(OrderException.class,
            () -> orderService.createOrder(req, 1L));
    assertTrue(ex.getMessage().contains("O-006"));
}
```

- [ ] **Step 3: 编写 T3 — PENDING 订单取消成功（确认 releaseReservation 调用）**

```java
@Test
@DisplayName("取消订单-未支付订单(PENDING)取消成功，调用 releaseReservation")
void cancelOrder_pending_success() {
    String orderId = "2026060200001TEST";
    Order order = new Order();
    order.setOrderId(orderId);
    order.setUserId(1L);
    order.setShopId("shop1");
    order.setProductId("1");
    order.setQuantity(2);
    order.setOrderStatus(Order.PENDING);

    when(orderMapper.selectOrderDetailByUser(anyLong(), eq(orderId)))
            .thenReturn(order);
    when(orderMapper.updateOrderStatusCas(orderId, Order.CANCELLED, Order.PENDING))
            .thenReturn(1);
    when(productFeignClient.releaseReservation(orderId))
            .thenReturn(ApiResponse.success(null));

    orderService.cancelOrder(1L, orderId);

    verify(productFeignClient).releaseReservation(orderId);
    verify(productFeignClient, never()).restoreStock(any());
}
```

注意：需要 mock `updateOrderStatusCas` 的两次调用。第一次（PAID→CANCELLED）返回 0，第二次（PENDING→CANCELLED）返回 1。

```java
when(orderMapper.updateOrderStatusCas(orderId, Order.CANCELLED, Order.PAID)).thenReturn(0);
when(orderMapper.updateOrderStatusCas(orderId, Order.CANCELLED, Order.PENDING)).thenReturn(1);
```

- [ ] **Step 4: 运行新增测试**

```powershell
mvn test -pl order-service -Dtest='OrderServiceImplTest#createOrder_contactIdNotExists+createOrder_contactFeignReturnsNull+cancelOrder_pending_success' 2>&1 | Select-String -Pattern "Tests run:|BUILD"
```

预期：3/3 通过。

- [ ] **Step 5: 全量回归 order-service 测试**

```powershell
mvn test -pl order-service 2>&1 | Select-String -Pattern "Tests run:.*Failures.*0.*Errors.*0|BUILD"
```

预期：现有 55 + 新增 3 = 58 测试全部通过。

---

### Task 4: Bug #3 PAID→CANCELLED 状态机确认（选做）

**分析：**
- `Order.java:34` `PAID, Set.of(SHIPPED, CANCELLED)` — 状态机 **显式允许** PAID→CANCELLED
- `OrderServiceImpl.cancelOrder L112` 优先 CAS PAID→CANCELLED（意图：已支付订单也可取消并恢复库存）
- 这在电商场景中常见：用户支付后、发货前可取消订单。SAAS 订单系统也普遍支持。

**结论：** 非 bug，而是业务设计。但集成测试中发现此行为与早期的 `02-order-init.sql` 注释不一致（注释中已支付状态不允许取消）。建议与产品确认反馈流程。

**为避免读者困惑，可选更新注释：**

- [ ] **Step 1（可选）：更新 Order.java 状态机注释（仅注释）**

```java
/** 待支付 → 支付 / 取消 */
PENDING, Set.of(PAID, CANCELLED),
/** 已支付 → 发货 / 取消（发货前可取消并自动退款/恢复库存） */
PAID, Set.of(SHIPPED, CANCELLED),
```

- [ ] **Step 2（可选）：更新 02-order-init.sql 中 t_order 表注释**

原注释：
```sql
'订单状态：PENDING待支付 PAID待发货 SHIPPED已发货 DELIVERED已送达 CANCELLED已取消 RETURNED已退款'
```

改为：
```sql
'订单状态：PENDING待支付 PAID已支付(可取消) SHIPPED已发货 DELIVERED已送达 CANCELLED已取消 RETURNED已退款'
```

此步骤非必须，不影响功能。如果选择实施，`0.5 分钟`。

---

### Task 5: Bug #4 X-User-Id 注入追溯（关闭）

**分析：**
- `SaTokenAuthGlobalFilter.java:54` 在鉴权通过后 `.header("X-User-Id", loginId)`
- Sa-Token 的 `loginId` 是纯数值字符串（`"2061615993330995200"`），**不含** "USER:" 前缀
- `OrderUserController` 的 `@RequestHeader("X-User-Id") Long userId` 可正常转换
- 集成测试 U20 验证：不传 `X-User-Id`、仅传 `satoken` → 200 + 正确返回数据

**结论：** 网关自动注入逻辑工作正常，**非 bug**。无需任何修改。

- [ ] **Step 1: 关闭 Bug #4 — 标记为"非 bug，网关自动处理"**

在集成测试报告中添加注记：
```
Bug #4 X-User-Id 注入 — 关闭。原因：SaTokenAuthGlobalFilter 已从 token 自动注入 userId，无需客户端传入。
```

---

## 风险与注意事项

1. **Bug #1 修复后的影响范围**：`confirmReservation` 和 `releaseReservation` 的参数绑定改动后，除 `OrderServiceImpl.cancelOrder` 外，`OrderEventConsumer`（异步消费 Redis Stream 的库存确认事件）也调用 `confirmReservation`，会同步受益。
2. **Bug #2 的影响**：新增的 Feign 调用在长事务中增加额外 RTT。如果 Contact 服务不可用，下单将失败（之前是静默写入无效 ID）。这是预期行为（fail-fast）。
3. **回滚策略**：如果 `contactFeignClient.getContactById` 返回 Contact 服务不可用的 500，`ApiResponse` 的 data 可能为 null，抛异常 → 下单回滚。考虑极端情况：可限制仅在 data==null 时抛异常，而 `ApiResponse` 本身为 null（网络级错误）时保有选择性（当前策略：均抛）。
4. **单元测试中 ProductDTO 构造**：该 DTO 有 10 字段且包含 `imageUrl`（String），构造时不要遗漏最后一个参数。参考已有 `createOrder_success` 的 mock 写法。
5. **Bug #3 不修改生产代码**，仅在文档层面更新注释（可选）。如果产品确认要禁止 PAID→CANCELLED，则从 `Order.java` 的 `TRANSITIONS` 中移除 `CANCELLED` 并修改 `cancelOrder()` 移除 PAID 优先的 CAS 逻辑。

## 预期结果

| Bug | 修复前 | 修复后 |
|-----|--------|--------|
| #1 releaseReservation 500 | 取消 PENDING 订单返回 500 | 返回 200，正常取消+释放预占 |
| #2 联系人未校验 | 创建订单允许写入不存在的 contactId | 抛出 "O-006 联系人不存在" |
| #3 PAID→CANCELLED | 标记为"轻微异常" | 确认是业务设计，更新注释 |
| #4 X-User-Id 注入 | 标记为"轻微异常" | 关闭（网关自动处理） |
