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
| GET | `/api/user/auth/profile` | 查询个人信息 | - |
| PUT | `/api/user/auth/profile` | 更新个人信息 | 见下方 |

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

#### 查询个人信息响应:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": 100,
    "username": "user123",
    "phone": "13800138000",
    "email": "user@example.com",
    "infoId": 1,
    "status": 1,
    "nickname": "昵称",
    "avatar": "http://example.com/avatar.jpg"
  }
}
```

#### 更新个人信息请求体（全部可选）:
```json
{
  "nickname": "新昵称",
  "avatar": "http://example.com/avatar.jpg",
  "phone": "13800138000",
  "email": "new@example.com"
}
```

**Header**: `X-User-Id: <userId>`（由 Gateway 自动注入）

**响应**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "nickname": "新昵称",
    "avatar": "http://example.com/avatar.jpg",
    "phone": "13800138000",
    "email": "new@example.com"
  }
}
```

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

## Chat Service（AI 聊天服务）

**端口**: 8085

### AI 聊天 API (`/chat/chat`)

**Header**: 无（用户标识由 Gateway 注入 `X-User-Id`）

| 方法 | 路径 | 作用 |
|------|------|------|
| POST | `/chat/chat` | AI 对话（返回结构化 multi-turn 回复） |

#### 请求体:

```json
{
  "message": "推荐一些商品"
}
```

- `message` 必填，不能为空

#### 响应（纯文本）:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "message": "您好！有什么可以帮您的吗？",
    "reason": "用户主动打招呼，无具体业务请求，返回友好问候",
    "data": null
  }
}
```

- `data.data == null` 表示纯文本回复，无结构数据

#### 响应示例（商品）:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "message": "为您找到以下商品：",
    "reason": "用户请求推荐商品，调用 getAllProducts 获取列表后返回",
    "data": {
      "type": "product",
      "products": [
        {
          "id": 1,
          "name": "商品名称",
          "price": 99.99,
          "tags": "标签1,标签2",
          "description": "商品描述",
          "stock": 100,
          "imageUrl": "http://example.com/image.jpg",
          "shopName": "店铺名称"
        }
      ]
    }
  }
}
```

#### 响应示例（订单）:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "message": "已为您查询到订单信息：",
    "reason": "用户查询订单，调用 getOrderById 获取详情后返回",
    "data": {
      "type": "order",
      "orders": [
        {
          "orderId": "2026052200001ABCDE",
          "productId": "1",
          "quantity": 1,
          "totalPrice": 99.99,
          "orderStatus": "PENDING",
          "orderDate": "2026-05-22T12:00:00",
          "contactName": "张三",
          "contactPhone": "13800138000",
          "contactAddress": "广东省深圳市南山区xxx"
        }
      ]
    }
  }
}
```

- `data.data.type` 判别器：`"product"` 表示商品数据，`"order"` 表示订单数据
- `data.reason` 为 AI 的推理依据，适合调试/日志场景

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
  "data": {
    "products": [
      {
        "id": 1,
        "name": "商品名称",
        "price": 99.99,
        "tags": "标签1,标签2",
        "imageId": 1,
        "imageUrl": "http://example.com/image.jpg"
      }
    ],
    "page": 0,
    "size": 20
  }
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

## Shop Service（店铺服务）

**端口**: 8087

### 用户端店铺 API (`/api/user/shop`)

**Header**: `X-User-Id: <userId>`

| 方法 | 路径 | 作用 | 参数 |
|------|------|------|------|
| GET | `/api/user/shop/list?page=1&size=10` | 分页获取活跃店铺列表 | `page`（默认1），`size`（默认10） |
| GET | `/api/user/shop/{shopId}` | 获取店铺详情 | `shopId`（路径参数） |

#### 响应示例（列表）:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "shops": [
      { "id": 1, "merchantId": 1, "shopInfoId": 1, "status": 1, "createdAt": "2026-01-01T00:00:00", "updatedAt": "2026-01-01T00:00:00" }
    ],
    "total": 10,
    "page": 1,
    "size": 10
  }
}
```

#### 响应示例（详情）:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "shop": { "id": 1, "merchantId": 1, "shopInfoId": 1, "status": 1, "createdAt": "2026-01-01T00:00:00", "updatedAt": "2026-01-01T00:00:00" },
    "shopInfo": { "id": 1, "name": "店铺名称", "description": "店铺描述", "logourl": "http://example.com/logo.jpg" }
  }
}
```

---

## Order Service（订单服务）

**端口**: 8082

### 用户订单 API (`/api/user/order`)

**Header**: `X-User-Id: <userId>`

| 方法 | 路径 | 作用 |
|------|------|------|
| GET | `/api/user/order/list` | 查询当前用户的订单列表 |
| GET | `/api/user/order/{orderId}` | 查询当前用户的订单详情 |
| POST | `/api/user/order/place` | 创建/下单 |
| PUT | `/api/user/order/{orderId}/cancel` | 取消订单 |
| DELETE | `/api/user/order/{orderId}` | 删除订单（逻辑删除） |
| PUT | `/api/user/order/{orderId}/pay` | 支付订单 |
| PUT | `/api/user/order/{orderId}/deliver` | 用户确认收货 |
| POST | `/api/user/order/{orderId}/return-request` | 用户提交退货申请 |

#### 请求体（下单）:

```json
{
  "productId": 1,
  "quantity": 1,
  "contactId": 1
}
```

- `productId`、`quantity`、`contactId` 必填
- `quantity` 最小值为 1

#### 响应示例（列表）:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "orderId": "2026052200001ABCDE",
      "productId": 1,
      "shopId": 1,
      "totalPrice": 99.99,
      "quantity": 1,
      "orderStatus": "PENDING"
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
    "orderId": "2026052200001ABCDE",
    "userId": 1,
    "shopId": 1,
    "productId": 1,
    "quantity": 1,
    "totalPrice": 99.99,
    "orderStatus": "PENDING",
    "orderDate": "2026-05-22T12:00:00",
    "contactId": 1,
    "contactName": "张三",
    "contactPhone": "13800138000",
    "contactAddress": "广东省深圳市南山区xxx",
    "trackingNumber": "SF1234567890"
  }
}
```

---

# Merchant API（商家端）

> **关于 ID 类型**：由于使用雪花算法生成 64 位 ID，超出 JS 安全整数范围，所有返回给前端的 Long 类型 ID 均序列化为 **字符串** 而非数字。前端应始终以字符串方式处理 ID。

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

#### 登录/注册响应:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "xxx",
    "accountType": "MERCHANT",
    "merchantInfo": {
      "id": "2062474586787811328",
      "username": "seller1",
      "phone": "13900139001",
      "email": null,
      "infoId": null,
      "status": 1,
      "nickname": "商家一号",
      "avatar": null
    }
  }
}
```



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

请求方式: `multipart/form-data`

| 字段 | 类型 | 说明 |
|------|------|------|
| `product` | JSON 字符串 | 商品信息（见下方 JSON 结构） |
| `image` | 文件 | 商品图片，仅支持 JPG/PNG，最大 10MB |

`product` 部分 JSON 结构:
```json
{
  "name": "商品名称",
  "description": "商品描述",
  "price": 99.99,
  "stock": 100,
  "shopId": 1
}
```

- `name`、`price`、`stock`、`shopId` 必填
- `price` 必须为正数
- `stock` 必须 >= 0

**更新商品**（全部可选，以 multipart/form-data 提交）:
```json
{
  "name": "新名称",
  "description": "新描述",
  "price": 199.99,
  "stock": 50
}
```



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


---

## Order Service（订单服务）

**端口**: 8082

### 商家订单 API (`/api/seller/order`)

| 方法 | 路径 | 作用 | 参数 |
|------|------|------|------|
| GET | `/api/seller/order/shop/{shopId}/list` | 查询指定店铺的订单列表 | `shopId`（路径参数） |
| GET | `/api/seller/order/shop/{shopId}/{orderId}` | 查询指定店铺的订单详情 | `shopId`、`orderId`（路径参数） |
| PUT | `/api/seller/order/{orderId}/ship` | 商家发货 | 见下方请求体 |
| PUT | `/api/seller/order/{orderId}/approve-return` | 商家审核通过退货申请 | `orderId`（路径参数），`shopId`（请求参数） |
| PUT | `/api/seller/order/{orderId}/confirm-return` | 商家确认退货完成 | `orderId`（路径参数），`shopId`（请求参数） |

#### 请求体（发货）:

```json
{
  "trackingNumber": "SF1234567890",
  "contactId": 1,
  "shippingDate": "2026-05-22T12:00:00"
}
```

- `trackingNumber`、`contactId` 必填
- `shippingDate` 可选

#### 响应示例（列表）:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "orderId": "2026052200001ABCDE",
      "productId": 1,
      "contactId": 1,
      "quantity": 1,
      "orderStatus": "PENDING"
    }
  ]
}
```

---

## Shop Service（店铺服务）

**端口**: 8087

### 商家端店铺 API (`/api/seller/shop`)

**Header**: `X-User-Id: <userId>`

| 方法 | 路径 | 作用 | 请求体 |
|------|------|------|--------|
| GET | `/api/seller/shop/merchant/{merchantId}` | 根据商家ID查询关联店铺ID列表 | - |
| GET | `/api/seller/shop/{shopId}` | 查询店铺详情（含权限校验） | - |
| GET | `/api/seller/shop/{shopId}/employees` | 查询店铺员工列表 | - |
| POST | `/api/seller/shop/register` | 创建店铺 | `CreateShopRequest` |
| PUT | `/api/seller/shop/{shopId}` | 更新店铺信息 | `UpdateShopRequest` |
| DELETE | `/api/seller/shop/{shopId}` | 关闭店铺 | - |
| PUT | `/api/seller/shop/{shopId}/open` | 重新开店 | - |
| POST | `/api/seller/shop/{shopId}/employees/register` | 添加店员 | `AddEmployeeRequest` |
| DELETE | `/api/seller/shop/{shopId}/employees/{merchantId}` | 移除店员 | - |

#### 请求体:

**创建店铺**:
```json
{
  "name": "店铺名称",
  "description": "店铺描述",
  "logoId": "http://example.com/logo.jpg"
}
```
- `name` 必填，最长100字符
- `description` 可选，最长500字符

**更新店铺**（全部可选）:
```json
{
  "name": "新名称",
  "description": "新描述",
  "logoId": "http://example.com/new-logo.jpg"
}
```

**添加店员**:
```json
{
  "username": "employee001",
  "password": "pass123",
  "phone": "13800138000",
  "name": "店员姓名"
}
```
- `username` 必填，3-20位字母数字下划线
- `password`、`phone`、`name` 可选

#### 响应示例:

**创建成功**:
```json
{
  "code": 200,
  "message": "创建店铺成功",
  "data": { "id": 123456789 }
}
```

**店铺详情**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "shop": { "id": 1, "merchantId": 1, "shopInfoId": 1, "status": 1, "createdAt": "2026-01-01T00:00:00", "updatedAt": "2026-01-01T00:00:00" }
  }
}
```

**员工列表**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "employees": [
      { "merchantId": 1, "shopId": 1, "role": 1, "assignedBy": 1 }
    ],
    "total": 1
  }
}
```

**商家关联店铺**:
```json
{
  "code": 200,
  "message": "成功",
  "data": { "shopIds": ["2062474586787811328", "2062474586787811329", "2062474586787811330"] }
}
```



