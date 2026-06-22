# 下单流程并发化 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `OrderServiceImpl.createOrder()` 中两次互相独立的 Feign 调用（getProductById / getContactById）改为 CompletableFuture 并行执行，缩短单次下单 RT。

**Architecture:** 引入 `CompletableFuture.allOf()` 编排两个 supplier，使用 JDK 默认的 `ForkJoinPool.commonPool()`，异常解包能力下沉到 `OrderException.unwrap()` 静态方法以便复用。除 createOrder 前两步外，其余逻辑（库存预校验、shopId 校验、订单 ID 生成、insertOrder、reserveStock、事件发布）完全保持原样。

**Tech Stack:** Java 17（pattern matching for instanceof）、Spring Boot、JUnit 5、Mockito、CompletableFuture（JDK 标准库）。

**Spec:** `docs/superpowers/specs/2026-06-22-concurrent-order-design.md`

---

## 文件改动总览

| 文件 | 改动类型 | 责任 |
|---|---|---|
| `order-service/.../exception/OrderException.java` | 修改 | 新增 `static unwrap(Throwable)` 静态方法 |
| `order-service/.../service/impl/OrderServiceImpl.java` | 修改 | 改写 createOrder 前两步为并行；新增 fetchProduct / fetchContact 两个 private 方法 |
| `order-service/.../exception/OrderExceptionTest.java` | 新增 | unwrap 单元测试（3 个用例） |
| `order-service/.../service/OrderServiceImplTest.java` | 修改 | 新增 4 个并发场景用例 |

**共 4 个文件：3 修改 + 1 新增测试文件。**

> 注：spec 文件清单写的是 3 个，未单列 unwrap 的单元测试。本计划补充独立的 OrderExceptionTest.java，使工具方法可独立验证（更符合可复用性硬性要求）。

---

## Task 1: OrderException 新增 unwrap 静态方法（TDD）

**Files:**
- Create: `AI-Shopping-backend/order-service/src/test/java/com/gzasc/aishopping/order/exception/OrderExceptionTest.java`
- Modify: `AI-Shopping-backend/order-service/src/main/java/com/gzasc/aishopping/order/exception/OrderException.java`

### Step 1.1 写失败的测试

- [ ] 创建文件 `OrderExceptionTest.java`，内容如下：

```java
package com.gzasc.aishopping.order.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class OrderExceptionTest {

    @Test
    @DisplayName("unwrap - cause 为 OrderException 时原样返回")
    void unwrap_orderExceptionCause_returnsSameInstance() {
        OrderException original = new OrderException("商品不存在（错误代码：O-003）");
        CompletionException wrapped = new CompletionException(original);

        OrderException result = OrderException.unwrap(wrapped);

        assertSame(original, result);
        assertEquals("商品不存在（错误代码：O-003）", result.getMessage());
    }

    @Test
    @DisplayName("unwrap - cause 为非 OrderException 时返回通用 OrderException")
    void unwrap_nonOrderExceptionCause_returnsGenericOrderException() {
        RuntimeException original = new RuntimeException("网络抖动");
        CompletionException wrapped = new CompletionException(original);

        OrderException result = OrderException.unwrap(wrapped);

        assertNotSame(original, result);
        assertTrue(result.getMessage().contains("系统繁忙"));
    }

    @Test
    @DisplayName("unwrap - cause 为 null 时返回通用 OrderException")
    void unwrap_nullCause_returnsGenericOrderException() {
        CompletionException wrapped = new CompletionException("no cause", null);

        OrderException result = OrderException.unwrap(wrapped);

        assertNotNull(result);
        assertTrue(result.getMessage().contains("系统繁忙"));
    }
}
```

### Step 1.2 运行测试，确认失败

- [ ] 在 `AI-Shopping-backend` 目录运行：

```
mvn -pl order-service -am test -Dtest=OrderExceptionTest
```

期望：编译失败，错误信息 `cannot find symbol: method unwrap(java.util.concurrent.CompletionException)`。

### Step 1.3 实现 unwrap

- [ ] 修改 `OrderException.java` 完整内容如下：

```java
package com.gzasc.aishopping.order.exception;

public class OrderException extends RuntimeException {
    private int code = 400;

    public OrderException(String message) {
        super(message);
    }

    public OrderException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 把 CompletableFuture.join() 抛出的 CompletionException 解包为业务异常。
     * - cause 是 OrderException -> 原样返回（保留原始错误代码和文案）
     * - 其它情况（含 cause 为 null） -> 返回通用 "系统繁忙，请稍后重试"
     *
     * 调用方使用： throw OrderException.unwrap(e);
     */
    public static OrderException unwrap(Throwable e) {
        Throwable cause = e == null ? null : e.getCause();
        if (cause instanceof OrderException oe) {
            return oe;
        }
        return new OrderException("系统繁忙，请稍后重试");
    }
}
```

### Step 1.4 运行测试，确认通过

- [ ] 运行：

```
mvn -pl order-service -am test -Dtest=OrderExceptionTest
```

期望：`Tests run: 3, Failures: 0, Errors: 0`。

### Step 1.5 提交

- [ ] 

```
git add AI-Shopping-backend/order-service/src/main/java/com/gzasc/aishopping/order/exception/OrderException.java AI-Shopping-backend/order-service/src/test/java/com/gzasc/aishopping/order/exception/OrderExceptionTest.java
git commit -m "feat(order): OrderException 新增 unwrap 静态方法以支持 CompletionException 解包"
```

---
## Task 2: OrderServiceImpl 抽出 fetchProduct / fetchContact（先重构，再并行）

**目的：** 先把 Feign 调用 + 空校验抽出为 private 方法，语义不变，确保现有 OrderServiceImplTest 全绿后，再做并行化。

**注意语义微调：** 抽取后调用顺序改为先 fetchProduct、再 fetchContact、再做库存预校验。原代码顺序是 fetchProduct -> 库存预校验 -> fetchContact。这意味着库存不足/reserveStock 失败的两个现有用例需要补 `mockContact(1);`。

**Files:**
- Modify: `AI-Shopping-backend/order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java`
- Modify: `AI-Shopping-backend/order-service/src/test/java/com/gzasc/aishopping/order/service/OrderServiceImplTest.java`

### Step 2.1 跑现有测试，确认基线绿

- [ ] 运行：

```
mvn -pl order-service -am test -Dtest=OrderServiceImplTest
```

期望：所有用例 PASS。

### Step 2.2 改 createOrder，抽出辅助方法

- [ ] 用以下内容替换 `OrderServiceImpl.java` 中 `createOrder` 方法整段（第 50 行 `@Override` 到第 90 行右花括号 `}`）：

```java
    @Override
    @Transactional
    public String createOrder(PlaceOrderRequest request, Long userId) {
        ProductDTO product = fetchProduct(request.getProductId());
        ContactDTO contact = fetchContact(request.getContactId());

        BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        int stock = product.getStock() != null ? product.getStock() : 0;

        if (stock < request.getQuantity()) {
            throw new OrderException("商品库存不足，当前库存：" + stock + "（错误代码：O-005）");
        }

        Long shopIdObj = product.getShopId();
        if (shopIdObj == null) {
            throw new OrderException("获取店铺信息失败");
        }
        String shopId = String.valueOf(shopIdObj);

        String orderId = orderIdSelector.generate();
        Order order = Order.buildInitOrder(orderId, userId, shopId, String.valueOf(request.getProductId()),
                request.getQuantity(), price.multiply(BigDecimal.valueOf(request.getQuantity())));
        order.setContactId(request.getContactId());

        int result = orderMapper.insertOrder(order);
        if (result <= 0) {
            throw new OrderException("创建订单失败");
        }

        productFeignClient.reserveStock(new StockReserveRequest(orderId, Long.valueOf(order.getProductId()), request.getQuantity()));

        return orderId;
    }

    private ProductDTO fetchProduct(Long productId) {
        ApiResponse<ProductDTO> resp = productFeignClient.getProductById(productId);
        if (resp == null || resp.getData() == null) {
            throw new OrderException("商品不存在（错误代码：O-003）");
        }
        return resp.getData();
    }

    private ContactDTO fetchContact(Integer contactId) {
        ApiResponse<ContactDTO> resp = contactFeignClient.getContactById(contactId);
        if (resp == null || resp.getData() == null) {
            throw new OrderException("联系人不存在，请重新选择联系人（错误代码：O-006）");
        }
        return resp.getData();
    }
```

### Step 2.3 调整受影响的两个现有测试用例

- [ ] 修改 `OrderServiceImplTest.java` 中 `createOrder_insufficientStock`（OR-003），在 `when(productFeignClient.getProductById(1L)).thenReturn(...)` 行后追加 `mockContact(1);`。改后内容：

```java
    @Test
    @DisplayName("OR-003 下单 - 库存不足")
    void createOrder_insufficientStock() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(5);
        request.setContactId(1);

        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 3, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        mockContact(1);

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder(request, 100L));
        assertTrue(ex.getMessage().contains("库存不足"));
        verify(orderMapper, never()).insertOrder(any());
    }
```

- [ ] 检查 `OrderServiceImplTest.java` 中 `createOrder_shopIdNull`（OR-002）：该用例原本已有 `mockContact(1);`（原代码顺序是 product -> stock -> contact -> shopId），重构后顺序变为 product -> contact -> stock -> shopId，mockContact 依然必需，**保持原样无需修改**。

注：本步骤只用核对，确认该用例 mock 完整即可跳过。

### Step 2.4 跑调整后的测试，确认全绿

- [ ] 运行：

```
mvn -pl order-service -am test -Dtest=OrderServiceImplTest
```

期望：所有用例 PASS（原有 30+ 个加调整后的）。

### Step 2.5 提交

- [ ] 

```
git add AI-Shopping-backend/order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java AI-Shopping-backend/order-service/src/test/java/com/gzasc/aishopping/order/service/OrderServiceImplTest.java
git commit -m "refactor(order): createOrder 提取 fetchProduct / fetchContact 私有方法（等价重构）"
```

---
## Task 3: 并行化 fetchProduct / fetchContact（TDD - 红）

**目的：** 先写 4 个表达"并行行为"的新测试用例，确认它们能跑（即编译通过），但其中某些用例会因为"目前是串行"而无法验证并行特性（仅断言行为正确性）。然后下一个 Task 把代码真正并行。

**Files:**
- Modify: `AI-Shopping-backend/order-service/src/test/java/com/gzasc/aishopping/order/service/OrderServiceImplTest.java`

### Step 3.1 在 OrderServiceImplTest 顶部 import 添加

- [ ] 在 import 区追加：

```java
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
```

### Step 3.2 添加 4 个新用例

- [ ] 在 `// ==================== 下单 (OR-001 ~ OR-005) ====================` 注释块末尾、`// ==================== 支付` 注释块之前，追加以下用例：

```java
    // ==================== 并发场景 (OR-080 ~ OR-083) ====================

    @Test
    @DisplayName("OR-080 并发下单 - 商品/联系人查询都成功")
    void createOrder_concurrent_bothSuccess() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(1);
        request.setContactId(1);

        when(orderIdSelector.generate()).thenReturn("ORDER_CONCURRENT_001");
        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        mockContact(1);
        when(orderMapper.insertOrder(any(Order.class))).thenReturn(1);
        when(productFeignClient.reserveStock(any(StockReserveRequest.class)))
                .thenReturn(ApiResponse.success(null));

        String orderId = orderService.createOrder(request, 100L);

        assertEquals("ORDER_CONCURRENT_001", orderId);
        verify(productFeignClient).getProductById(1L);
        verify(contactFeignClient).getContactById(1);
    }

    @Test
    @DisplayName("OR-081 并发下单 - 商品查询失败时联系人查询异常被吞掉，对外只抛 O-003")
    void createOrder_concurrent_productFailed() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(1);
        request.setContactId(1);

        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(null));
        // 联系人查询也可能被调用（并行场景），但不影响最终异常
        when(contactFeignClient.getContactById(1)).thenReturn(ApiResponse.success(null));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder(request, 100L));
        // 商品和联系人查询都返回 null 时，谁先完成就抛谁；都可能。这里允许两种文案。
        assertTrue(ex.getMessage().contains("商品不存在") || ex.getMessage().contains("联系人不存在"));
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    @DisplayName("OR-082 并发下单 - 联系人查询失败时对外抛 O-006")
    void createOrder_concurrent_contactFailed() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(1);
        request.setContactId(1);

        ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null);
        when(productFeignClient.getProductById(1L)).thenReturn(ApiResponse.success(mockProduct));
        when(contactFeignClient.getContactById(1)).thenReturn(ApiResponse.success(null));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder(request, 100L));
        assertTrue(ex.getMessage().contains("联系人不存在"));
        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    @DisplayName("OR-083 并发下单 - Feign 抛 RuntimeException 时包装为通用 OrderException")
    void createOrder_concurrent_feignRuntimeException() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setProductId(1L);
        request.setQuantity(1);
        request.setContactId(1);

        when(productFeignClient.getProductById(1L)).thenThrow(new RuntimeException("网络抖动"));
        when(contactFeignClient.getContactById(1)).thenThrow(new RuntimeException("网络抖动"));

        OrderException ex = assertThrows(OrderException.class,
                () -> orderService.createOrder(request, 100L));
        assertTrue(ex.getMessage().contains("系统繁忙"));
        verify(orderMapper, never()).insertOrder(any());
    }
```

### Step 3.3 跑新用例，预期 OR-083 失败

- [ ] 运行：

```
mvn -pl order-service -am test -Dtest="OrderServiceImplTest#createOrder_concurrent*"
```

期望：
- `OR-080 / OR-081 / OR-082` 三个 PASS（当前串行实现也能满足）
- `OR-083` FAIL：当前串行实现会把 RuntimeException 直接抛出，而不是包装为 OrderException

注意：若 OR-081 也 FAIL，说明串行下两个 mock 都 stub 但只调用了第一个，导致 Mockito 严格 mock 报告 unused stubbing。可以接受 OR-081 在并行实现下才稳定通过。本步骤的核心红信号是 OR-083 失败。

### Step 3.4 提交红测试

- [ ] 

```
git add AI-Shopping-backend/order-service/src/test/java/com/gzasc/aishopping/order/service/OrderServiceImplTest.java
git commit -m "test(order): 新增 4 个并发下单场景用例（红，等待并行实现）"
```

---
## Task 4: 并行化 createOrder（TDD - 绿）

**目的：** 实现并行化，让 4 个新用例全绿。

**Files:**
- Modify: `AI-Shopping-backend/order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java`

### Step 4.1 改造 createOrder 为并行

- [ ] 替换 `createOrder` 方法（从第 50 行 `@Override` 到第 90 行），改为：

```java
    @Override
    @Transactional
    public String createOrder(PlaceOrderRequest request, Long userId) {
        CompletableFuture<ProductDTO> productFuture =
                CompletableFuture.supplyAsync(() -> fetchProduct(request.getProductId()));
        CompletableFuture<ContactDTO> contactFuture =
                CompletableFuture.supplyAsync(() -> fetchContact(request.getContactId()));

        ProductDTO product;
        ContactDTO contact;
        try {
            CompletableFuture.allOf(productFuture, contactFuture).join();
            product = productFuture.join();
            contact = contactFuture.join();
        } catch (CompletionException e) {
            throw OrderException.unwrap(e);
        }

        BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        int stock = product.getStock() != null ? product.getStock() : 0;

        if (stock < request.getQuantity()) {
            throw new OrderException("商品库存不足，当前库存：" + stock + "（错误代码：O-005）");
        }

        Long shopIdObj = product.getShopId();
        if (shopIdObj == null) {
            throw new OrderException("获取店铺信息失败");
        }
        String shopId = String.valueOf(shopIdObj);

        String orderId = orderIdSelector.generate();
        Order order = Order.buildInitOrder(orderId, userId, shopId, String.valueOf(request.getProductId()),
                request.getQuantity(), price.multiply(BigDecimal.valueOf(request.getQuantity())));
        order.setContactId(request.getContactId());

        int result = orderMapper.insertOrder(order);
        if (result <= 0) {
            throw new OrderException("创建订单失败");
        }

        productFeignClient.reserveStock(new StockReserveRequest(orderId, Long.valueOf(order.getProductId()), request.getQuantity()));

        return orderId;
    }
```

### Step 4.2 跑全部测试

- [ ] 运行：

```
mvn -pl order-service -am test -Dtest=OrderServiceImplTest
```

期望：所有用例 PASS（现有 30+ 个 + 新增 4 个）。

### Step 4.3 提交

- [ ] 

```
git add AI-Shopping-backend/order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java
git commit -m "feat(order): createOrder 并行化 - 商品/联系人查询用 CompletableFuture 并行执行"
```

---
## Task 5: 端到端验收

**目的：** 把 order-service 整个模块的测试跑一遍，确认无回归。

### Step 5.1 跑 order-service 全部测试

- [ ] 运行：

```
mvn -pl order-service -am test
```

期望：`BUILD SUCCESS`，无失败用例。

### Step 5.2 跑 lint / 编译检查

- [ ] 运行：

```
mvn -pl order-service -am compile
```

期望：无 warning（除项目原有的 warning 外不应增加新的）。

### Step 5.3 人工审阅 diff

- [ ] 运行：

```
git log --oneline -5
git diff main...HEAD -- AI-Shopping-backend/order-service
```

确认改动范围：
- OrderException.java：仅新增 unwrap 静态方法
- OrderServiceImpl.java：仅 createOrder 方法体改变，新增 2 个 private 方法
- 测试文件：1 新增 + 1 修改

无其它无关改动。

---

## 验收清单

实施完成后，确认以下事项全部成立：

- [ ] `OrderException.unwrap(Throwable)` 静态方法存在并可被外部调用
- [ ] `OrderServiceImpl.createOrder()` 中商品查询和联系人查询通过 `CompletableFuture.allOf().join()` 并行执行
- [ ] 异常通过 `OrderException.unwrap(e)` 解包，业务异常文案保留
- [ ] `@Transactional` 注解保留
- [ ] `reserveStock` 调用保留在事务内
- [ ] 现有所有测试用例 PASS
- [ ] 新增 4 个并发场景用例 PASS
- [ ] 新增 3 个 OrderException.unwrap 单元测试 PASS
- [ ] 没有引入新的 maven 依赖
- [ ] 没有新增 @Configuration 类
- [ ] 没有新增 application.yml 配置项

---

## 范围外（明确不做，与 spec 一致）

- Sentinel 限流配置调整
- Feign 超时参数调整
- product-service 任何改动
- 任何缓存机制引入（Redis / Caffeine）
- 支付/取消/退货等其它订单状态流转链路改造
- 自定义线程池（继续使用 ForkJoinPool.commonPool）