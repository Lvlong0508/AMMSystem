# Shop 服务测试报告（2026-06-08，重构后）

## 单元测试

| 测试类 | 用例数 | 通过 |
|--------|:------:|:----:|
| ShopMerchantControllerTest | 9 | 9 |
| ShopUserControllerTest | 7 | 7 |
| InternalShopControllerTest | 7 | 7 |
| GlobalExceptionHandlerTest | 5 | 5 |
| ShopServiceImplTest | 24 | 24 |
| ShopMapperTest | 17 | 17 |
| ShopInfoMapperTest | 7 | 7 |
| **合计** | **76** | **76（100%）** |

> 变更说明：MerchantRoleMapperTest（15 用例）已删除（merchant_roles 表已废弃）
> ShopServiceImplTest 从 44 用例精简为 24（删除了角色/员工相关测试）

## 重构 API 变更清单

### 删除的 API

| 方法 | 路径 | 原因 |
|------|------|------|
| GET | `/api/seller/shop/myShop` | 改为 `/my-shop`（单数语义） |
| POST | `/api/seller/shop/register` | 合并到 auth-service 注册流程 |
| GET | `/api/seller/shop/{shopId}/employees` | 不再有员工概念 |
| POST | `/api/seller/shop/{shopId}/employees` | 同上 |
| DELETE | `/api/seller/shop/{shopId}/employees/{merchantId}` | 同上 |
| GET | `/internal/shop/employees/roles/{merchantId}` | 同上 |

### 新增的 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/internal/shop/create-for-merchant` | auth-service 注册时 Feign 调用创建店铺 |

### 保留的 API

| 方法 | 路径 | 备注 |
|------|------|------|
| GET | `/api/seller/shop/my-shop` | 返回单个 shop（不再是列表） |
| GET | `/api/seller/shop/{shopId}` | 同前缀兼容 |
| PUT | `/api/seller/shop/{shopId}` | 权限校验改为 merchantId 比对 |
| PATCH | `/api/seller/shop/{shopId}/close` | 同上 |
| PATCH | `/api/seller/shop/{shopId}/open` | 同上 |

## 数据模型变更

- `merchant_roles` 表已删除
- `shops` 表新增 `UNIQUE KEY uk_merchant_id (merchant_id)` 约束

## 注册流程变更

### 旧流程（两步）

```
商家注册（auth-service）→ 调 shop-service 创建店铺（auth API）
```

### 新流程（一步）

```
商家注册（auth-service，请求体含 shop 字段）
  → auth-service 内部 Feign 调用 shop-service 创建店铺
  → 如果失败则回滚注册
```

### 新请求体示例

```json
{
  "username": "seller01",
  "password": "pass123",
  "phone": "13900139000",
  "shop": {
    "name": "我的小店",
    "description": "新店开张",
    "logoUrl": "http://logo.jpg"
  }
}
```