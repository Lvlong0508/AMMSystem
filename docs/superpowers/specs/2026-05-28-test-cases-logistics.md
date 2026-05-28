# logistics-service 测试用例文档

> 版本: 1.0 | 日期: 2026-05-28

## 1. 概述

本文档针对 AI-Shopping 项目的 logistics-service 模块 (端口 8084, 数据库 eureka_logistics) 编写测试用例。涵盖物流记录创建、查询、删除及内部 Feign 接口的完整测试。

- **Controller**: LogisticsController (外部API) + InternalLogisticsController (Feign内部调用)
- **核心逻辑**: `createLogistics` (事务插入, useGeneratedKeys) + `getLatestLogistics` (按订单+类型排序取最新)
- **异常**: LogisticsException(400), MethodArgumentNotValidException(400), Exception(500)

## 2. 测试环境

| 环境项 | 值 |
|--------|----|
| 应用端口 | 8084 |
| 数据库 | eureka_logistics (MySQL) |
| 缓存 | N/A (目前无缓存层, 直查DB) |
| 外部依赖 | Eureka Registry |
| Mock 要求 | Service 层接口调用需 Mock DB (Mapper), Feign 调用模拟内部请求 |

## 3. 测试用例表

### 3.1 物流记录创建

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| LG-001 | 正常创建交付类型物流记录 | 数据库无重复约束冲突, 请求参数合法 | 1. 构造 CreateLogisticsRequest(orderId="ORD001", type="DELIVERY", contactId=1, trackingNumber="SF1234567890")<br>2. POST `/logistics/create`<br>3. 验证响应 | `ApiResponse` 中 code=200, data 为 LogisticsResponse 含自增 id, 各字段与入参一致, createdAt 可能为 null（INSERT 未回读数据库默认值），需在插入后显式设置 | P0 |
| LG-002 | 正常创建退货类型物流记录 | 数据库无重复约束冲突, 请求参数合法 | 1. 构造 CreateLogisticsRequest(orderId="ORD002", type="RETURN", contactId=2, trackingNumber="SF0987654321")<br>2. POST `/logistics/create`<br>3. 验证响应 | `ApiResponse` 中 code=200, data 中 type="RETURN", 其余字段正确，createdAt 可能为 null（INSERT 未回读数据库默认值） | P0 |
| LG-003 | 创建物流记录 - 缺少 orderId(@Valid 失败) | 无 | 1. 构造请求, orderId=null 或空字符串<br>2. POST `/logistics/create` | 响应 400, 错误信息提示 orderId 校验失败 | P1 |
| LG-004 | 创建物流记录 - trackingNumber 为空 | 无 | 1. 构造请求, trackingNumber=null 或空<br>2. POST `/logistics/create` | 响应 400, 错误信息提示 trackingNumber 校验失败 | P1 |
| LG-005 | 创建物流记录 - 无效 type 枚举 | 无 | 1. 构造请求, type="INVALID"<br>2. POST `/logistics/create` | type 为纯字符串，无枚举校验，无效值不会被 API 层拒绝，直接写入数据库 | P1 |

### 3.2 物流记录查询

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| LG-006 | 查询全部物流列表 | 数据库中存在 ≥2 条记录 | 1. GET `/logistics/list`<br>2. 验证响应 | `ApiResponse` 中 data 为 List, 大小 ≥2, 每条含完整 LogisticsResponse 字段 | P0 |
| LG-007 | 按运单号查询物流记录 | 数据库中存在运单号 "SF1234567890" 的记录 | 1. GET `/logistics/search/tracking?trackingNumber=SF1234567890`<br>2. 验证响应 | `ApiResponse` 中 data.trackingNumber = "SF1234567890", 其余字段正确 | P0 |
| LG-008 | 按订单ID查询物流列表 | 订单 "ORD001" 存在 ≥2 条物流记录 | 1. GET `/logistics/order/ORD001`<br>2. 验证响应 | 返回 List\<LogisticsResponse\>, 所有记录的 orderId = "ORD001" | P0 |
| LG-009 | 查询订单最新物流记录 | 订单 "ORD001" 存在多条记录, type="DELIVERY" | 1. GET `/logistics/order/ORD001/latest?type=DELIVERY`<br>2. 验证响应 | 返回单条 LogisticsResponse, createdAt 为该订单+类型下最新时间 | P0 |
| LG-010 | 查询订单退货类型最新物流 | 订单 "ORD002" 存在多条 RETURN 类型记录 | 1. GET `/logistics/order/ORD002/latest?type=RETURN`<br>2. 验证响应 | 返回单条 LogisticsResponse, type="RETURN", createdAt 最新 | P1 |
| LG-011 | 按不存在的运单号查询 | 数据库中无该运单号 | 1. GET `/logistics/search/tracking?trackingNumber=NONEXIST` | 响应 400, 抛出 LogisticsException, 提示信息为"物流信息不存在" | P1 |
| LG-012 | 查询不存在的物流记录(按ID删除后查) | 该 ID 不存在 | 1. GET `/logistics/order/ORD999` (不存在的订单) | 响应 400, 抛出 LogisticsException, 提示信息为"物流信息不存在" | P1 |
| LG-013 | 查询最新物流 - 不存在的类型组合 | 订单存在但 type 不匹配 | 1. GET `/logistics/order/ORD001/latest?type=RETURN` (该订单无 RETURN 记录) | 响应 400, 抛出 LogisticsException | P1 |
| LG-014 | 查询全部物流列表 - 空数据 | 数据库内 logistics 表无记录 | 1. GET `/logistics/list` | 返回空 List, code=200 | P1 |

### 3.3 物流记录删除

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| LG-015 | 删除存在的物流记录 | 数据库中存在 id=1 的记录 | 1. DELETE `/logistics/delete/1`<br>2. 再次查询 id=1 确认已删除 | 响应 ApiResponse, code=200, data=null; 数据库中该记录已不存在 | P0 |
| LG-016 | 删除不存在的物流记录 | id=9999 不存在 | 1. DELETE `/logistics/delete/9999` | 响应 400, 抛出 LogisticsException | P1 |

### 3.4 内部接口 (Feign)

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| LG-017 | 内部创建物流记录 | 合法请求参数 | 1. 构造 LogisticsRequest(orderId="ORD003", type="DELIVERY", ...)<br>2. POST `/internal/logistics/create` | 返回 ApiResponse, data 含自增 id, 字段与入参一致，createdAt 可能为 null（INSERT 未回读数据库默认值） | P0 |
| LG-018 | 内部查询订单物流列表 | 订单 "ORD003" 存在记录 | 1. GET `/internal/logistics/order/ORD003` | 返回 List\<LogisticsResponse\>, 所有 orderId = "ORD003" | P0 |
| LG-019 | 内部查询订单最新物流 | 订单 "ORD003" 存在多条 DELIVERY 记录 | 1. GET `/internal/logistics/order/ORD003/latest?type=DELIVERY` | 返回单条最新 LogisticsResponse | P0 |
| LG-020 | 内部查询不存在的订单 | 订单 "ORD_NOT_EXIST" 无记录 | 1. GET `/internal/logistics/order/ORD_NOT_EXIST` | 响应 400, 抛出 LogisticsException | P1 |

### 3.5 异常处理

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| LG-021 | @Valid 校验失败返回 400 | 请求体缺少必填字段 (orderId 为空) | 1. POST `/logistics/create` 传空 orderId<br>2. 断言响应 | HTTP Status = 400, 错误信息包含字段名及校验描述 | P1 |
| LG-022 | LogisticsException 返回 400 | 查询不存在的运单号 | 1. GET `/logistics/search/tracking?trackingNumber=UNKNOWN` | HTTP Status = 400, 消息体包含 "LogisticsException" 或自定义错误码 | P1 |
| LG-023 | 未预期异常返回 500 | Mapper 抛出 RuntimeException (如 DB 连接失败) | 1. Mock Mapper 抛出异常<br>2. 调用任意 API | HTTP Status = 500, 不暴露堆栈细节 | P2 |
| LG-024 | 创建重复运单号 (唯一约束 假设场景) | DB 层配置了 trackingNumber 唯一约束 | 1. 先创建 trackingNumber="DUPLICATE" 的记录<br>2. 再次创建相同 trackingNumber | 响应 500 (数据层异常), 或取决于 DB 约束定义 | P2 |

## 4. 测试要点总结

### 4.1 覆盖情况

| 模块 | 用例数 | 覆盖范围 |
|------|--------|----------|
| 物流记录创建 | 5 (LG-001 ~ LG-005) | 正常创建 DELIVERY/RETURN + 3 条入参校验异常 |
| 物流记录查询 | 9 (LG-006 ~ LG-014) | 全部列表/运单号/订单ID/最新物流 + 4 条不存在场景 |
| 物流记录删除 | 2 (LG-015 ~ LG-016) | 删除存在 + 删除不存在 |
| 内部接口 | 4 (LG-017 ~ LG-020) | 创建 + 查询列表 + 最新物流 + 不存在 |
| 异常处理 | 4 (LG-021 ~ LG-024) | @Valid / LogisticsException / 500 / 约束冲突 |
| **合计** | **24** | |

### 4.2 优先级分布

- **P0**: LG-001, LG-002, LG-006, LG-007, LG-008, LG-009, LG-015, LG-017, LG-018, LG-019 (10 条)
- **P1**: LG-003, LG-004, LG-005, LG-010, LG-011, LG-012, LG-013, LG-014, LG-016, LG-020, LG-021, LG-022 (12 条)
- **P2**: LG-023, LG-024 (2 条)

### 4.3 边界与异常场景

1. **空数据库查询**: 当 logistics 表无记录时, `/list` 应返回空列表而非异常 (LG-014)
2. **枚举值校验**: type 字段只接受 DELIVERY/RETURN, 传入无效值应被拒绝 (LG-005)
3. **日志不存在**: 按 ID/运单号/订单号查询不存在数据, 统一响应 400 LogisticsException (LG-011, LG-012, LG-013)
4. **最新记录排序**: `getLatestLogistics` 按 ORDER BY created_at DESC LIMIT 1, 测试需构造多条记录验证先后顺序 (LG-009, LG-010)
5. **事务回滚**: `createLogistics` 标注 `@Transactional`, 若 insert 后抛出运行时异常需回滚
