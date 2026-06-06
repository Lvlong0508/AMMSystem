# 雪花 ID 精度修复计划

## 背景

所有服务使用雪花算法生成 64 位 Long 类型 ID（如 `2062474586787811328`），超出 JavaScript `Number.MAX_SAFE_INTEGER`（9,007,199,254,740,991）。Jackson 默认将 Long 序列化为 JSON 数字，前端解析时发生精度丢失。

## 问题模式

```
后端 Long(2062474586787811328) → Jackson → JSON 数字(2062474586787811000) → JS 解析
```

精度丢失的 ID 若被前端传回后端做匹配（路径参数、Header），将导致查询失败或权限校验错误。

## 已修复（全部完成 ✅）

| 位置 | 方式 | 测试 |
|------|------|------|
| `auth-service/.../AuthConverter.toMerchantInfoMap()` | `String.valueOf(merchant.getId())` | AuthConverterTest ✅ |
| `auth-service/.../AuthConverter.toUserInfoMap()` | `String.valueOf(user.getId())` | AuthConverterTest ✅ |
| `shop-service/.../ShopMerchantController.getShopsByMerchant()` | `shopIds.stream().map(String::valueOf).toList()` | ShopMerchantControllerTest ✅ |
| `shop-service/.../ShopMerchantController.getShop()` | `Shop → ShopVO` | ShopMerchantControllerTest ✅ |
| `shop-service/.../ShopMerchantController.createShop()` | `String.valueOf(shop.getId())` | ShopMerchantControllerTest ✅ |
| `shop-service/.../ShopUserController.getShopList()` | `List\<Shop> → List\<ShopVO>` | ShopUserControllerTest ✅ |
| `shop-service/.../ShopUserController.getShopDetail()` | `Shop → ShopVO, ShopInfoDTO → ShopInfoVO` | ShopUserControllerTest ✅ |
| `shop-service/.../ShopServiceImpl.getShopEmployees()` | `String.valueOf(mr.getMerchantId())` 等 3 字段 | ShopServiceImplTest ✅ |
| `product-service/.../ProductUserController` 4 端点 | `DTO → Map<String, Object>` 字符串 ID | ProductUserControllerTest ✅ |
| `product-service/.../ProductSellerController.getProductDetail()` | `DTO → Map<String, Object>` | ProductSellerControllerTest ✅ |
| `order-service/.../OrderUserController.getOrderDetail()` | `DTO → Map<String, Object>`, `userId` 转 String | OrderUserControllerTest ✅ |
| `order-service/.../OrderSellerController.getShopOrderDetail()` | 同上 | OrderSellerControllerTest ✅ |

## VO 类（新增 7 个）

| 模块 | 类 |
|------|----|
| shop-service | `vo/ShopVO.java`, `vo/ShopInfoVO.java` |
| product-service | `vo/ShopInfoVO.java`, `vo/ProductAbstractVO.java`, `vo/ProductDetailVO.java` |
| order-service | `vo/OrderDetailVO.java` |

## Converter 类（新增 1 个）

| 模块 | 类 |
|------|----|
| shop-service | `service/impl/ShopConverter.java` |

## 测试结果

| 模块 | 运行测试数 | 结果 |
|------|-----------|------|
| auth-service | 4 | ✅ 全部通过 |
| shop-service | 77 | ✅ 全部通过 |
| product-service | 32 | ✅ 全部通过 |
| order-service | 35 | ✅ 全部通过 |
| **合计** | **148** | **✅ 全部通过** |

### 1. ShopMerchantController（3个）

| # | 端点 | 影响字段 | 说明 |
|---|------|---------|------|
| 1 | `GET /api/seller/shop/{shopId}` | `shop.id`, `shop.merchantId`, `shop.shopInfoId` | `shopId` 是路径参数 |
| 2 | `GET /api/seller/shop/{shopId}/employees` | `employees[].merchantId`, `employees[].shopId`, `employees[].assignedBy` | `merchantId` 用于删除员工 |
| 3 | `POST /api/seller/shop/register` | `data.id` | 新店铺 ID 用于后续操作 |

### 2. ShopUserController（2个）

| # | 端点 | 影响字段 | 说明 |
|---|------|---------|------|
| 4 | `GET /api/user/shop/list` | `shops[].id`, `shops[].merchantId`, `shops[].shopInfoId` | `id` 用于查详情 |
| 5 | `GET /api/user/shop/{shopId}` | `shop.{id,merchantId,shopInfoId}`, `shopInfo.id` | 路径参数 |

### 3. UserAuthController（3个）

| # | 端点 | 影响字段 | 说明 |
|---|------|---------|------|
| 6 | `POST /api/user/auth/register` | `data.userInfo.id` | 通过 `X-User-Id` Header 传回 |
| 7 | `POST /api/user/auth/login` | `data.userInfo.id` | 同上 |
| 8 | `GET /api/user/auth/profile` | `data.id` | 同上 |

### 4. ProductUserController（4个）

| # | 端点 | 影响字段 | 说明 |
|---|------|---------|------|
| 9 | `GET /api/user/product/all` | `products[].id`, `products[].shop.id` | 用于查详情 |
| 10 | `GET /api/user/product/{productId}` | `id`, `shop.id` | 路径参数 |
| 11 | `GET /api/user/product/search` | `id`, `shop.id` | 同 all |
| 12 | `GET /api/user/product/price-range` | `id`, `shop.id` | 同 all |

### 5. ProductSellerController（1个）

| # | 端点 | 影响字段 | 说明 |
|---|------|---------|------|
| 13 | `GET /api/seller/product/{productId}` | `id`, `shop.id` | 路径参数 |

### 6. Order 服务（1个）

| # | 端点 | 影响字段 | 说明 |
|---|------|---------|------|
| 14 | `GET /api/user/order/{orderId}` | `data.userId` | 通过 `X-User-Id` Header 传回 |

## 修复方案建议

### 方案A：逐个 Controller 改（推荐）

对每个端点的返回处，对 Long ID 字段调用 `String.valueOf()` 或转为 `List<String>`。精确可控，但工作量大。

### 方案B：全局 Jackson 配置

```yaml
spring:
  jackson:
    serialization:
      WRITE_NUMBERS_AS_STRINGS: true
```

全部 Long 都变字符串，影响面大（总数、状态码等也会变字符串），不推荐。

### 方案C：自定义 Jackson 序列化器

对实体类 Id 字段添加 `@JsonSerialize(using = ToStringSerializer.class)`，需逐字段加注解。
