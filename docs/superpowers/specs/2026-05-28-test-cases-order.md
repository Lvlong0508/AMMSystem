# order-service 测试用例文档

> 版本: 1.0 | 日期: 2026-05-28

## 1. 概述

本文档覆盖 order-service（端口 8082，数据库 eureka_order）的全部核心业务逻辑，包括下单、支付、取消、发货、收货、退货、删除等正向/逆向流程，以及 Redis Stream 事件消费、超时自动取消、订单号生成、状态机合法性等基础设施能力。

**权限模型：**
- `X-User-Id` 请求头标识当前登录用户（所有用户端接口强制校验）。
- 商家端接口还需 `shopId` 路径参数，商家只能操作自己店铺的订单。
- 内部接口同样需要 X-User-Id 请求头，并过滤为仅返回该用户数据。

**测试分级：**
- **P0** - 核心流程（下单→支付→发货→收货），必须每轮回归通过。
- **P1** - 重要特性（取消、退货、删除、事件消费等核心业务分支）。
- **P2** - 边缘场景与异常（边界条件、非法状态转换、幂等、并发等）。

---

## 2. 测试环境

| 项目 | 说明 |
|------|------|
| 服务端口 | 8082 |
| 数据库 | eureka_order (MySQL) |
| Redis | 订单号生成、预占库存缓存、Stream 事件、幂等控制 |
| 依赖服务 | product-service (Feign)、logistics-service (Feign)、contact-service (Feign) |
| 注册中心 | Eureka Server |
| Mock 策略 | 单元测试 Mock Feign 客户端；集成测试启动嵌入式 Redis + H2 |

---

## 3. 测试用例表

### 3.1 下单流程

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| OR-001 | 正常下单 - 所有参数合法 | 商品存在且库存充足、联系地址存在 | 1. Mock 商品库存查询返回可用<br>2. 调用 POST /api/user/order/place (body: productId=1, quantity=2, contactId=1, header: X-User-Id=100)<br>3. 验证数据库订单记录 | 1. 订单创建成功，返回 orderId<br>2. orderId 格式符合 yyyyMMdd+5位INCR+5位字母<br>3. 订单状态为 PENDING<br>4. 商品预占库存 Feign 被调用一次 | P0 |
| OR-002 | 下单 - 商品不存在 | 商品 ID 在 product-service 中查不到 | 1. Mock getProductById 返回 null<br>2. 调用 POST /api/user/order/place | 1. 返回 4xx 错误<br>2. 订单未写入数据库<br>3. 库存未发生任何变动 | P1 |
| OR-003 | 下单 - 库存不足 | 商品库存 < 请求数量 | 1. Mock getProductById 返回 stock=1<br>2. 下单 quantity=3 | 1. 返回 4xx 错误，提示库存不足<br>2. 订单未创建 | P1 |
| OR-004 | 下单 - 联系地址不存在 | contactId 对应用户无此地址 | 1. 调用下单接口（代码未校验 contactId 有效性） | 1. 订单仍创建成功（当前代码不验证 contactId 存在性） | P1 |
| OR-005 | 下单 - 预占库存后下游失败 | 预占库存 Feign 调用抛出异常 | 1. Mock reserveStock 抛出 RuntimeException<br>2. 调用下单接口 | 1. 订单已写入数据库（状态 PENDING）<br>2. 事务已提交，需靠补偿机制<br>3. 接口返回 500 但订单号已生成 | P2 |
| OR-006 | 下单 - 数量为0或负数 | quantity=0 | 1. @Valid 校验触发 | 1. 返回 400 Bad Request<br>2. 校验错误信息提示 quantity 非法 | P2 |
| OR-007 | 下单 - X-User-Id 为空 | 请求不携带 X-User-Id 头 | 1. 调用下单接口 | 1. 返回 500 服务器内部错误（MissingRequestHeaderException 被全局异常处理器兜底） | P1 |

---

### 3.2 订单支付

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| OR-008 | 正常支付 - PENDING→PAID | 订单状态为 PENDING，订单属于当前用户 | 1. PUT /api/user/order/{orderId}/pay (X-User-Id)<br>2. 查询订单状态 | 1. 返回 200<br>2. 订单状态变为 PAID<br>3. Redis Stream 发送 STOCK_CONFIRM 事件 | P0 |
| OR-009 | 支付 - 订单不属于当前用户 | 订单 userId ≠ X-User-Id | 1. 用其他用户 ID 调用支付接口 | 1. 返回 403/404 错误 | P1 |
| OR-010 | 支付 - 状态不是 PENDING | 订单状态为 PAID 或 CANCELLED | 1. 对 PAID 订单再次调用支付 | 1. CAS 更新失败<br>2. 返回 400，提示状态不合法<br>3. 订单状态不变 | P1 |
| OR-011 | 支付 - 订单不存在 | 传入不存在的 orderId | 1. 调用支付接口 | 1. 返回 400 错误（OrderException @ResponseStatus BAD_REQUEST） | P1 |
| OR-012 | 支付成功后 STOCK_CONFIRM 事件消费 | 订单已 PAID，Redis Stream 中有 STOCK_CONFIRM | 1. 触发 Stream 消费<br>2. 检查幂等逻辑 | 1. productFeignClient.confirmReservation 被调用<br>2. 重复消费时幂等跳过（检查订单状态）<br>3. 不重复扣减 | P0 |
| OR-013 | 支付 - 并发重复支付 | 两线程同时支付同一订单 | 1. 并发调用 PUT pay 两次 | 1. CAS 保证仅一个成功<br>2. 订单状态为 PAID<br>3. 仅发出一条 STOCK_CONFIRM | P1 |

---

### 3.3 订单取消

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| OR-014 | 取消 PENDING 订单 | 订单状态 PENDING，属于当前用户 | 1. PUT /api/user/order/{orderId}/cancel<br>2. 查询订单 | 1. 状态变为 CANCELLED<br>2. 预占库存（releaseReservation）被释放 | P0 |
| OR-015 | 取消 PAID 订单 | 订单状态 PAID | 1. 调用取消接口 | 1. 状态变为 CANCELLED<br>2. 实际库存（restoreStock）被恢复 | P0 |
| OR-016 | 取消 - 不允许的状态 | 订单状态 SHIPPED/DELIVERED/RETURNING | 1. 对 SHIPPED 订单调用取消 | 1. CAS 失败<br>2. 返回 400 状态不合法<br>3. 订单状态不变 | P1 |
| OR-017 | 取消 - 订单不属于当前用户 | 订单 userId ≠ X-User-Id | 1. 用其他用户 ID 取消 | 1. 返回 403/404 | P1 |
| OR-018 | 取消 - 订单不存在 | orderId 不存在 | 1. 调用取消接口 | 1. 返回 400（OrderException @ResponseStatus BAD_REQUEST） | P2 |

---

### 3.4 发货（商家端）

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| OR-019 | 正常发货 - PAID→SHIPPED | 订单 PAID，商家属于该店铺 | 1. PUT /api/seller/order/{orderId}/ship?shopId={shopId} (body: {trackingNumber, contactId}) — shopId 为 QueryParam，body 含 contactId 必填字段<br>2. 查询订单 | 1. 返回 200<br>2. 状态变为 SHIPPED<br>3. Redis Stream 发送 LOGISTICS_CREATE<br>4. 订单 trackingNumber 更新 | P0 |
| OR-020 | 发货 - 非 PAID 状态 | 订单状态 PENDING/SHIPPED | 1. 对 PENDING 订单调用发货 | 1. CAS 失败<br>2. 返回 400 | P1 |
| OR-021 | 发货 - 商家不属于该店铺 | shopId 与订单 shopId 不匹配 | 1. 用错误 shopId 调用 | 1. 返回 403/404 | P1 |
| OR-022 | 发货 - trackingNumber 或 contactId 为空 | Body 中 trackingNumber 为空字符串或 contactId 为 null | 1. @Valid 校验触发（ShipOrderRequest 中 trackingNumber @NotBlank, contactId @NotNull） | 1. 返回 400 | P2 |
| OR-023 | 发货后 LOGISTICS_CREATE 事件消费 | 订单已 SHIPPED | 1. 触发 Stream 消费<br>2. 检查幂等逻辑 | 1. logisticsFeignClient.createLogistics 被调用<br>2. 重复消费时幂等（查是否已有物流记录） | P1 |
| OR-024 | 发货 - Feign 物流创建失败后重试 | createLogistics 抛出异常 | 1. FileFallbackDaemon 记录到文件<br>2. @Scheduled 定时重试 | 1. 事件写入本地文件<br>2. 60s 后重试成功时正常创建物流 | P2 |

---

### 3.5 确认收货

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| OR-025 | 正常确认收货 - SHIPPED→DELIVERED | 订单 SHIPPED，属于当前用户 | 1. PUT /api/user/order/{orderId}/deliver<br>2. 查询订单 | 1. 返回 200<br>2. 状态变为 DELIVERED | P0 |
| OR-026 | 确认收货 - 非 SHIPPED 状态 | 订单状态 PAID/DELIVERED | 1. 对 PAID 订单调用 | 1. CAS 失败<br>2. 返回 400 | P1 |
| OR-027 | 确认收货 - 订单不属于当前用户 | 订单 userId ≠ X-User-Id | 1. 错误用户 ID 调用 | 1. 返回 403/404 | P1 |

---

### 3.6 退货流程（申请→审核→确认）

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| OR-028 | 申请退货 - SHIPPED→RETURN_PENDING | 订单 SHIPPED，属于当前用户 | 1. POST /api/user/order/{orderId}/return-request<br>2. 查询订单 | 1. 返回 200<br>2. 状态变为 RETURN_PENDING | P1 |
| OR-029 | 申请退货 - DELIVERED→RETURN_PENDING | 订单 DELIVERED | 1. 调用申请退货 | 1. 返回 200<br>2. 状态变为 RETURN_PENDING | P1 |
| OR-030 | 申请退货 - 不支持的状态 | 订单 PAID/PENDING/RETURNING | 1. 对 PAID 订单调用 | 1. CAS 失败<br>2. 返回 400 | P1 |
| OR-031 | 审核退货 - RETURN_PENDING→RETURNING | 订单 RETURN_PENDING，商家有权限 | 1. PUT /api/seller/order/{orderId}/approve-return (含 shopId)<br>2. 查询订单 | 1. 返回 200<br>2. 状态变为 RETURNING | P1 |
| OR-032 | 审核退货 - 非 RETURN_PENDING | 订单 PAID 或其他状态 | 1. 调用审核接口 | 1. 返回 400 | P2 |
| OR-033 | 确认退货 - RETURNING→RETURNED | 订单 RETURNING，商家有权限 | 1. PUT /api/seller/order/{orderId}/confirm-return (含 shopId)<br>2. 查询订单 | 1. 返回 200<br>2. 状态变为 RETURNED<br>3. Redis Stream 发送 STOCK_RESTORE | P1 |
| OR-034 | 确认退货后 STOCK_RESTORE 消费 | 订单 RETURNED | 1. 触发 Stream 消费<br>2. 检查幂等逻辑 | 1. productFeignClient.restoreStock 被调用<br>2. Redis SET NX 7天保证幂等<br>3. 重复消费不重复恢复 | P1 |
| OR-035 | 退货全流程 - SHIPPED→RETURNED | 订单 SHIPPED，商家用户均正常 | 1. 申请退货→审核→确认退货<br>2. 验证每一步状态 | 1. 状态严格按 SHIPPED→RETURN_PENDING→RETURNING→RETURNED 流转<br>2. 最终库存恢复 | P1 |

---

### 3.7 订单删除

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| OR-036 | 删除 DELIVERED 订单 | 订单 DELIVERED，属于当前用户 | 1. DELETE /api/user/order/{orderId}<br>2. 查询 deleted_orders 表 | 1. 返回 200<br>2. 原始订单从 orders 表删除<br>3. deleted_orders 表有备份记录 | P1 |
| OR-037 | 删除 CANCELLED 订单 | 订单 CANCELLED | 1. 调用删除接口 | 1. 同上，备份后删除 | P1 |
| OR-038 | 删除 - 不允许的状态 | 订单 PAID/SHIPPED/RETURNING | 1. 对 PAID 订单调用删除 | 1. 返回 400，提示不允许删除 | P1 |
| OR-039 | 删除 - 订单不属于当前用户 | 订单 userId ≠ X-User-Id | 1. 错误用户 ID 调用 | 1. 返回 403/404 | P2 |

---

### 3.8 Redis Stream 事件

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| OR-040 | STOCK_CONFIRM - 正常消费 | 订单 PAID，库存预占存在 | 1. 模拟 Stream 消息到达<br>2. 消费逻辑执行 | 1. productFeignClient.confirmReservation 被调用<br>2. 订单状态未被修改 | P0 |
| OR-041 | STOCK_CONFIRM - 订单非 PAID 时释放预占 | 订单已被取消变为 CANCELLED | 1. 发送 STOCK_CONFIRM<br>2. 消费逻辑执行 | 1. 检查订单状态非 PAID<br>2. 调用 releaseReservation 释放预占，不调用 confirmReservation | P1 |
| OR-042 | STOCK_RESTORE - 幂等控制 | 同一消息消费两次 | 1. 第一次消费正常<br>2. Redis SET NX key 已存在<br>3. 第二次消费 | 1. 第一次 restoreStock 调用<br>2. 第二次因 key 存在直接跳过 | P1 |
| OR-043 | LOGISTICS_CREATE - 正常消费 | 订单 SHIPPED | 1. 消费 Stream 消息<br>2. 查物流 Feign | 1. createLogistics 被调用<br>2. 物流记录创建成功 | P0 |
| OR-044 | LOGISTICS_CREATE - 已有物流则跳过 | 该订单物流已创建 | 1. 消费消息<br>2. 查 getLatestLogistics 已有记录 | 1. 幂等跳过，不重复创建 | P1 |
| OR-045 | FileFallbackDaemon - 发送失败兜底 | Redis 不可用，消息发送失败 | 1. 支付/发货时 Redis 异常<br>2. 降级写入本地文件<br>3. 60s 定时任务扫描文件 | 1. 文件内容包含事件 JSON<br>2. 60s 后重试发送到 Stream<br>3. 发送成功后删除文件 | P2 |

---

### 3.9 订单查询（用户/商家/内部）

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| OR-046 | 用户查询订单列表 | 当前用户有多个订单（含不同状态） | 1. GET /api/user/order/list (X-User-Id)<br>2. 检查返回 | 1. 返回 200<br>2. 仅返回该用户订单<br>3. List<OrderAbstractUserDTO> 包含正确字段<br>4. 结果按时间倒序 | P0 |
| OR-047 | 用户查询订单详情 | 订单存在且属于当前用户 | 1. GET /api/user/order/{orderId} (X-User-Id)<br>2. 检查 DTO 字段 | 1. 返回 OrderDetailDTO<br>2. 包含 contactName/phone/address/trackingNumber<br>3. 字段值与数据库一致 | P0 |
| OR-048 | 用户查询详情 - 订单不属于当前用户 | 订单 userId ≠ X-User-Id | 1. 调用详情接口 | 1. 返回 403/404<br>2. 不可越权查看 | P1 |
| OR-049 | 用户查询详情 - 订单已删除 | 订单已被删除 | 1. 调用详情接口 | 1. 返回 400（OrderException @ResponseStatus BAD_REQUEST） | P1 |
| OR-050 | 商家查询店铺订单列表 | 店铺有多个订单 | 1. GET /api/seller/order/shop/{shopId}/list<br>2. 检查返回 | 1. 返回 List<OrderAbstractSellerDTO><br>2. 仅返回该店铺订单 | P0 |
| OR-051 | 商家查询订单详情 | 订单存在且属于该店铺 | 1. GET /api/seller/order/shop/{shopId}/{orderId}<br>2. 检查 DTO | 1. 返回 OrderDetailDTO<br>2. 字段完整 | P1 |
| OR-052 | 商家查询详情 - 订单不属于该店铺 | 订单 shopId ≠ 请求 shopId | 1. 错误 shopId 调用 | 1. 返回 403/404 | P1 |
| OR-053 | 内部查询订单详情 | 订单存在 | 1. GET /internal/order/{orderId} (header: X-User-Id) | 1. 返回 OrderDetailDTO<br>2. 仍需 X-User-Id 头<br>3. 仅返回属于该用户的订单 | P1 |
| OR-054 | 内部查询订单列表 | 有订单数据 | 1. GET /internal/order/list (header: X-User-Id) | 1. 返回 List<OrderAbstractUserDTO><br>2. 仍需 X-User-Id 头<br>3. 仅返回属于该用户的订单 | P1 |
| OR-055 | 查询空列表 | 用户/店铺无订单 | 1. 调用查询列表接口 | 1. 返回 200<br>2. 返回空数组 | P2 |

---

### 3.10 超时自动取消（定时任务）

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| OR-056 | 超时取消 - PENDING 超过30分钟 | 订单创建时间 > 30min，状态仍 PENDING | 1. 等待 @Scheduled 触发（60s周期）<br>2. 查询订单 | 1. 订单状态变为 CANCELLED<br>2. 预占库存被释放 | P0 |
| OR-057 | 超时取消 - 30分钟内不取消 | 订单创建 < 30min | 1. 定时任务执行<br>2. 查询订单 | 1. 订单状态保持 PENDING<br>2. 无库存操作 | P0 |
| OR-058 | 超时取消 - 已支付订单不受影响 | 订单 PAID，已过30分钟 | 1. 定时任务执行<br>2. 查询订单 | 1. 订单状态保持 PAID<br>2. 不会被取消 | P1 |
| OR-059 | 超时取消 - 批量场景 | 多个 PENDING 超时订单 | 1. 定时任务执行<br>2. 批量检查 | 1. 所有超时 PENDING 订单均被取消<br>2. 未超时的不受影响 | P1 |

---

### 3.11 订单号生成

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| OR-060 | 订单号格式验证 | 正常下单 | 1. 获取返回的 orderId | 1. 格式: yyyyMMdd + 5位数字 + 5位随机字母<br>2. 长度 18 位<br>3. 日期部分为当前日期 | P1 |
| OR-061 | 订单号递增 | 连续生成两个订单号 | 1. 同时创建两个订单<br>2. 比较 orderId | 1. 日期部分相同<br>2. 5位 INCR 部分递增 | P1 |
| OR-062 | Redis INCR key 过期 | key order:seq:yyyyMMdd 过期 | 1. 跨天测试<br>2. Mock Redis 返回 null 触发重新初始化 | 1. 新 key 创建<br>2. INCR 从 1 开始<br>3. key 设置 24h 过期 | P2 |

---

### 3.12 状态机合法性

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| OR-063 | 非法状态转换 - DELIVERED→PAID | 订单 DELIVERED | 1. 尝试通过内部接口或直接 SQL 变更状态回 PAID | 1. CAS updateOrderStatusCas 不允许逆向<br>2. 更新行数为 0 | P2 |
| OR-064 | 非法状态转换表验证 | 每种不合法转换 | 1. 对全部状态两两组合<br>2. 调用 updateOrderStatusCas | 1. 仅允许状态机中定义的转换<br>2. 其他组合全部返回 0 | P2 |
| OR-065 | CAS 乐观锁冲突 | 两线程同时更新同一订单 | 1. 同时调用支付和取消<br>2. 查询最终状态 | 1. 仅一个操作成功<br>2. CAS 版本号一致性保证<br>3. 不会出现状态覆盖 | P1 |

---

## 4. 测试要点总结

| 维度 | 要点 |
|------|------|
| **核心流程 (P0)** | 下单→支付→发货→收货 端到端验证；超时自动取消；用户/商家查询列表与详情；所有 Feign 接口调用验证 |
| **CAS 乐观锁** | 所有状态变更使用 `updateOrderStatusCas(oldStatus, newStatus)`，必须验证并发下不会出现状态覆盖 |
| **幂等性** | Redis Stream 的三个事件（STOCK_CONFIRM、STOCK_RESTORE、LOGISTICS_CREATE）都必须幂等，重复消费不产生副作用 |
| **权限控制** | X-User-Id 校验用户/内部接口的订单归属；shopId 校验商家的店铺归属 |
| **状态机** | 严格按定义的状态转换表执行，任何未定义的转换必须被 CAS 拒绝 |
| **异常回滚** | 下单预占库存失败时订单已入库，需注意数据一致性；FileFallbackDaemon 兜底保证事件最终送达 |
| **删除备份** | 删除订单前必须备份到 `deleted_orders` 表，确保数据可追溯 |
| **订单号** | 日期前缀+自增ID+随机字母，保证全局唯一且趋势递增 |
