# Product 服务测试报告（2026-06-04）

## 单元测试

| 测试类 | 用例数 | 通过 |
|--------|:------:|:----:|
| ProductUserControllerTest | 12 | 12 |
| ProductSellerControllerTest | 18 | 18 |
| ProductServiceImplTest | 26 | 26 |
| ImageStorageServiceImplTest | 6 | 6 |
| ProductReservationServiceImplTest | 15 | 15 |
| ProductMapperTest | 21 | 21 |
| ProductImageInfoMapperTest | 7 | 7 |
| ProductReservationMapperTest | 12 | 12 |
| SalableProductMapperTest | 8 | 8 |
| ReservationCleanupTaskTest | 2 | 2 |
| ProductServiceApplicationTests | 1 | 1 |
| **合计** | **128** | **128（100%）** |

## API 端到端集成测试

### 用户端 API（直连 product-service:8081）

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| 1 | 分页查询可售商品 | GET | `/api/user/product/all?page=0` | ✅ |
| 2 | 按ID查询商品详情 | GET | `/api/user/product/1` | ✅ 404 "商品不存在" |
| 3 | 按名称搜索商品 | GET | `/api/user/product/search?name=测试` | ✅ |
| 4 | 价格区间查询 | GET | `/api/user/product/price-range?minPrice=0&maxPrice=100&page=0` | ✅ |
| 5 | 查询不存在商品 | GET | `/api/user/product/99999` | ✅ 404 |
| 6 | 空关键词搜索 | GET | `/api/user/product/search?name=` | ✅ |
| 7 | 无效价格区间 | GET | `/api/user/product/price-range?minPrice=100&maxPrice=0` | ✅ |

### 商家端 API

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| 8 | 创建商品（JSON body） | POST | `/api/seller/product/create` | ✅ 返回商品ID |
| 9 | 创建商品（含图片上传） | POST | `/api/seller/product/create` multipart | ✅ |
| 10 | 查询商品详情 | GET | `/api/seller/product/{id}` | ✅ |
| 11 | 更新商品 | PUT | `/api/seller/product/{id}` | ✅ |
| 12 | 上架商品 | POST | `/api/seller/product/{id}/list` | ✅ |
| 13 | 下架商品 | POST | `/api/seller/product/{id}/unlist` | ✅ |
| 14 | 删除商品 | DELETE | `/api/seller/product/{id}` | ✅ |
| 15 | 删除不存在商品 | DELETE | `/api/seller/product/99999` | ✅ 404 |
| 16 | 创建商品空body | POST | `/api/seller/product/create` body=`{}` | ✅ 400 |

### 内部 API

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| 17 | 内部查询商品 | GET | `/internal/product/1` | ✅ |
| 18 | 内部批量查询 | GET | `/internal/product/batch?ids=1,2,3` | ✅ |
| 19 | 扣减库存 | POST | `/internal/product/deduct-stock` | ✅ |
| 20 | 恢复库存 | POST | `/internal/product/restore-stock` | ✅ |
| 21 | 库存不足扣减 | POST | `/internal/product/deduct-stock` qty=999999 | ✅ 400 "库存不足" |

**集成测试：21 用例全部通过 ✅**

## API 覆盖缺口

| 分组 | 端点数 | 有单元测试 |
|------|:------:|:----------:|
| 用户端 `/api/user/product/*` | 4 | 4 |
| 商家端 `/api/seller/product/*` | 7 | 7 |
| 内部 `/internal/product/*` | 9 | **0** |

内部 API 9 个端点全部缺少单元测试（仅依赖端到端集成测试覆盖）。

## 覆盖缺口详情

| 模块 | 说明 |
|------|------|
| InternalProductController | 9 个内部 API 全部无单元测试 |
| GlobalExceptionHandler | 4/6 异常类型未直接测试（MethodArgumentNotValid、MissingServletRequestPart、Multipart、HttpMessageNotReadable） |
| ProductConverter | 无直接单元测试 |
| ImageStorageServiceImpl | 异常路径未覆盖 |

## 未修复 BUG

无
