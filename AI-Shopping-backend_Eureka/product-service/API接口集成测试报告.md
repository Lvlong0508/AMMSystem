# Product 服务测试报告（2026-06-04，实际验证）

## 单元测试

| 测试类 | 用例数 | 通过 |
|--------|:------:|:----:|
| ProductUserControllerTest | 12 | 12 |
| ProductSellerControllerTest | 20 | 20 |
| InternalProductControllerTest | 10 | 10 |
| GlobalExceptionHandlerTest | 8 | 8 |
| ProductConverterTest | 0 | 0（仅 helper 方法，无 @Test） |
| ProductServiceImplTest | 24 | 24 |
| ImageStorageServiceImplTest | 5 | 5 |
| ProductReservationServiceImplTest | 15 | 15 |
| ProductMapperTest | 15 | 15 |
| ProductImageInfoMapperTest | 7 | 7 |
| ProductReservationMapperTest | 12 | 12 |
| SalableProductMapperTest | 6 | 6 |
| ReservationCleanupTaskTest | 2 | 2 |
| ProductServiceApplicationTests | 1 | 1 |
| **合计** | **137** | **137（100%）** |

> 原报告统计 162，多出 25 个（实际测试不存在或已移除）。

## API 端到端集成测试

直连 Product Service 端口 8081。create/update 使用 multipart 图片上传。

### 用户端 API

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| U1 | 可售商品列表 | GET | `/api/user/product/all?page=0` | ✅ 200 |
| U2 | 商品详情 | GET | `/api/user/product/{id}` | ✅ 200 |
| U3 | 商品详情（不存在） | GET | `/api/user/product/{id}` | ✅ 404 |
| U4 | 搜索 | GET | `/api/user/product/search?name=test` | ✅ 200 |
| U5 | 价格范围 | GET | `/api/user/product/price-range?minPrice=0&maxPrice=1000` | ✅ 200 |
| U6 | page=-1 | GET | `/api/user/product/all?page=-1` | ⚠ **500（SQL 错误泄露）** |
| U7 | 缺少 name 参数 | GET | `/api/user/product/search` | ⚠ **500（MissingServletRequestParameterException 未处理）** |
| U8 | 缺少价格参数 | GET | `/api/user/product/price-range` | ⚠ **500（同上）** |
| U9 | 非数字 ID | GET | `/api/user/product/abc` | ⚠ **500（MethodArgumentTypeMismatchException 未处理）** |

### 商家端 API

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| M1 | 创建商品（含图） | POST | `/api/seller/product/create` | ✅ 200 |
| M2 | 查询详情 | GET | `/api/seller/product/{id}` | ✅ 200 |
| M3 | 更新（纯文本） | PUT | `/api/seller/product/{id}` | ✅ 200 |
| M4 | 更新（含图） | PUT | `/api/seller/product/{id}` | ✅ 200 |
| M5 | 上架 | POST | `/api/seller/product/{id}/list` | ✅ 200 |
| M6 | 下架 | POST | `/api/seller/product/{id}/unlist` | ✅ 200 |
| M7 | 删除 | DELETE | `/api/seller/product/{id}` | ✅ 200 |
| M8 | 删除不存在商品 | DELETE | `/api/seller/product/{id}` | ✅ 404 |

### 内部 API

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| I1 | 查询商品 | GET | `/internal/product/{id}` | ✅ 200 |
| I2 | 查询（不存在） | GET | `/internal/product/{id}` | ✅ 404 |
| I3 | 批量查询 | GET | `/internal/product/batch?ids=` | ✅ 200 |
| I4 | 批量查询（空 ids） | GET | `/internal/product/batch?ids=` | ⚠ **500（空字符串 split 导致 NumberFormatException）** |
| I5 | 预占库存 | POST | `/internal/product/reserve-stock` | ✅ 200 |
| I6 | 确认预占 | POST | `/internal/product/confirm-reservation` | ✅ 200 |
| I7 | 释放预占 | POST | `/internal/product/release-reservation` | ✅ 200 |
| I8 | 恢复库存 | POST | `/internal/product/restore-stock` | ✅ 200 |
| I9 | 预占（商品不存在） | POST | `/internal/product/reserve-stock` | ⚠ **500（SQL 错误：null→int）** |
| I10 | 确认（不存在） | POST | `/internal/product/confirm-reservation` | ⚠ **500（异常消息泄露）** |
| I11 | 无效 JSON | POST | `/internal/product/restore-stock` | ✅ 400 |
| I12 | 缺少 orderId | POST | `/internal/product/confirm-reservation` | ⚠ **500（MissingServletRequestParameterException 未处理）** |

## 总结

- **单元测试**：137 全通过 ✅（原报告虚报 162，实际 137）
- **API 端到端测试**：正常流程全部通过 ✅；异常路径发现 6 个 BUG（均返回 500 应返回 400/404）
- **数据清理**：已删除所有测试商品

## 已记录 BUG（未修复）

| # | 严重性 | 端点 | 描述 |
|---|--------|------|------|
| 1 | 🟡 | GET `/api/user/product/all?page=-1` | 负 page 穿透到 SQL，500 + SQL 错误泄露到客户端 |
| 2 | 🟡 | GET `/api/user/product/search` | 缺少 required param `name` → 500，缺 `MissingServletRequestParameterException` handler |
| 3 | 🟡 | GET `/api/user/product/price-range` | 同上，缺 required param handler |
| 4 | 🟡 | GET `/internal/product/batch?ids=` | 空 ids 导致 `Long.valueOf("")` → NumberFormatException → 500 |
| 5 | 🟡 | POST `/internal/product/reserve-stock` 不存在的商品 | MyBatis 返回 null 给 primitive int → 500 + SQL 错误泄露 |
| 6 | 🟢 | GET `/api/user/product/abc` | `MethodArgumentTypeMismatchException` → 500，应加 handler → 400 |

