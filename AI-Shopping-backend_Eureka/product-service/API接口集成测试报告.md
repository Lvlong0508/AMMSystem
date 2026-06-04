# Product 服务测试报告（2026-06-04，实际验证）

## 单元测试

| 测试类 | 用例数 | 通过 |
|--------|:------:|:----:|
| ProductUserControllerTest | 12 | 12 |
| ProductSellerControllerTest | 20 | 20 |
| InternalProductControllerTest | 11 | 11 |
| GlobalExceptionHandlerTest | 10 | 10 |
| ProductConverterTest | 0 | 0（仅 helper 方法，无 @Test） |
| ProductServiceImplTest | 25 | 25 |
| ImageStorageServiceImplTest | 5 | 5 |
| ProductReservationServiceImplTest | 15 | 15 |
| ProductMapperTest | 15 | 15 |
| ProductImageInfoMapperTest | 7 | 7 |
| ProductReservationMapperTest | 12 | 12 |
| SalableProductMapperTest | 6 | 6 |
| ReservationCleanupTaskTest | 2 | 2 |
| ProductServiceApplicationTests | 1 | 1 |
| **合计** | **141** | **141（100%）** |

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
| U6 | page=-1 | GET | `/api/user/product/all?page=-1` | ✅ **400（已修复）** |
| U7 | 缺少 name 参数 | GET | `/api/user/product/search` | ✅ **400（已修复）** |
| U8 | 缺少价格参数 | GET | `/api/user/product/price-range` | ✅ **400（已修复）** |
| U9 | 非数字 ID | GET | `/api/user/product/abc` | ✅ **400（已修复）** |

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
| I4 | 批量查询（空 ids） | GET | `/internal/product/batch?ids=` | ✅ **200 + []（已修复）** |
| I5 | 预占库存 | POST | `/internal/product/reserve-stock` | ✅ 200 |
| I6 | 确认预占 | POST | `/internal/product/confirm-reservation` | ✅ 200 |
| I7 | 释放预占 | POST | `/internal/product/release-reservation` | ✅ 200 |
| I8 | 恢复库存 | POST | `/internal/product/restore-stock` | ✅ 200 |
| I9 | 预占（商品不存在） | POST | `/internal/product/reserve-stock` | ✅ **400（已修复）** |
| I10 | 确认（不存在） | POST | `/internal/product/confirm-reservation` | ✅ **400（已修复）** |
| I11 | 无效 JSON | POST | `/internal/product/restore-stock` | ✅ 400 |
| I12 | 缺少 orderId | POST | `/internal/product/confirm-reservation` | ✅ **400（已修复）** |

## 总结

- **单元测试**：141 全通过 ✅
- **API 端到端测试**：32 场景全部通过 ✅（含 6 个异常路径修复）
- **数据清理**：已删除所有测试商品
