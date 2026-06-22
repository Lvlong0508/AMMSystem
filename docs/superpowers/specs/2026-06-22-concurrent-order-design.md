# 下单流程并发化设计（最小可行版）

## 1. 目标与范围

| 项 | 内容 |
|---|---|
| **目标** | 缩短 OrderServiceImpl.createOrder() 的单次响应时间（RT） |
| **改造文件** | 仅 1 个生产代码文件：OrderServiceImpl.java |
| **不改动** | product-service、Feign 接口、预占库存模型、Redis Stream 事件链路、DB schema、application.yml |
| **不引入** | 新依赖、新框架、新配置类、新工具类、新线程池 |

### 1.1 非目标

- **不**追求提升整体吞吐 QPS（由 Sentinel 限流 + 现有连接池负责，本次不调整）
- **不**改造库存防超卖机制（product-service 现有 SELECT FOR UPDATE + 条件 UPDATE 已能扛日常大促几百 QPS）
- **不**改造支付/取消/退货等其它订单状态流转链路

## 2. 现状分析

### 2.1 当前 createOrder 调用图

OrderServiceImpl.createOrder() 当前为全串行执行：

```
[1] productFeignClient.getProductById   (Feign, ~50-200ms)
[2] contactFeignClient.getContactById   (Feign, ~30-100ms)
[3] 库存预校验 + 取 shopId               (内存, <1ms)
[4] orderIdSelector.generate()           (Redis INCR, ~1-3ms)
[5] orderMapper.insertOrder()            (DB, ~10-20ms)
[6] productFeignClient.reserveStock()    (Feign+DB, ~30-80ms)
return orderId
```

### 2.2 可并行的部分

- [1] 和 [2] 互相独立，可并行
- [3] 到 [6] 之间存在数据依赖，必须串行
- [5] 和 [6] 理论上可并行，但任一失败的补偿链复杂度会显著提升，本次不动

**结论**：唯一值得做的并行化是 [1] || [2]。

## 3. 改造方案

### 3.1 改造后调用图

```
[1] productFeignClient.getProductById --+
                                        +-- CompletableFuture.allOf().join()
[2] contactFeignClient.getContactById --+
                                        |
                                        v
                                  [3][4][5][6] 完全保持原样
```

### 3.2 执行线程

使用 CompletableFuture.supplyAsync(supplier) 的默认池 ForkJoinPool.commonPool()。

**理由**：几百 QPS 场景 commonPool 完全够用；避免新增 @Configuration 类和 application.yml 配置项；若未来撞到瓶颈，加 Executor 参数也只是单行改动。

### 3.3 异常传播

- 任一 Feign 调用失败时，在 supplier 内部抛出 OrderException
- CompletableFuture.join() 会把异常包装为 CompletionException
- **解包能力下沉到 OrderException 自身**：在 OrderException 类上新增静态方法 `OrderException.unwrap(Throwable)`，作为业务异常的"自我解包"语义
  - cause 是 OrderException -> 原样返回
  - 其它 -> 返回 new OrderException("系统繁忙，请稍后重试")
- 调用方只需 `throw OrderException.unwrap(e)`，无需在每个 Service 内重复写解包逻辑

### 3.4 业务校验保留

| 校验点 | 错误码 | 文案 |
|---|---|---|
| 商品为空 | O-003 | 商品不存在 |
| 联系人为空 | O-006 | 联系人不存在，请重新选择联系人 |
| 库存不足 | O-005 | 商品库存不足 |

### 3.5 不动的部分

- @Transactional 保留（EventPublisher.publishAfterCommit 依赖事务边界）
- reserveStock Feign 调用保留在事务内
- orderIdSelector.generate() / orderMapper.insertOrder() / EventPublisher / 状态机 CAS / OrderTimeoutTask 全部不动

## 4. 代码结构

新增 2 个 private 方法 + 改写 createOrder 前两步 + OrderException 新增静态方法。

### 4.1 createOrder 示意

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
    // 以下沿用改造前逻辑
    // ...
}
```

### 4.2 OrderServiceImpl 内辅助方法（fetchXxx 系列）

```java
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
        throw new OrderException("联系人不存在（错误代码：O-006）");
    }
    return resp.getData();
}
```

### 4.3 OrderException 内新增静态解包方法

```java
// com.gzasc.aishopping.order.exception.OrderException
public static OrderException unwrap(Throwable e) {
    Throwable cause = e.getCause();
    if (cause instanceof OrderException oe) return oe;
    return new OrderException("系统繁忙，请稍后重试");
}
```

### 4.4 可复用性

- fetchProduct / fetchContact 是显式命名的语义单元，未来并行聚合查询可直接复用 pattern
- OrderException.unwrap 是全局可复用能力，任何 Service 用 CompletableFuture 并行调用时都可以用，无须重复实现

## 5. 测试策略

修改 OrderServiceImplTest（已有），新增 4 个用例：

| 用例 | 断言 |
|---|---|
| bothSuccess_shouldCreateOrder | 都成功 -> 返回 orderId |
| productFailed_shouldThrowO003 | 商品查询 null -> 抛 O-003 |
| contactFailed_shouldThrowO006 | 联系人查询 null -> 抛 O-006 |
| bothFailed_shouldThrowOrderException | 都失败 -> 抛任一 OrderException |

测试工具：复用 Mockito + spring-boot-starter-test，不引入 embedded-redis / Testcontainers。
现有用例全部保持通过。

## 6. 文件改动清单

| 文件 | 改动类型 |
|---|---|
| order-service/.../service/impl/OrderServiceImpl.java | 修改 |
| order-service/.../exception/OrderException.java | 修改（新增 static unwrap 方法） |
| order-service/.../service/impl/OrderServiceImplTest.java | 修改 |

**共 3 个文件，0 新增、3 修改。**

## 7. 预期收益

| 指标 | 改造前 | 改造后 |
|---|---|---|
| [1]+[2] 段 RT | ~80-300ms（串行） | ~50-200ms（并行取较大者） |
| 单次 createOrder RT | 基线 | 节省约 30-100ms |
| 系统资源 | 无变化 | 无变化（commonPool 已存在） |

## 8. 风险与回滚

| 风险 | 评估 | 处理 |
|---|---|---|
| commonPool 被占满 | 低（本服务无其它 commonPool 重负载使用） | 加 Executor 参数，单行改动 |
| Feign 线程安全 | OpenFeign 客户端线程安全 | 无需处理 |
| 异常堆栈包装 | 通过 OrderException.unwrap 解包 | 无需处理 |
| 事务上下文丢失 | @Transactional 在主线程 | 无需处理 |
| 回滚 | 单文件改动 | git revert |

## 9. 与上一版（2026-06-21）对比

| 上版引入项 | 砍掉理由 |
|---|---|
| Redis Lua 扣库存 | 与预占模型冲突，product-service 现有机制已够几百 QPS |
| product-service 加 Redis | 不需要 |
| StockSyncTask 对账 | 不需要 |
| OrderThreadPoolConfig | commonPool 够用 |
| StockRedisService + Lua 脚本 | 不需要 |
| 去掉 @Transactional | 会破坏事件链路 |
| 补偿 INCRBY | 不需要 |
| Feign 超时调整 | 与本次目标无关，另行评估 |

**上版 10 个文件 -> 本版 3 个文件。**

## 10. 范围外事项

- Sentinel 限流配置调整
- Feign 超时参数调整
- product-service 任何改动
- 任何缓存机制引入（Redis / Caffeine）
- 支付/取消/退货等其它状态流转链路