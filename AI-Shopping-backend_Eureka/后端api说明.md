# 后端 API 说明

## 通用说明

- **Gateway 入口**：http://localhost:8080
- **成功响应**：{"code": 200, "message": "成功", "data": ...}
- **失败响应**：{"code": 400, "message": "错误信息"}
- **未授权**：{"code": 401, "message": "未登录"}
- **系统错误**：{"code": 500, "message": "系统错误，请稍后重试"}
- **认证方式**：Sa-Token，登录/注册成功后返回 	oken
- **用户标识**：Gateway 自动注入 X-User-Id: <userId>
- **密码**：BCrypt 加盐加密

---

# User API（用户端）

## Contact Service（地址服务）

**端口**：8083  
hello
## Auth Service
**端口**：8086  
## Auth Service（认证服务）
**端口**：8086  
**Gateway 路径前缀**：`/api/user/auth` → `auth-service`

### 用户认证 API

| 方法 | 路径 | 作用 | 请求体 |
|------|------|------|--------|
| POST | `/api/user/auth/register` | 用户注册 | `RegisterRequest` |
| POST | `/api/user/auth/login` | 用户登录 | `LoginRequest` |
| POST | `/api/user/auth/logout` | 用户登出 | - |
| GET  | `/api/user/auth/check-username?username=xxx` | 检查用户名 | - |
| GET  | `/api/user/auth/check-phone?phone=xxx` | 检查手机号 | - |
| GET  | `/api/user/auth/profile` | 查询个人信息 | - |
| PUT  | `/api/user/auth/profile` | 更新个人信息 | `UpdateProfileRequest` |

#### Request Body
```json
{"test": "value"}
```
#### 请求体 — `RegisterRequest`

```json
{"username":"user123","password":"password123"}
```

#### 登录/注册成功响应

```json
{"code":200,"message":"登录成功","data":{"token":"xxx","accountType":"USER"}}
```

---

## Contact Service（地址服务）

**端口**：8083  
**Gateway 路径前缀**：`/api/user/contact` → `contact-service`

### 用户地址 API

**Header**：`X-User-Id: <userId>`

| 方法 | 路径 | 作用 |
|------|------|------|
| POST   | `/api/user/contact/create` | 创建地址 |
| PUT    | `/api/user/contact/update` | 更新地址 |
| DELETE | `/api/user/contact/delete/{id}` | 删除地址 |
| GET    | `/api/user/contact/list` | 获取地址列表 |
| PUT    | `/api/user/contact/set-default/{id}` | 设置默认地址 |

---

## Product Service（商品服务）

**端口**：8081  
**Gateway 路径前缀**：`/api/user/product` → `product-service`

### 用户端商品 API

| 方法 | 路径 | 作用 |
|------|------|------|
| GET | `/api/user/product/all?page=0` | 分页查询可售商品列表 |
| GET | `/api/user/product/{productId}` | 商品详情 |
| GET | `/api/user/product/search?name=xxx` | 按名称搜索 |
| GET | `/api/user/product/shop/{shopId}` | 按店铺查询 |
| GET | `/api/user/product/price-range?minPrice=0&maxPrice=100&page=0` | 按价格区间查询 |

---

## Order Service（订单服务）

**端口**：8082  
**Gateway 路径前缀**：`/api/user/order` → `order-service`

### 用户端订单 API

**Header**：`X-User-Id: <userId>`

| 方法 | 路径 | 作用 | 请求体 |
|------|------|------|--------|
| POST   | `/api/user/order/place` | 创建订单 | `PlaceOrderRequest` |
| GET    | `/api/user/order/list` | 订单列表 | - |
| GET    | `/api/user/order/{orderId}` | 订单详情 | - |
| PUT    | `/api/user/order/{orderId}/cancel` | 取消订单 | - |
| PUT    | `/api/user/order/{orderId}/pay` | 支付 | - |
| PUT    | `/api/user/order/{orderId}/deliver` | 确认收货 | - |
| POST   | `/api/user/order/{orderId}/return-request` | 退货申请 | `CreateReturnRequest` |
| POST   | `/api/user/order/{orderId}/return-logistics` | 退货物流 | `SubmitReturnLogisticsRequest` |

---

## Shop Service（店铺服务）

**端口**：8087  
**Gateway 路径前缀**：`/api/user/shop` → `shop-service`

### 用户端店铺 API

**Header**：`X-User-Id: <userId>`

| 方法 | 路径 | 作用 |
|------|------|------|
| GET | `/api/user/shop/list?page=1&size=10` | 分页获取活跃店铺列表 |
| GET | `/api/user/shop/{shopId}` | 店铺详情 |

---

## Chat Service（AI 聊天服务）

**端口**：8085  
**Gateway 路径前缀**：`/api/user/chat` → `chat-service`（重写为 `/chat`）

| 方法 | 路径 | 作用 |
|------|------|------|
| POST | `/api/user/chat/chat` | AI 对话 |

#### 请求体
```json
{"message": "推荐一些商品"}
```

#### 响应格式
```json
{"code":200,"message":"成功","data":{"message":"回复","reason":"原因","data":null}}
```
- `data.data == null`：纯文本
- `data.data.type == "product"`：商品数据
- `data.data.type == "order"`：订单数据

---

## Logistics Service（物流服务）

**端口**：8084  
**Gateway 路径**：`/api/user/logistics/**` → 重写为 `/logistics/**`

### 用户端物流 API（只读）

| 方法 | 路径 | 作用 |
|------|------|------|
| GET | `/api/user/logistics/order/{orderId}` | 查询订单物流记录 |
| GET | `/api/user/logistics/order/{orderId}/latest?type=DELIVERY` | 最新物流记录 |
| GET | `/api/user/logistics/search/tracking?trackingNumber=xxx` | 按单号搜索 |

---

# Seller API（商家端）

## Auth Service（认证服务）

**端口**：8086  
**Gateway 路径前缀**：`/api/seller/auth` → `auth-service`

### 商家认证 API

| 方法 | 路径 | 作用 | 请求体 |
|------|------|------|--------|
| POST | `/api/seller/auth/register` | 注册（可选创建店铺） | `RegisterRequest`（含可选 `shop`） |
| POST | `/api/seller/auth/login` | 登录 | `LoginRequest` |
| POST | `/api/seller/auth/logout` | 登出 | - |
| GET  | `/api/seller/auth/check-username?username=xxx` | 检查用户名 | - |
| GET  | `/api/seller/auth/check-phone?phone=xxx` | 检查手机号 | - |

#### 注册（含店铺）
```json
{
  "username": "seller01",
  "password": "pass123",
  "phone": "13900139000",
  "shop": {"name":"我的小店","description":"新店开张","logoUrl":"http://..."}
}
```

---

## Shop Service（店铺服务）

**端口**：8087  
**Gateway 路径前缀**：`/api/seller/shop` → `shop-service`

### 商家端店铺 API

**Header**：`X-User-Id: <userId>`

| 方法 | 路径 | 作用 | 请求体 |
|------|------|------|--------|
| GET   | `/api/seller/shop/my-shop` | 我的店铺（一对一） | - |
| POST  | `/api/seller/shop/register` | 创建店铺 | `CreateShopRequest` |
| GET   | `/api/seller/shop/{shopId}` | 店铺详情 | - |
| PUT   | `/api/seller/shop/{shopId}` | 更新店铺 | `UpdateShopRequest` |
| PATCH | `/api/seller/shop/{shopId}/close` | 关闭店铺 | - |
| PATCH | `/api/seller/shop/{shopId}/open` | 重新开店 | - |

#### 响应 — 我的店铺
```json
{"code":200,"data":{"shop":{"id":1,"name":"我的小店","status":1}}}
```

#### 响应 — 店铺详情
```json
{"code":200,"data":{"shop":{"id":1,"merchantId":1,"status":1},"shopInfo":{"id":1,"name":"店铺名称","logourl":"http://..."}}}
```

---

## Contact Service（地址服务）

**端口**：8083  
**Gateway 路径前缀**：`/api/merchant/address` → `contact-service`

### 商家端店铺地址 API

**Header**：`X-Shop-Id: <shopId>`

| 方法 | 路径 | 作用 |
|------|------|------|
| POST   | `/api/merchant/address/create` | 创建店铺地址 |
| PUT    | `/api/merchant/address/update/{id}` | 更新地址 |
| DELETE | `/api/merchant/address/delete/{id}` | 删除地址 |
| GET    | `/api/merchant/address/list` | 地址列表 |
| GET    | `/api/merchant/address/ship-default` | 默认发货地址 |
| PUT    | `/api/merchant/address/set-default/{id}` | 设置默认地址 |

#### `CreateAddressRequest`
```json
{"name":"仓库A","phone":"13800138000","address":"xxx","addressType":1,"isDefault":1}
```
- `addressType`：必填，1=发货地址 2=退货地址

---

## Product Service（商品服务）

**端口**：8081  
**Gateway 路径前缀**：`/api/seller/product` → `product-service`

### 商家端商品 API

| 方法 | 路径 | 作用 | 请求体 |
|------|------|------|--------|
| POST   | `/api/seller/product/create` | 创建商品 | multipart/form-data |
| PUT    | `/api/seller/product/{productId}` | 更新商品 | multipart/form-data |
| DELETE | `/api/seller/product/{productId}` | 删除商品 | - |
| GET    | `/api/seller/product/{productId}` | 商品详情 | - |
| GET    | `/api/seller/product/shop/{shopId}` | 店铺商品列表 | - |
| GET    | `/api/seller/product/batch?ids=1,2,3` | 批量查询 | - |
| POST   | `/api/seller/product/{productId}/list` | 上架 | - |
| POST   | `/api/seller/product/{productId}/unlist` | 下架 | - |

---

## Order Service（订单服务）

**端口**：8082  
**Gateway 路径前缀**：`/api/seller/order` → `order-service`

### 商家端订单 API

| 方法 | 路径 | 作用 | 请求体 |
|------|------|------|--------|
| GET | `/api/seller/order/shop/{shopId}/list` | 订单列表 | - |
| GET | `/api/seller/order/shop/{shopId}/shipment-list` | 待发货订单 | - |
| GET | `/api/seller/order/shop/{shopId}/{orderId}` | 订单详情 | - |
| PUT | `/api/seller/order/{orderId}/ship?shopId=xxx` | 发货 | `ShipOrderRequest` |
| PUT | `/api/seller/order/{orderId}/confirm-return?shopId=xxx` | 确认退货完成 | - |
| GET | `/api/seller/order/return-requests/pending?shopId=xxx` | 待处理退货 | - |
| GET | `/api/seller/order/return-requests/processed?shopId=xxx` | 已处理退货 | - |
| PUT | `/api/seller/order/return-requests/{orderId}/review?shopId=xxx` | 审核退货 | `ReviewReturnRequest` |

---

## Logistics Service（商家端直接访问）

**端口**：8084  
**访问方式**：Eureka 发现，路径 `/logistics/**`

| 方法 | 路径 | 作用 | 请求体 |
|------|------|------|--------|
| POST   | `/logistics/create` | 创建物流记录 | `CreateLogisticsRequest` |
| GET    | `/logistics/list` | 所有物流记录 | - |
| GET    | `/logistics/search/tracking?trackingNumber=xxx` | 按单号搜索 | - |
| DELETE | `/logistics/delete/{id}` | 删除物流记录 | - |
| GET    | `/logistics/order/{orderId}` | 订单物流记录 | - |
| GET    | `/logistics/order/{orderId}/latest?type=DELIVERY` | 最新物流 | - |

---

# Internal API（服务间内部接口）

## Product Service — `/internal/product`

| 方法 | 路径 | 作用 |
|------|------|------|
| GET | `/internal/product/{productId}` | 商品基本信息 |
| GET | `/internal/product/page?page=0` | 分页查询可售商品 |
| GET | `/internal/product/detail/{productId}` | 完整详情 |
| GET | `/internal/product/batch?ids=1,2,3` | 批量查询 |
| POST | `/internal/product/restore-stock` | 恢复库存 |
| POST | `/internal/product/reserve-stock` | 预占库存 |
| POST | `/internal/product/confirm-reservation?orderId=xxx` | 确认预占扣减 |
| POST | `/internal/product/release-reservation?orderId=xxx` | 释放预占 |

## Order Service — `/internal/order`

**Header**：`X-User-Id: <userId>`

| 方法 | 路径 | 作用 |
|------|------|------|
| GET | `/internal/order/{orderId}` | 订单详情 |
| GET | `/internal/order/list` | 用户所有订单 |

## Shop Service — `/internal/shop`

| 方法 | 路径 | 作用 |
|------|------|------|
| POST | `/internal/shop/create-for-merchant` | 为商家创建店铺 |
| GET  | `/internal/shop/info/{shopId}` | 店铺详细信息 |
| POST | `/internal/shop/info/batch` | 批量查询 |

## Contact Service — `/internal/contact`

| 方法 | 路径 | 作用 |
|------|------|------|
| GET | `/internal/contact/{id}` | 查询联系人 |

---

# Gateway 路由配置

**端口**：8080

| 路由 ID | 路径前缀 | 目标服务 | 重写 |
|---------|---------|---------|------|
| user-auth | `/api/user/auth/**` | auth-service | 透传 |
| user-product | `/api/user/product/**` | product-service | 透传 |
| user-order | `/api/user/order/**` | order-service | 透传 |
| user-contact | `/api/user/contact/**` | contact-service | 透传 |
| user-logistics | `/api/user/logistics/**` | logistics-service | → /logistics/** |
| user-chat | `/api/user/chat/**` | chat-service | → /chat/** |
| user-shop | `/api/user/shop/**` | shop-service | 透传 |
| seller-auth | `/api/seller/auth/**` | auth-service | 透传 |
| seller-product | `/api/seller/product/**` | product-service | 透传 |
| seller-order | `/api/seller/order/**` | order-service | 透传 |
| merchant-address | `/api/merchant/address/**` | contact-service | 透传 |
| seller-shop | `/api/seller/shop/**` | shop-service | 透传 |
