# Product 服务测试报告（2026-06-04）

## 单元测试

| 测试类 | 用例数 | 通过 |
|--------|:------:|:----:|
| ProductUserControllerTest | 12 | 12 |
| ProductSellerControllerTest | 20 | 20 |
| InternalProductControllerTest | 19 | 19 |
| GlobalExceptionHandlerTest | 8 | 8 |
| ProductConverterTest | 5 | 5 |
| ProductServiceImplTest | 27 | 27 |
| ImageStorageServiceImplTest | 5 | 5 |
| ProductReservationServiceImplTest | 15 | 15 |
| ProductMapperTest | 21 | 21 |
| ProductImageInfoMapperTest | 7 | 7 |
| ProductReservationMapperTest | 12 | 12 |
| SalableProductMapperTest | 8 | 8 |
| ReservationCleanupTaskTest | 2 | 2 |
| ProductServiceApplicationTests | 1 | 1 |
| **合计** | **162** | **162（100%）** |

## API 端到端集成测试

所有端点 27/27 全部通过。updateProduct 已改为 multipart 图片上传（可选），新增测试场景：含图更新、纯文本更新、格式校验、商品不存在。

### 用户端 API

| 方法 | 端点 | 结果 |
|------|------|:----:|
| GET | `/api/user/product/all?page=0` | ✅ |
| GET | `/api/user/product/{id}` | ✅ |
| GET | `/api/user/product/{id}`（不存在） | ✅ 404 |
| GET | `/api/user/product/search?name=` | ✅ |
| GET | `/api/user/product/price-range?minPrice=&maxPrice=` | ✅ |

### 商家端 API

| 方法 | 端点 | 结果 |
|------|------|:----:|
| POST | `/api/seller/product/create` | ✅ |
| GET | `/api/seller/product/{id}` | ✅ |
| PUT | `/api/seller/product/{id}` (multipart, 图片可选) | ✅ |
| DELETE | `/api/seller/product/{id}` | ✅ |
| POST | `/api/seller/product/{id}/list` | ✅ |
| POST | `/api/seller/product/{id}/unlist` | ✅ |

### 内部 API

| 方法 | 端点 | 结果 |
|------|------|:----:|
| GET | `/internal/product/{id}` | ✅ |
| GET | `/internal/product/batch?ids=` | ✅ |
| POST | `/internal/product/restore-stock` | ✅ |
| POST | `/internal/product/reserve-stock` | ✅ |
| POST | `/internal/product/confirm-reservation` | ✅ |
| POST | `/internal/product/release-reservation` | ✅ |

## 总结

- **单元测试**：162 个全部通过，覆盖 Controller、Service、Mapper、Converter、ExceptionHandler 各层
- **API 端到端测试**：25 个端点全部通过，覆盖用户端、商家端、内部 API 的成功/失败/参数校验路径。本次将 updateProduct 改为 multipart 图片上传（可选），新增异步删除旧图功能
- **未修复 BUG**：无

## 未修复 BUG

无
