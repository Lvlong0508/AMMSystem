# 后端 API 说明

## 通用说明

- **Gateway 入口**：`http://localhost:8080`
- **成功响应**：`{"code": 200, "message": "成功", "data": ...}`
- **失败响应**：`{"code": 400, "message": "错误信息"}`
- **未授权**：`{"code": 401, "message": "未登录"}`
- **系统错误**：`{"code": 500, "message": "系统错误，请稍后重试"}`
- **认证方式**：Sa-Token，登录/注册成功后返回 `token`
- **用户标识**：Gateway 认证后向后端注入 `X-User-Id: <userId>`
- **商家店铺标识**：部分商家接口需要 `X-Shop-Id: <shopId>`
- **密码存储**：BCrypt 加盐加密

---

# User API（用户端）

## Auth Service（认证服务）

- **端口**：`8086`
- **Gateway 路径前缀**：`/api/user/auth` → `auth-service`

| 方法 | 路径 | 作用 | 请求体/参数 |
|------|------|------|-------------|
| POST | `/api/user/auth/register` | 用户注册 | `RegisterRequest` |
| POST | `/api/user/auth/login` | 用户登录 | `LoginRequest` |
| POST | `/api/user/auth/logout` | 用户登出 | - |
| GET | `/api/user/auth/check-username?username=xxx` | 检查用户名是否存在 | `username` |
| GET | `/api/user/auth/check-phone?phone=xxx` | 检查手机号是否存在 | `phone` |
| GET | `/api/user/auth/profile` | 查询个人信息 | Header：`X-User-Id` |
| PUT | `/api/user/auth/profile` | 更新个人信息 | Header：`X-User-Id`，`UpdateProfileRequest` |

### 登录/注册成功响应

```json
{"code":200,"message":"登录成功","data":{"token":"xxx","accountType":"USER"}}
```

---

## Contact Service（地址服务）

- **端口**：`8083`
- **Gateway 路径前缀**：`/api/user/contact` → `contact-service`
- **Header**：`X-User-Id: <userId>`

| 方法 | 路径 | 作用 | 请求体/参数 |
|------|------|------|-------------|
| POST | `/api/user/contact/create` | 创建用户地址 | `CreateContactRequest` |
| PUT | `/api/user/contact/update` | 更新用户地址 | `UpdateContactRequest` |
| DELETE | `/api/user/contact/delete/{id}` | 删除用户地址 | `id` |
| GET | `/api/user/contact/list` | 获取用户地址列表 | - |
| PUT | `/api/user/contact/set-default/{id}` | 设置默认用户地址 | `id` |

---

## Product Service（商品服务）

- **端口**：`8081`
- **Gateway 路径前缀**：`/api/user/product` → `product-service`

| 方法 | 路径 | 作用 | 请求体/参数 |
|------|------|------|-------------|
| GET | `/api/user/product/all?page=0` | 分页查询可售商品列表 | `page` |
| GET | `/api/user/product/{productId}` | 查询商品详情 | `productId` |
| GET | `/api/user/product/search?name=xxx` | 按名称搜索商品 | `name` |
| GET | `/api/user/product/shop/{shopId}` | 按店铺查询商品 | `shopId` |
| GET | `/api/user/product/price-range?minPrice=0&maxPrice=100&page=0` | 按价格区间查询商品 | `minPrice`、`maxPrice`、`page` |

---

## Order Service（订单服务）

- **端口**：`8082`
- **Gateway 路径前缀**：`/api/user/order` → `order-service`
- **Header**：`X-User-Id: <userId>`

| 方法 | 路径 | 作用 | 请求体/参数 |
|------|------|------|-------------|
| GET | `/api/user/order/list` | 查询用户订单列表 | - |
| GET | `/api/user/order/{orderId}` | 查询订单详情 | `orderId` |
| POST | `/api/user/order/place` | 创建订单 | `PlaceOrderRequest` |
| PUT | `/api/user/order/{orderId}/cancel` | 取消订单 | `orderId` |
| DELETE | `/api/user/order/{orderId}` | 删除订单 | `orderId` |
| PUT | `/api/user/order/{orderId}/pay` | 支付订单 | `orderId` |
| PUT | `/api/user/order/{orderId}/deliver` | 确认收货 | `orderId` |
| POST | `/api/user/order/{orderId}/return-request` | 提交退货申请 | `orderId`，`CreateReturnRequest` |
| POST | `/api/user/order/{orderId}/return-logistics` | 提交退货物流 | `orderId`，`SubmitReturnLogisticsRequest` |

---

## Shop Service（店铺服务）

- **端口**：`8087`
- **Gateway 路径前缀**：`/api/user/shop` → `shop-service`
- **Header**：`X-User-Id: <userId>`

| 方法 | 路径 | 作用 | 请求体/参数 |
|------|------|------|-------------|
| GET | `/api/user/shop/list?page=1&size=10` | 分页获取活跃店铺列表 | `page`、`size` |
| GET | `/api/user/shop/{shopId}` | 查询店铺详情 | `shopId` |

---

## Chat Service（AI 聊天服务）

- **端口**：`8085`
- **Gateway 路径前缀**：`/api/user/chat` → `chat-service`（重写为 `/chat`）

| 方法 | 路径 | 作用 | 请求体/参数 |
|------|------|------|-------------|
| POST | `/api/user/chat/chat` | AI 对话 | `ChatRequest` |

### 请求体示例

```json
{"message":"推荐一些商品"}
```

### 响应说明

- `data.data == null`：纯文本回复
- `data.data.type == "product"`：商品数据
- `data.data.type == "order"`：订单数据

---

## Logistics Service（物流服务）

- **端口**：`8084`
- **Gateway 路径前缀**：`/api/user/logistics` → `logistics-service`（重写为 `/logistics`）

| 方法 | 路径 | 作用 | 请求体/参数 |
|------|------|------|-------------|
| POST | `/api/user/logistics/create` | 创建物流记录 | `CreateLogisticsRequest` |
| GET | `/api/user/logistics/list` | 查询物流列表 | - |
| GET | `/api/user/logistics/search/tracking?trackingNumber=xxx` | 按物流单号查询 | `trackingNumber` |
| DELETE | `/api/user/logistics/delete/{id}` | 删除物流记录 | `id` |
| GET | `/api/user/logistics/order/{orderId}` | 查询订单物流记录 | `orderId` |
| GET | `/api/user/logistics/order/{orderId}/latest` | 查询订单最新物流记录 | `orderId` |

---

# Seller API（商家端）

## Auth Service（商家认证服务）

- **端口**：`8086`
- **Gateway 路径前缀**：`/api/seller/auth` → `auth-service`

| 方法 | 路径 | 作用 | 请求体/参数 |
|------|------|------|-------------|
| POST | `/api/seller/auth/register` | 商家注册 | `RegisterRequest` |
| POST | `/api/seller/auth/login` | 商家登录 | `LoginRequest` |
| POST | `/api/seller/auth/logout` | 商家登出 | - |
| GET | `/api/seller/auth/check-username?username=xxx` | 检查用户名是否存在 | `username` |
| GET | `/api/seller/auth/check-phone?phone=xxx` | 检查手机号是否存在 | `phone` |

### 登录/注册成功响应

```json
{"code":200,"message":"登录成功","data":{"token":"xxx","accountType":"MERCHANT"}}
```

---

## Product Service（商家商品服务）

- **端口**：`8081`
- **Gateway 路径前缀**：`/api/seller/product` → `product-service`

| 方法 | 路径 | 作用 | 请求体/参数 |
|------|------|------|-------------|
| GET | `/api/seller/product/{productId}` | 查询商品详情 | `productId` |
| GET | `/api/seller/product/shop/{shopId}` | 查询店铺商品 | `shopId` |
| GET | `/api/seller/product/batch` | 批量查询商品 | 查询参数以 Controller 为准 |
| POST | `/api/seller/product/create` | 创建商品 | multipart：`product: CreateProductRequest`，`image: MultipartFile` |
| PUT | `/api/seller/product/{productId}` | 更新商品 | multipart：`productId`，`product: UpdateProductRequest`，`image?: MultipartFile` |
| DELETE | `/api/seller/product/{productId}` | 删除商品 | `productId` |
| POST | `/api/seller/product/{productId}/list` | 商品上架 | `productId` |
| POST | `/api/seller/product/{productId}/unlist` | 商品下架 | `productId` |

---

## Order Service（商家订单服务）

- **端口**：`8082`
- **Gateway 路径前缀**：`/api/seller/order` → `order-service`

| 方法 | 路径 | 作用 | 请求体/参数 |
|------|------|------|-------------|
| GET | `/api/seller/order/shop/{shopId}/list` | 查询店铺订单列表 | `shopId` |
| GET | `/api/seller/order/shop/{shopId}/shipment-list` | 查询待发货订单列表 | `shopId` |
| GET | `/api/seller/order/shop/{shopId}/{orderId}` | 查询店铺订单详情 | `shopId`、`orderId` |
| PUT | `/api/seller/order/{orderId}/ship` | 发货 | `orderId`，`ShipOrderRequest` |
| PUT | `/api/seller/order/{orderId}/confirm-return` | 确认退货 | `orderId` |
| GET | `/api/seller/order/return-requests/pending` | 查询待处理退货申请 | - |
| GET | `/api/seller/order/return-requests/processed` | 查询已处理退货申请 | - |
| PUT | `/api/seller/order/return-requests/{orderId}/review` | 审核退货申请 | `orderId`，`ReviewReturnRequest` |

---

## Address Service（商家地址服务）

- **端口**：`8083`
- **Gateway 路径前缀**：`/api/merchant/address` → `contact-service`
- **Header**：`X-Shop-Id: <shopId>`

| 方法 | 路径 | 作用 | 请求体/参数 |
|------|------|------|-------------|
| POST | `/api/merchant/address/create` | 创建商家地址 | `CreateAddressRequest` |
| PUT | `/api/merchant/address/update/{id}` | 更新商家地址 | `id`，`UpdateAddressRequest` |
| DELETE | `/api/merchant/address/delete/{id}` | 删除商家地址 | `id` |
| GET | `/api/merchant/address/list` | 查询商家地址列表 | - |
| GET | `/api/merchant/address/ship-default` | 查询默认发货地址 | - |
| PUT | `/api/merchant/address/set-default/{id}` | 设置默认商家地址 | `id` |

---

## Logistics Service（商家物流服务）

- **端口**：`8084`
- **Gateway 路径前缀**：`/api/seller/logistics` → `logistics-service`（重写为 `/logistics`）

| 方法 | 路径 | 作用 | 请求体/参数 |
|------|------|------|-------------|
| POST | `/api/seller/logistics/create` | 创建物流记录 | `CreateLogisticsRequest` |
| GET | `/api/seller/logistics/list` | 查询物流列表 | - |
| GET | `/api/seller/logistics/search/tracking?trackingNumber=xxx` | 按物流单号查询 | `trackingNumber` |
| DELETE | `/api/seller/logistics/delete/{id}` | 删除物流记录 | `id` |
| GET | `/api/seller/logistics/order/{orderId}` | 查询订单物流记录 | `orderId` |
| GET | `/api/seller/logistics/order/{orderId}/latest` | 查询订单最新物流记录 | `orderId` |

---

## Chat Service（商家 AI 聊天服务）

- **端口**：`8085`
- **Gateway 路径前缀**：`/api/seller/chat` → `chat-service`（重写为 `/chat`）

| 方法 | 路径 | 作用 | 请求体/参数 |
|------|------|------|-------------|
| POST | `/api/seller/chat/chat` | AI 对话 | `ChatRequest` |

---

## Shop Service（商家店铺服务）

- **端口**：`8087`
- **Gateway 路径前缀**：`/api/seller/shop` → `shop-service`
- **Header**：`X-User-Id: <userId>`

| 方法 | 路径 | 作用 | 请求体/参数 |
|------|------|------|-------------|
| GET | `/api/seller/shop/my-shop` | 查询我的店铺 | - |
| POST | `/api/seller/shop/register` | 注册店铺（JSON） | `CreateShopRequest` |
| POST | `/api/seller/shop/register` | 注册店铺（multipart，可上传 logo） | `shop: CreateShopRequest`，`logo?: MultipartFile` |
| GET | `/api/seller/shop/{shopId}` | 查询店铺详情 | `shopId` |
| PUT | `/api/seller/shop/{shopId}` | 更新店铺 | `shopId`，`UpdateShopRequest` |
| PATCH | `/api/seller/shop/{shopId}/close` | 关闭店铺 | `shopId` |
| PATCH | `/api/seller/shop/{shopId}/open` | 开启店铺 | `shopId` |

---

# Gateway 路由配置

- **端口**：`8080`

| 路由 ID | 对外路径前缀 | 目标服务 | 转发/重写 |
|---------|--------------|----------|-----------|
| user-auth | `/api/user/auth/**` | `auth-service` | 透传 |
| user-product | `/api/user/product/**` | `product-service` | 透传到 `/api/user/product/**` |
| user-order | `/api/user/order/**` | `order-service` | 透传到 `/api/user/order/**` |
| user-contact | `/api/user/contact/**` | `contact-service` | 透传到 `/api/user/contact/**` |
| user-logistics | `/api/user/logistics/**` | `logistics-service` | 重写为 `/logistics/**` |
| user-chat | `/api/user/chat/**` | `chat-service` | 重写为 `/chat/**` |
| seller-auth | `/api/seller/auth/**` | `auth-service` | 透传 |
| seller-product | `/api/seller/product/**` | `product-service` | 透传到 `/api/seller/product/**` |
| seller-order | `/api/seller/order/**` | `order-service` | 透传到 `/api/seller/order/**` |
| merchant-address | `/api/merchant/address/**` | `contact-service` | 透传到 `/api/merchant/address/**` |
| seller-logistics | `/api/seller/logistics/**` | `logistics-service` | 重写为 `/logistics/**` |
| seller-chat | `/api/seller/chat/**` | `chat-service` | 重写为 `/chat/**` |
| seller-shop | `/api/seller/shop/**` | `shop-service` | 透传 |
| user-shop | `/api/user/shop/**` | `shop-service` | 透传 |
