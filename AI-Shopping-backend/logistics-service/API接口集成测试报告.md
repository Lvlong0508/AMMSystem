# Logistics 服务测试报告（2026-06-04，实际验证）

## 单元测试

| 测试类 | 用例数 | 通过 |
|--------|:------:|:----:|
| LogisticsControllerTest | 23 | 23 |
| InternalLogisticsControllerTest | 7 | 7 |
| LogisticsServiceImplTest | 19 | 19 |
| LogisticsMapperTest | 14 | 14 |
| **合计** | **63** | **63（100%）** |

## API 端到端集成测试

直接测试 Logistics 端口 8084，覆盖创建/查询/删除/内部 API/异常处理/Gateway 路由。

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| 1 | 创建 DELIVERY | POST | `/logistics/create` | ✅ 200 |
| 2 | 创建 RETURN | POST | `/logistics/create` | ✅ 200 |
| 3 | type 为空（默认 DELIVERY） | POST | `/logistics/create` | ✅ 200 |
| 4 | type=INVALID（无校验直写） | POST | `/logistics/create` | ✅ 200 |
| 5 | orderId 为空 | POST | `/logistics/create` | ✅ 400 |
| 6 | orderId 为 null | POST | `/logistics/create` | ✅ 400 |
| 7 | orderId 超过20字符 | POST | `/logistics/create` | ✅ 400 |
| 8 | trackingNumber 为空 | POST | `/logistics/create` | ✅ 400 |
| 9 | trackingNumber 为 null | POST | `/logistics/create` | ✅ 400 |
| 10 | contactId 为 null | POST | `/logistics/create` | ✅ 400 |
| 11 | 无效 JSON 体 | POST | `/logistics/create` | ⚠ **500（非 400）** |
| 12 | 查询全部列表 | GET | `/logistics/list` | ✅ 200 |
| 13 | 按运单号查询（存在） | GET | `/logistics/search/tracking` | ✅ 200 |
| 14 | 按运单号查询（不存在） | GET | `/logistics/search/tracking` | ✅ 400 |
| 15 | 按订单号查询（存在） | GET | `/logistics/order/{orderId}` | ✅ 200 |
| 16 | 按订单号查询（不存在） | GET | `/logistics/order/{orderId}` | ✅ 200 + [] |
| 17 | 查询订单最新物流（存在） | GET | `/logistics/order/{orderId}/latest` | ✅ 200 |
| 18 | 查询订单最新物流（不存在） | GET | `/logistics/order/{orderId}/latest` | ✅ 400 |
| 19 | 删除存在的记录 | DELETE | `/logistics/delete/{id}` | ✅ 200 |
| 20 | 删除不存在的记录 | DELETE | `/logistics/delete/99999` | ✅ 400 |
| 21 | 内部创建物流 | POST | `/internal/logistics/create` | ✅ 200 |
| 22 | 内部按订单号查询 | GET | `/internal/logistics/order/{orderId}` | ✅ 200 |
| 23 | 内部查询最新物流 | GET | `/internal/logistics/order/{orderId}/latest` | ✅ 200 |
| 24 | Gateway 路由（未认证） | POST | `/api/seller/logistics/create` | ✅ 401 |
| 25 | 同订单多条记录 | POST+GET | `/logistics/create` + `/logistics/order/{orderId}` | ✅ 200，返回 2 条 |

## 总结

- **单元测试**：63 个运行，63 通过，通过率 100%
- **API 端到端测试**：25 个场景，24 个符合预期，**1 个偏差**（#11 无效 JSON 返回 500 而非 400）
- **测试数据已清理**：删除 6 条测试记录，数据库恢复初始状态

## 二次审查发现的 BUG 与报告偏差

| # | 类型 | 描述 |
|---|------|------|
| 1 | 报告偏差 | 原报告称 #11 无效 JSON 返回 `code=400`，实际测试返回 `code=500`（`HttpMessageNotReadableException` 未被 `GlobalExceptionHandler` 捕获，落入通用 Exception handler 返回 500） |
| 2 | 设计缺陷 | `type` 字段无 `@Pattern` 或枚举校验，任意字符串可写入数据库，存在数据完整性问题 |
| 3 | 报告统计偏差 | 原报告统计 27 个集成测试用例（含 Mock 模拟的 #27），实际 HTTP 端点场景为 25 个；#27 是单元测试覆盖的 Mock 场景，不应计入端到端统计 |

## 未修复 BUG

| BUG | 描述 |
|-----|------|
| 无效 JSON 返回 500 | `GlobalExceptionHandler` 缺少 `HttpMessageNotReadableException` 或 `HttpMediaTypeNotSupportedException` 的专用 handler，建议添加 → 400 |
| type 字段无校验 | 建议添加 `@Pattern(regexp = "DELIVERY|RETURN")` 或使用枚举约束 |
