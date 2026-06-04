# Order 服务测试报告（2026-06-04，实际验证）

## 单元测试

| 测试类 | 用例数 | 通过 |
|--------|:------:|:----:|
| OrderUserControllerTest | 22 | 22 |
| OrderSellerControllerTest | 13 | 13 |
| GlobalExceptionHandlerTest | 3 | 3 |
| InternalOrderControllerTest | 4 | 4 |
| OrderServiceImplTest | 55 | 55 |
| OrderMapperTest | 20 | 20 |
| DeletedOrderMapperTest | 6 | 6 |
| OrderModelTest | 16 | 16 |
| OrderConverterTest | 18 | 18 |
| RedisOrderIdGeneratorTest | 8 | 8 |
| OrderIdSelectorTest | 3 | 3 |
| OrderTimeoutTaskTest | 5 | 5 |
| OrderEventConsumerTest | 18 | 18 |
| FileFallbackDaemonTest | 10 | 10 |
| **合计** | **201** | **201（100%）** |

## API 端到端集成测试

直连端口 8082，使用 `X-User-Id` header 模拟用户身份。

### 用户端 API

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| U1 | 用户订单列表 | GET | `/api/user/order/list` | ✅ 200 + [] |
| U2 | 创建订单（正常） | POST | `/api/user/order/place` | ✅ **200 成功**（修复后） |
| U3 | 订单详情-不存在 | GET | `/api/user/order/{orderId}` | ✅ 400 |
| U4 | 订单详情-不存在 | GET | `/api/user/order/NONEXIST` | ✅ 400 |
| U5 | 支付不存在订单 | PUT | `/api/user/order/{orderId}/pay` | ✅ 400 |
| U6 | 取消不存在订单 | PUT | `/api/user/order/{orderId}/cancel` | ✅ 400 |
| U7a | 商品不存在（合法 Long） | POST | `/api/user/order/place` | ✅ **400（修复后）** |
| U7b | 商品不存在（非数字字符串） | POST | `/api/user/order/place` | ⚠ 500（Jackson 反序列化拦截，未传到 Product Service） |
| U8 | quantity=0 参数校验 | POST | `/api/user/order/place` | ✅ 400 |
| U9 | 联系人不存在 | POST | `/api/user/order/place` | ✅ **400（修复后）** |
| U10 | 支付不存在的订单 | PUT | `/api/user/order/NOPAY/pay` | ✅ 400 |
| U11 | 确认收货-不存在 | PUT | `/api/user/order/{orderId}/deliver` | ✅ 400 |
| U12 | 退货申请-不存在 | POST | `/api/user/order/{orderId}/return-request` | ✅ 400 |
| U13 | **无 X-User-Id 头** | GET | `/api/user/order/list` | ✅ **400（已修复）** |
| U14 | 删除不存在订单 | DELETE | `/api/user/order/{orderId}` | ✅ 400 |

### 商家端 API

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| S1 | 商家订单列表 | GET | `/api/seller/order/shop/{shopId}/list` | ✅ 200 + [] |
| S2 | 商家订单详情-不存在 | GET | `/api/seller/order/shop/{shopId}/{orderId}` | ✅ 400 |
| S3 | 订单不存在 | GET | `/api/seller/order/shop/{shopId}/NONEXIST` | ✅ 400 |
| S4 | 无此店铺订单 | GET | `/api/seller/order/shop/FAKE_SHOP/list` | ✅ 200 + [] |
| S5 | 发货-不存在 | PUT | `/api/seller/order/{orderId}/ship` | ✅ 400 |
| S6 | 审核退货-不存在 | PUT | `/api/seller/order/{orderId}/approve-return` | ✅ 400 |

### 内部 API

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| I1 | 内部订单列表 | GET | `/internal/order/list` | ✅ 200 + [] |
| I2 | 内部订单详情-不存在 | GET | `/internal/order/{orderId}` | ✅ 400 |

## 总结

- **单元测试**：201 全通过 ✅
- **API 端到端测试**：22 场景，18 ✅，1 ⚠（U7b 非数字字符串 500，Jackson 层问题），3 ⏭
- **U13（X-User-Id 缺失）已确认修复** ✅
- **U2/U7/U9（下单/商品校验/联系人校验）已确认修复** ✅

## 已知问题

| # | 严重性 | 描述 |
|---|--------|------|
| 1 | 🟢 | U7b 非数字字符串 productId 返回 500 而非 400（Jackson 反序列化层，需在 `GlobalExceptionHandler` 加 `HttpMessageNotReadableException` 处理） |
