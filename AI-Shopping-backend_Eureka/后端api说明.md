# 后端 API 说明

## 通用说明

- 成功响应：`{"code": 200, "message": "成功", "data": ...}`
- 失败响应：`{"code": 400, "message": "错误信息"}`
- 未授权：`{"code": 401, "message": "未登录" / "未获取到店铺ID"}`
- 系统错误：`{"code": 500, "message": "系统错误，请稍后重试"}`

---

# User API（用户端）

## Auth Service（认证服务）

**端口**: 8086

### 用户认证 API (`/api/user/auth`)

| 方法 | 路径 | 作用 | 请求体 |
|------|------|------|--------|
| POST | `/api/user/auth/register` | 用户注册 | 见下方 |
| POST | `/api/user/auth/login` | 用户登录 | 见下方 |
| POST | `/api/user/auth/logout` | 用户登出 | - |
| GET | `/api/user/auth/check-username?username=xxx` | 检查用户名是否可用 | - |
| GET | `/api/user/auth/check-phone?phone=xxx` | 检查手机号是否可用 | - |

#### 请求体:
```json
{
  "username": "user123",
  "password": "password123",
  "phone": "13800138000",
  "email": "user@example.com"
}
```

- 登录/注册成功后返回 `token`，后续请求需在 Header 中携带: `satoken: <token>`
- 密码使用 BCrypt 加盐加密存储

---

## Contact Service（地址服务）

**端口**: 8083

### 用户地址 API (`/api/user/contact`)

**Header**: `X-User-Id: <userId>`

| 方法 | 路径 | 作用     |
|------|------|--------|
| POST | `/api/user/contact/create` | 创建地址   |
| PUT | `/api/user/contact/update` | 更新地址   |
| DELETE | `/api/user/contact/delete/{id}` | 删除地址   |
| GET | `/api/user/contact/list` | 获取地址列表 |
| PUT | `/api/user/contact/set-default/{id}` | 设置默认地址 |

#### 请求体:

**创建地址**:
```json
{
  "name": "张三",
  "phone": "13800138000",
  "address": "广东省深圳市南山区xxx"
}
```

**更新地址**:
```json
{
  "id": 1,
  "name": "张三",
  "phone": "13800138000",
  "address": "广东省深圳市南山区xxx"
}
```

#### 响应示例:

**创建成功**:
```json
{
  "code": 200,
  "message": "创建地址成功",
  "data": { "id": 1 }
}
```

**获取列表**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "contacts": [
      { "id": 1, "name": "张三", "phone": "13800138000", "address": "xxx", "isDefault": 1 }
    ],
    "total": 1
  }
}
```

---

### 内部接口 API (`/internal/contact`)

| 方法 | 路径 | 作用                               |
|------|------|----------------------------------|
| GET | `/internal/contact/{id}` | 根据ID查询联系人，提供给Order服务构建订单信息（内部调用） |

---

## Chat Service（AI 聊天服务）

**端口**: 8085

### AI 聊天 API (`/chat/chat`)

| 方法 | 路径 | 作用 |
|------|------|------|
| POST | `/chat/chat` | AI 对话 |

#### 请求体:
```json
{
  "message": "你好，请推荐一些商品"
}
```

#### 响应:
```json
{
  "reply": "您好！根据您的需求，我为您推荐以下商品..."
}
```

---

## Product Service（商品服务）

**端口**: 8081

### 商品查询 API (`/api/user/product`)

| 方法 | 路径 | 作用 | 参数 |
|------|------|------|------|
| GET | `/api/user/product/all?page=0` | 分页查询可售商品列表 | `page`（默认0） |
| GET | `/api/user/product/{productId}` | 根据ID查询商品详情 | `productId`（路径参数） |
| GET | `/api/user/product/search?name=xxx` | 按名称模糊搜索商品 | `name` |
| GET | `/api/user/product/price-range?minPrice=0&maxPrice=100&page=0` | 按价格区间查询 | `minPrice`, `maxPrice`, `page`（默认0） |

#### 响应示例（列表）:
```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "name": "商品名称",
      "price": 99.99,
      "tags": "标签1,标签2",
      "imageId": 1,
      "imageUrl": "http://example.com/image.jpg"
    }
  ]
}
```

#### 响应示例（详情）:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "name": "商品名称",
    "price": 99.99,
    "tags": "标签1,标签2",
    "description": "商品描述",
    "stock": 100,
    "isSale": true,
    "imageId": 1,
    "imageUrl": "http://example.com/image.jpg",
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
}
```

---

# Merchant API（商家端）

## Auth Service（认证服务）

**端口**: 8086

### 商家认证 API (`/api/seller/auth`)

| 方法 | 路径 | 作用 | 请求体 |
|------|------|------|--------|
| POST | `/api/seller/auth/register` | 商家注册 | 见下方 |
| POST | `/api/seller/auth/login` | 商家登录 | 见下方 |
| POST | `/api/seller/auth/logout` | 商家登出 | - |
| GET | `/api/seller/auth/check-username?username=xxx` | 检查用户名是否可用 | - |
| GET | `/api/seller/auth/check-phone?phone=xxx` | 检查手机号是否可用 | - |

#### 请求体:
```json
{
  "username": "employee001",
  "password": "pass123",
  "phone": "13900139000",
  "merchantId": 1
}
```

---

### 内部接口 API (`/internal/auth`)

| 方法 | 路径 | 作用                         |
|------|------|----------------------------|
| POST | `/internal/auth/register-employee` | 内部调用（店长为自己店铺添加店员账号）：注册店员账号 |

---

## Product Service（商品服务）

**端口**: 8081

### 商家商品 API (`/api/seller/product`)

**Header**: `satoken: <token>`

| 方法 | 路径 | 作用 |
|------|------|------|
| POST | `/api/seller/product/create` | 创建商品 |
| PUT | `/api/seller/product/{productId}` | 更新商品 |
| DELETE | `/api/seller/product/{productId}` | 删除商品 |
| GET | `/api/seller/product/{productId}` | 查询商品详情 |
| GET | `/api/seller/product/batch?ids=1,2,3` | 批量查询商品抽象信息 |
| POST | `/api/seller/product/{productId}/list` | 上架商品 |
| POST | `/api/seller/product/{productId}/unlist` | 下架商品 |

#### 请求体:

**创建商品**:
```json
{
  "name": "商品名称",
  "description": "商品描述",
  "price": 99.99,
  "stock": 100,
  "imageUrl": "http://example.com/image.jpg"
}
```

- `name`、`imageUrl` 必填
- `price` 必须为正数
- `stock` 必须 >= 0

**更新商品**（全部可选）:
```json
{
  "name": "新名称",
  "description": "新描述",
  "price": 199.99,
  "stock": 50,
  "imageUrl": "http://example.com/new-image.jpg"
}
```

---

### 内部接口 API (`/internal/product`)

| 方法 | 路径 | 作用 |
|------|------|------|
| GET | `/internal/product/{productId}` | 根据ID查询商品详情（订单服务调用） |
| GET | `/internal/product/batch?ids=1,2,3` | 批量查询商品抽象信息 |
| POST | `/internal/product/deduct-stock` | 扣减库存 |
| POST | `/internal/product/restore-stock` | 恢复库存 |

#### 请求体（扣减/恢复库存）:
```json
{
  "productId": "1",
  "quantity": 2
}
```

- `productId`、`quantity` 必填
- `quantity` 必须为正数

---

## Contact Service（联系人服务）

**端口**: 8083

### 商家地址 API (`/api/merchant/address`)

**Header**: `X-Shop-Id: <shopId>`

| 方法 | 路径 | 作用 |
|------|------|------|
| POST | `/api/merchant/address/create` | 创建店铺地址 |
| PUT | `/api/merchant/address/update/{id}` | 更新店铺地址 |
| DELETE | `/api/merchant/address/delete/{id}` | 删除店铺地址 |
| GET | `/api/merchant/address/list` | 获取店铺地址列表 |
| GET | `/api/merchant/address/ship-default` | 获取默认收货地址 |
| PUT | `/api/merchant/address/set-default/{id}` | 设置默认地址 |

#### 请求体:

**创建地址**:
```json
{
  "name": "仓库A",
  "phone": "13800138000",
  "address": "广东省深圳市南山区xxx",
  "addressType": 1,
  "isDefault": 1
}
```

- `addressType`: 地址类型（1=收货/退货地址）
- `isDefault`: 是否默认（1=是，0=否）

#### 响应示例:

**获取默认收货地址**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": 1,
    "name": "仓库A",
    "phone": "13800138000",
    "address": "xxx",
    "addressType": 1,
    "isDefault": 1
  }
}
```

---

## Logistics Service（物流服务）

**端口**: 8084

### 物流 API (`/logistics`)

| 方法 | 路径 | 作用 | 参数 |
|------|------|------|------|
| POST | `/logistics/create` | 创建物流记录 | 见下方请求体 |
| GET | `/logistics/list` | 查询所有物流 | - |
| GET | `/logistics/search/tracking?trackingNumber=xxx` | 按快递单号搜索 | `trackingNumber` |
| DELETE | `/logistics/delete/{id}` | 删除物流记录 | `id`（路径参数） |
| GET | `/logistics/order/{orderId}` | 查询某订单的所有物流 | `orderId`（路径参数） |
| GET | `/logistics/order/{orderId}/latest?type=DELIVERY` | 查询某订单最新一条指定类型物流 | `orderId`（路径参数），`type`（DELIVERY / RETURN） |

#### 请求体（创建物流）:

```json
{
  "orderId": "2026052200001ABCDE",
  "type": "DELIVERY",
  "contactId": 1,
  "trackingNumber": "SF1234567890"
}
```

- `orderId` 必填
- `type` 可选，默认 `DELIVERY`；`DELIVERY` = 发货，`RETURN` = 退货
- `contactId` 必填
- `trackingNumber` 必填

#### 响应示例（创建成功）:

```json
{
  "code": 200,
  "message": "创建物流信息成功",
  "data": {
    "id": 1,
    "orderId": "2026052200001ABCDE",
    "type": "DELIVERY",
    "contactId": 1,
    "trackingNumber": "SF1234567890",
    "createdAt": "2026-05-22T12:00:00.000+00:00"
  }
}
```

#### 响应示例（按订单查询列表）:

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": 1,
      "orderId": "2026052200001ABCDE",
      "type": "DELIVERY",
      "contactId": 1,
      "trackingNumber": "SF1234567890",
      "createdAt": "2026-05-22T12:00:00.000+00:00"
    }
  ]
}
```

### 内部接口 API (`/internal/logistics`)

供 order-service 通过 Feign 调用。

| 方法 | 路径 | 作用 | 参数 |
|------|------|------|------|
| POST | `/internal/logistics/create` | 创建物流记录（订单发货时调用） | 见下方请求体 |
| GET | `/internal/logistics/order/{orderId}` | 查询某订单所有物流 | `orderId`（路径参数） |
| GET | `/internal/logistics/order/{orderId}/latest?type=DELIVERY` | 查询某订单最新物流 | `orderId`、`type` |

#### 请求体（创建物流）:

```json
{
  "orderId": "2026052200001ABCDE",
  "type": "DELIVERY",
  "contactId": 1,
  "trackingNumber": "SF1234567890"
}
```

#### 使用场景:

- **发货**: order-service 调用 `POST /internal/logistics/create` 创建 `type=DELIVERY` 的物流记录，然后更新订单状态为 `SHIPPED`
- **退货**: 用户发起退货后，商家调用 `POST /internal/logistics/create` 创建 `type=RETURN` 的物流记录
- **查单**: 订单详情页面调用 `GET /internal/logistics/order/{orderId}/latest?type=DELIVERY` 获取发货物流信息

> 一个订单可关联多条物流记录（如：一次发货 + 一次退货），通过 `orderId` + `type` 进行区分和查询。