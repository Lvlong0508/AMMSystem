# Logistics 服务 API 接口集成测试报告

## 1. 测试概述

| 项目 | 内容 |
|------|------|
| 测试目标 | 验证 Logistics 服务全部 REST API 端点的功能正确性和容错能力 |
| 测试类型 | API 接口集成测试（端到端） |
| 测试日期 | 2026-06-01 |
| 测试工具 | PowerShell Invoke-RestMethod / Invoke-WebRequest |

## 2. 测试环境

| 组件 | 地址 | 状态 |
|------|------|:----:|
| MySQL | localhost:3306 | ✅ 运行中 |
| Logistics Service | http://localhost:8084 | ✅ 运行中 |

### 路由链路

```
Client
  → GET/POST http://localhost:8084/logistics/*
    → Logistics Controller → Service → Mapper → MySQL
```

## 3. 测试用例及结果

### 3.1 外部 API (/logistics/**)

| # | 用例 | 方法 | 端点 | 预期结果 | 实际结果 | 状态 |
|---|------|------|------|----------|----------|:----:|
| 1 | 创建物流（正常） | POST | /logistics/create | 200 + 创建物流信息成功 | code=200, message=创建物流信息成功 | ✅ |
| 2 | 创建物流-缺少orderId | POST | /logistics/create | 400 + 订单号不能为空 | code=400, message=订单号不能为空 | ✅ |
| 3 | 创建物流-缺少contactId | POST | /logistics/create | 400 + 联系人ID不能为空 | code=400, message=联系人ID不能为空 | ✅ |
| 4 | 查询所有物流列表 | GET | /logistics/list | 200 + 物流列表 | code=200, message=查询成功 | ✅ |
| 5 | 按运单号查询 | GET | /logistics/search/tracking?trackingNumber= | 200 + 匹配物流信息 | code=200, message=查询成功 | ✅ |
| 6 | 按不存在运单号查询（容错） | GET | /logistics/search/tracking?trackingNumber= | 400 + 物流信息不存在 | code=400, message=物流信息不存在 | ✅ |
| 7 | 按订单号查询物流列表 | GET | /logistics/order/{orderId} | 200 + 物流列表 | code=200, message=查询成功 | ✅ |
| 8 | 按订单号+类型查询最新物流 | GET | /logistics/order/{orderId}/latest?type= | 200 + 最新物流信息 | code=200, message=查询成功 | ✅ |
| 9 | 查询不存在订单的物流（容错） | GET | /logistics/order/{orderId}/latest?type= | 400 + 物流信息不存在 | code=400, message=物流信息不存在 | ✅ |
| 10 | 删除物流信息 | DELETE | /logistics/delete/{id} | 200 + 删除物流信息成功 | code=200, message=删除物流信息成功 | ✅ |
| 11 | 删除不存在物流信息（容错） | DELETE | /logistics/delete/{id} | 400 + 物流信息不存在 | code=400, message=物流信息不存在 | ✅ |

### 3.2 内部 API (/internal/logistics/**)

| # | 用例 | 方法 | 端点 | 预期结果 | 实际结果 | 状态 |
|---|------|------|------|----------|----------|:----:|
| 12 | 内部创建物流 | POST | /internal/logistics/create | 200 + 创建物流信息成功 | code=200, message=创建物流信息成功 | ✅ |
| 13 | 内部按订单号查询 | GET | /internal/logistics/order/{orderId} | 200 + 物流列表 | code=200, message=查询成功 | ✅ |
| 14 | 内部查询最新物流 | GET | /internal/logistics/order/{orderId}/latest?type= | 200 + 最新物流信息 | code=200, message=查询成功 | ✅ |

## 4. 测试结果统计

| 维度 | 数值 |
|------|:----:|
| 总用例数 | 14 |
| 通过 | 14 |
| 失败 | 0 |
| 通过率 | **100%** |

## 5. 关键验证点分析

### 5.1 创建物流流程

- 创建物流时 type 默认值为 DELIVERY
- 创建成功后返回完整的 LogisticsResponse（含自增 id 和 created_at）
- 内部 API 和外部 API 功能对等，使用不同的 DTO 对象（外部用 CreateLogisticsRequest 带 @Valid 校验，内部用 LogisticsRequest 无校验）

### 5.2 查询机制

- 按运单号查询可精确定位单条记录
- 按订单号查询返回该订单下的所有物流记录，按 created_at 倒序
- 按订单号+类型查询最新物流使用 LIMIT 1

### 5.3 参数校验

- orderId 必填（@NotBlank），缺失返回 400 + "订单号不能为空"
- orderId 长度限制（@Size(max=20)），超过 20 字符返回 400 + "订单号长度不能超过20个字符"
- contactId 必填（@NotNull），缺失返回 400 + "联系人ID不能为空"
- trackingNumber 必填（@NotBlank），缺失返回 400 + "运单号不能为空"
- type 可选，不传时默认 DELIVERY

### 5.4 容错处理

- 查询不存在的运单号 → 400 "物流信息不存在"
- 查询不存在的订单号最新物流 → 400 "物流信息不存在"
- 删除不存在的物流信息 → 400 "物流信息不存在"

## 6. 已有关联的单测覆盖

| 模块 | 测试文件 | 测试数 | 覆盖范围 |
|------|----------|:------:|----------|
| 外部 API Controller | controller/LogisticsControllerTest.java | 22 | 创建/查询/删除/参数校验/orderId超长校验 |
| 内部 API Controller | controller/InternalLogisticsControllerTest.java | 7 | 内部创建/查询 |
| Service 实现 | service/impl/LogisticsServiceImplTest.java | 18 | 创建/查询/删除 |
| Mapper | mapper/LogisticsMapperTest.java | 14 | SQL CRUD + createdAt 回读验证 |

## 7. 结论

Logistics 服务全部 14 个 API 端点（11 个外部 API + 3 个内部 API）集成测试通过，通过率 100%。
核心流程（创建 → 查询 → 删除）完整闭环，参数校验和容错处理符合预期，外部 API 和内部 API 功能对等。

## 8. 本次变更记录

### 8.1 createdAt 响应缺失修复

**问题：** 创建物流后响应中 `createdAt` 始终为 `null`。

**根因：** `LogisticsMapper.insertLogistics()` 使用 `@Options(useGeneratedKeys = true)` 仅回写自增主键 id，未回读 DB 默认生成的 `created_at`。

**修复：** 将 `@Options` + `@SelectKey` 合并为单一 `@SelectKey`，insert 后执行 `SELECT LAST_INSERT_ID() AS id, created_at FROM logistics WHERE id = LAST_INSERT_ID()`，同时回填 `id` 和 `createdAt`。

**变更文件：**

| 文件 | 变更 |
|------|------|
| `mapper/LogisticsMapper.java:11-16` | `@Options` 替换为 `@SelectKey`，回读 id + createdAt |
| `mapper/LogisticsMapperTest.java:37` | 新增 `assertThat(logistics.getCreatedAt()).isNotNull()` |

### 8.2 order_id 字段长度校验修复

**问题：** `order_id` 数据库字段 `varchar(20)`，服务端无长度校验，传入超过 20 字符时 MyBatis 抛出 DataTruncation 异常，返回 500。

**分析：** orderId 由 `RedisOrderIdGenerator` 生成，格式为 `yyyyMMdd`（8位）+ 5位序列号 + 5位随机字母 = 18 位，正常业务流程不会超过 20 字符。但 API 入口缺少防御性校验。

**修复：** 在 `CreateLogisticsRequest.orderId` 上增加 `@Size(max = 20)` 注解。

**变更文件：**

| 文件 | 变更 |
|------|------|
| `dto/CreateLogisticsRequest.java:10` | 新增 `@Size(max = 20)` 校验 |
| `controller/LogisticsControllerTest.java:109-123` | 新增超长 orderId 校验测试用例 |

### 8.3 验证结果

| 修复项 | 修复前 | 修复后 | 验证方式 |
|--------|--------|--------|----------|
| createdAt 响应缺失 | `"createdAt": null` | `"createdAt": "2026-06-01T11:23:04.000+00:00"` | 端到端 HTTP 请求 |
| orderId 超长校验 | 500 "系统错误" | 400 "订单号长度不能超过20个字符" | 端到端 HTTP 请求 |
| 全量单测 | - | 61/61 通过 | mvn test |
