# AI-Shopping 项目快速索引表

> 本文件为 AI 对话快速理解项目的索引文档，可直接嵌入对话开头使用。

---

## 1. 项目概述

**一句话说明**：基于 Spring Cloud Eureka 的 AI 智能购物平台，支持用户购物和商家入驻双端功能。

**技术栈**：
- 后端：Java 17 + Spring Boot 3.2.3 + Spring Cloud 2023.0.0 + MyBatis
- 前端：Vue 3 + Vite + Tailwind CSS
- 认证：Sa-Token 1.39.0（Redis 存储）
- AI：LangChain4j 0.35.0（DashScope / GLM）
- 注册中心：Eureka（HTTP Basic Auth: admin/admin）
- 网关：Spring Cloud Gateway（WebFlux）
- 数据库：MySQL（多库分离：eureka_auth / product / order / contact / shop / logistics）
- 缓存：Redis（Token 存储 + 分布式限流 + 事件驱动 + 订单ID生成）
- 服务调用：OpenFeign（统一契约模块 common-api）
- 事件驱动：Redis Stream + 本地文件兜底

---

## 2. 目录结构

```
AI-Shopping/
├── AI-Shopping-backend_Eureka/          # 后端微服务根目录
│   ├── pom.xml                          # 父 POM（版本统一管理）
│   ├── eureka-server/                  # 服务注册中心（端口 8761）
│   ├── gateway-service/                # API 网关（端口 8080）
│   ├── common-api/                     # 统一契约模块（Feign 接口 + DTO + ApiResponse）
│   ├── auth-service/                   # 认证服务（端口 8086）
│   ├── product-service/                # 商品服务（端口 8081）
│   ├── shop-service/                   # 店铺服务（端口 8087）
│   ├── order-service/                  # 订单服务（端口 8082）
│   ├── contact-service/                # 联系人/地址服务（端口 8083）
│   ├── logistics-service/             # 物流服务（端口 8084）
│   ├── chat-service/                   # AI 聊天服务（端口 8085）
│   └── sql/                            # 数据库初始化脚本
│       ├── init/                       # 建表脚本
│       └── insert/                     # 数据插入脚本
│
├── AI-Shopping-frontier/               # 前端根目录
│   ├── frontier-user/                  # 用户端前端（Vue 3 + Vite）
│   └── frontier-seller/                # 商家端前端
│
├── start-frontier.bat                  # 启动前端脚本
├── stop-frontier.bat                   # 停止前端脚本
├── AI-Shopping-backend_Eureka/start-end.bat   # 启动后端脚本
├── AI-Shopping-backend_Eureka/stop-end.bat    # 停止后端脚本
└── pom.xml                             # 根 POM
```

---

## 3. 功能模块

### 3.1 认证模块（auth-service）

**职责**：用户和商家的注册、登录、登出、Token 校验、用户名/手机号查重、员工注册

**核心文件**：
- `controller/UserAuthController.java` - 用户认证控制器
- `controller/MerchantAuthController.java` - 商家认证控制器
- `service/UserAuthService.java` / `impl/UserAuthServiceImpl.java` - 用户认证
- `service/MerchantAuthService.java` / `impl/MerchantAuthServiceImpl.java` - 商家认证 + 员工注册
- `service/UserInfoService.java` / `MerchantInfoService.java` - 扩展信息
- `mapper/user/UserMapper.java` / `UserInfoMapper.java` - 用户表 Mapper
- `mapper/merchant/MerchantMapper.java` / `MerchantInfoMapper.java` - 商家表 Mapper
- `model/User.java` / `UserInfo.java` - 消费者用户
- `model/Merchant.java` / `MerchantInfo.java` - 商家用户
- `util/BCryptUtil.java` - BCrypt 加密（strength=12）

**API 端点**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/auth/register` | 用户注册 |
| POST | `/api/user/auth/login` | 用户登录 |
| POST | `/api/user/auth/logout` | 用户登出 |
| GET | `/api/user/auth/check-username` | 检查用户名是否已存在 |
| GET | `/api/user/auth/check-phone` | 检查手机号是否已注册 |
| POST | `/api/seller/auth/register` | 商家注册 |
| POST | `/api/seller/auth/login` | 商家登录 |
| POST | `/api/seller/auth/logout` | 商家登出 |
| GET | `/api/seller/auth/check-username` | 检查商家用户名 |
| GET | `/api/seller/auth/check-phone` | 检查商家手机号 |

**注意**：README 早期版本的 `GET /api/user/auth/info` 和 `GET /api/seller/auth/info` 端点**不存在**。

**Token 机制**：
- Sa-Token + Redis 存储
- 用户 Token 标记 `accountType=USER`，商家标记 `accountType=MERCHANT`
- 不允许并发登录（`is-concurrent: false`），Token 有效期 1 天
- Gateway 层统一校验 Token 并注入 `X-User-Id` Header

**密码**：BCrypt 加密（strength=12）

**数据库**：eureka_auth（t_user, user_info, t_merchant, merchant_info）

---

### 3.2 API 网关模块（gateway-service）

**职责**：请求路由、Token 校验、Redis 分布式限流、角色权限控制、CORS

**核心文件**：
- `filter/IpRateLimitFilter.java` - IP 限流过滤器（Order=-200，Redis 分布式）
- `filter/SaTokenAuthGlobalFilter.java` - 认证鉴权过滤器（Order=-100，注入 X-User-Id）
- `service/RedisRateLimitService.java` - Redis 限流实现（INCR + EXPIRE，300次/60秒）
- `service/impl/AuthServiceImpl.java` - 认证服务（直读 Redis Token）
- `config/AuthWhitelistProperties.java` - 白名单配置
- `config/IpRateLimitProperties.java` - 限流参数配置
- `handler/GlobalErrorWebExceptionHandler.java` - 全局异常处理（Order=-1）
- `exception/GatewayAuthException.java` - 网关认证异常
- `application.yml` - 路由规则（19 条 + 4 条内部路由）

**过滤器链**：

| Order | 过滤器 | 职责 |
|-------|--------|------|
| -200 | `IpRateLimitFilter` | IP 限流（最先执行） |
| -100 | `SaTokenAuthGlobalFilter` | 认证 + 鉴权 + userId 注入 |
| -1 | `GlobalErrorWebExceptionHandler` | 异常兜底（非 Filter） |

**路由规则**：
- 用户端：`/api/user/auth/*`, `/api/user/product/*`, `/api/user/order/*`, `/api/user/contact/*`, `/api/user/logistics/*`, `/api/user/chat/*`, `/api/user/shop/*`
- 商家端：`/api/seller/auth/*`, `/api/seller/product/*`, `/api/seller/order/*`, `/api/seller/contact/*`, `/api/seller/address/*`, `/api/seller/logistics/*`, `/api/seller/chat/*`, `/api/seller/shop/*`
- 内部：`/internal/shop/*`, `/internal/auth/*`, `/internal/product/*`, `/internal/order/*`

**白名单（9 条）**：
```
/api/user/auth/login, /api/user/auth/register
/api/user/auth/check-username, /api/user/auth/check-phone
/api/seller/auth/login, /api/seller/auth/register
/api/seller/auth/check-username, /api/seller/auth/check-phone
/api/seller/shop/register
```

**认证逻辑**：
1. OPTIONS 预检 → 直接放行
2. 白名单路径 → 跳过认证
3. 其他 → 从 Header 取 `satoken`，直读 Redis key `satoken:login:token:{token}`
4. 校验 loginId 前缀与路径匹配：`USER:` → `/api/user/**`，`MERCHANT:` → `/api/seller/**`
5. 通过后重写 Header，注入 `X-User-Id`

**限流**：Redis 计数器，每 IP 每 60 秒最多 300 次

**依赖**：Redis（Token 校验 + 分布式限流）

---

### 3.3 商品模块（product-service）

**职责**：商品 CRUD、库存管理、库存预占/确认/释放、上下架、图片管理、过期预占清理

**核心文件**：
- `controller/ProductUserController.java` - 用户端（浏览/搜索/详情）
- `controller/ProductSellerController.java` - 商家端（CRUD/上下架）
- `controller/internal/InternalProductController.java` - 内部（库存预占/扣减，Feign 提供方）
- `service/ProductService.java` - 商品业务 + Caffeine 店铺缓存
- `service/ProductReservationService.java` - 库存预占（FOR UPDATE 行级锁）
- `mapper/ProductMapper.java` / `SalableProductMapper.java` / `ProductImageInfoMapper.java` / `ProductReservationMapper.java`
- `model/Product.java` / `ProductImageInfo.java` / `ProductReservation.java`
- `task/ReservationCleanupTask.java` - 每 2 分钟清理过期预占
- `converter/ProductConverter.java` - DTO 转换

**库存预占流程**：
```
下单 → reserve()
  ├── SELECT stock FOR UPDATE
  ├── SELECT SUM(reserved) FROM reservations FOR UPDATE
  ├── 校验可用库存 = stock - reserved >= quantity
  └── INSERT reservation (status=RESERVED, expired_at=now+30min)

支付 → confirm()
  ├── UPDATE reservation SET status=CONFIRMED
  └── UPDATE products SET stock = stock - quantity

取消 → release()
  └── UPDATE reservation SET status=RELEASED

超时 → ReservationCleanupTask (每2分钟)
  └── 查询过期 RESERVED → 逐条 release()
```

**API 端点**：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/product/all?page=0` | 分页浏览可售商品 |
| GET | `/api/user/product/{productId}` | 商品详情 |
| GET | `/api/user/product/search?name=` | 按名称搜索 |
| GET | `/api/user/product/price-range` | 价格区间筛选 |
| POST | `/api/seller/product/create` | 创建商品 |
| PUT | `/api/seller/product/{id}` | 更新商品 |
| DELETE | `/api/seller/product/{id}` | 删除商品（需先下架） |
| POST | `/api/seller/product/{id}/list` | 上架 |
| POST | `/api/seller/product/{id}/unlist` | 下架 |
| GET | `/internal/product/{id}` | 内部查详情（Feign） |
| POST | `/internal/product/deduct-stock` | 内部扣减库存 |
| POST | `/internal/product/restore-stock` | 内部恢复库存 |
| POST | `/internal/product/reserve-stock` | 内部预占库存 |
| POST | `/internal/product/confirm-reservation` | 内部确认预占 |
| POST | `/internal/product/release-reservation` | 内部释放预占 |

**依赖**：eureka_product（products, salable_products, product_image_info, product_reservation）

---

### 3.4 订单模块（order-service）

**职责**：订单全生命周期管理、状态机、Redis Stream 事件驱动、超时自动取消

**核心文件**：
- `controller/OrderUserController.java` - 用户端（8 个端点）
- `controller/OrderSellerController.java` - 商家端（5 个端点）
- `controller/internal/InternalOrderController.java` - 内部查询
- `service/OrderService.java` / `impl/OrderServiceImpl.java` - 核心业务 + Feign 调用
- `model/Order.java` - 订单实体 + 状态机（`TRANSITIONS` Map + `canTransition()`）
- `stream/OrderEventConsumer.java` - Redis Stream 事件消费
- `stream/FileFallbackDaemon.java` - Redis 不可用时本地文件兜底（每分钟重试）
- `stream/RedisStreamConfig.java` - Stream + Consumer Group 初始化
- `stream/OrderEventType.java` - 事件枚举（STOCK_CONFIRM / STOCK_RESTORE / LOGISTICS_CREATE）
- `task/OrderTimeoutTask.java` - 每分钟扫描超时 PENDING 订单自动取消
- `id/RedisOrderIdGenerator.java` - 订单 ID 生成（yyyyMMdd + 序号 + 随机字母）
- `converter/OrderConverter.java` - 转换器
- `model/DeletedOrder.java` - 删除前备份实体

**订单状态机**：
```
PENDING → PAID → SHIPPED → DELIVERED → DELETED
  │         │                          ▲
  ▼         ▼                          │
CANCELLED ←─┘                          │
  │                                    │
  ▼                                    │
DELETED ◄──────────────────────────────┘

SHIPPED / DELIVERED → RETURN_PENDING → RETURNING → RETURNED
```

**事件驱动（Redis Stream）**：
- 事务提交后（`afterCommit`）发送事件，保证事务回滚时不发消息
- STOCK_CONFIRM：支付后确认库存扣减（若已取消则释放预占）
- STOCK_RESTORE：确认退货后恢复库存（Redis SET NX 幂等锁，7天 TTL）
- LOGISTICS_CREATE：发货后创建物流记录（先查幂等）
- 兜底：Redis 不可用时写入 `data/failover/` 目录，每分钟重试

**订单 ID 生成**：`RedisOrderIdGenerator`，格式 `yyyyMMdd+5位序号+5位随机字母`（18位）

**API 端点**：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/order/list` | 用户订单列表 |
| GET | `/api/user/order/{orderId}` | 订单详情 |
| POST | `/api/user/order/place` | 创建订单 |
| PUT | `/api/user/order/{orderId}/cancel` | 取消订单（CAS 二段式） |
| DELETE | `/api/user/order/{orderId}` | 删除订单（先备份再物理删） |
| PUT | `/api/user/order/{orderId}/pay` | 支付 |
| PUT | `/api/user/order/{orderId}/deliver` | 确认收货 |
| POST | `/api/user/order/{orderId}/return-request` | 申请退货 |
| GET | `/api/seller/order/shop/{shopId}/list` | 店铺订单列表 |
| GET | `/api/seller/order/shop/{shopId}/{orderId}` | 店铺订单详情 |
| PUT | `/api/seller/order/{orderId}/ship` | 发货 |
| PUT | `/api/seller/order/{orderId}/approve-return` | 同意退货 |
| PUT | `/api/seller/order/{orderId}/confirm-return` | 确认退货 |

**Feign 调用**：`ProductFeignClient`（库存）、`ShopFeignClient`（店铺）、`ContactFeignClient`（联系人）、`LogisticsFeignClient`（物流）

**依赖**：eureka_order 数据库、Redis（Stream + ID 生成）

---

### 3.5 联系人/地址模块（contact-service）

**职责**：用户收货联系人 + 商家发货/退货地址的 CRUD 及默认地址管理

**路径差异**：用户端 `/api/user/contact/*`，商家端 `/api/merchant/address/*`（非对称设计）

**核心文件**：
- `controller/UserContactController.java` - 用户联系人（`/api/user/contact/*`）
- `controller/MerchantContactController.java` - 商家地址（`/api/merchant/address/*`）
- `controller/internal/InternalContactController.java` - 内部查询（返回 `ContactDTO`）
- `service/UserContactService.java` / `impl/UserContactServiceImpl.java`
- `service/ShopAddressService.java` / `impl/ShopAddressServiceImpl.java`
- `model/Contact.java` - 用户联系人
- `model/ShopAddress.java` - 商家地址（含 addressType: 1收货/2发货）

**API 端点**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/contact/create` | 创建联系人 |
| PUT | `/api/user/contact/update` | 更新联系人（id 在 body） |
| DELETE | `/api/user/contact/delete/{id}` | 删除联系人 |
| GET | `/api/user/contact/list` | 联系人列表 |
| PUT | `/api/user/contact/set-default/{id}` | 设为默认 |
| POST | `/api/merchant/address/create` | 创建商家地址 |
| PUT | `/api/merchant/address/update/{id}` | 更新地址（id 在 path） |
| DELETE | `/api/merchant/address/delete/{id}` | 删除地址 |
| GET | `/api/merchant/address/list` | 地址列表 |
| GET | `/api/merchant/address/ship-default` | 默认发货地址 |
| PUT | `/api/merchant/address/set-default/{id}` | 设为默认 |
| GET | `/internal/contact/{id}` | 内部查联系人（Feign） |

**依赖**：eureka_contact（t_contact, user_contact, shop_address, shop_address_rel）

---

### 3.6 物流模块（logistics-service）

**职责**：物流记录 CRUD，按订单/运单号查询

**核心文件**：
- `controller/LogisticsController.java` - 外部 API（路径 `/logistics/*`，无 `/api/user/` 前缀）
- `controller/InternalLogisticsController.java` - 内部 API（`/internal/logistics/*`）
- `service/LogisticsService.java` / `impl/LogisticsServiceImpl.java`
- `model/Logistics.java` - 物流实体（orderId, type, contactId, trackingNumber）
- `dto/LogisticsConverter.java` - 转换器
- `dto/CreateLogisticsRequest.java` / `LogisticsResponse.java`

**API 端点**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/logistics/create` | 创建物流 |
| GET | `/logistics/list` | 全部物流 |
| GET | `/logistics/search/tracking?trackingNumber=` | 按运单号查 |
| DELETE | `/logistics/delete/{id}` | 删除 |
| GET | `/logistics/order/{orderId}` | 按订单查 |
| GET | `/logistics/order/{orderId}/latest?type=` | 按订单+类型查最新 |

**注意**：外部 API 路径为 `/logistics/*`（不含 `/api/user/` 前缀），与其它服务路径风格不同。

**依赖**：eureka_logistics（logistics）

---

### 3.7 店铺模块（shop-service）

**职责**：店铺 CRUD、员工管理、角色权限（店长/店员）

**核心文件**：
- `controller/ShopUserController.java` - 用户端（`/api/user/shop/*`）
- `controller/ShopMerchantController.java` - 商家端（`/api/seller/shop/*`）
- `controller/internal/InternalShopController.java` - 内部（角色/店铺信息查询）
- `service/ShopService.java` / `impl/ShopServiceImpl.java`
- `service/MerchantRoleService.java` / `impl/MerchantRoleServiceImpl.java`
- `service/ShopInfoService.java` / `impl/ShopInfoServiceImpl.java`
- `model/Shop.java` / `ShopInfo.java` / `MerchantRole.java`

**角色权限**：

| role | 名称 | 权限 | 校验方法 |
|------|------|------|---------|
| 1 | 店长 | 全部操作（更新/关店/员工管理） | `checkShopOwner()` |
| 2 | 店员 | 只读（查看店铺/员工列表） | `checkShopAccess()` |

**API 端点**：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/shop/list` | 活跃店铺列表 |
| GET | `/api/user/shop/{shopId}` | 店铺详情 |
| POST | `/api/seller/shop/register` | 注册店铺（自动成为店长） |
| GET | `/api/seller/shop/merchant/{merchantId}` | 商家关联店铺 |
| GET | `/api/seller/shop/{shopId}` | 店铺信息 |
| PUT | `/api/seller/shop/{shopId}` | 更新店铺（需店长） |
| DELETE | `/api/seller/shop/{shopId}` | 关闭店铺 |
| PUT | `/api/seller/shop/{shopId}/open` | 重新开店 |
| GET | `/api/seller/shop/{shopId}/employees` | 员工列表 |
| POST | `/api/seller/shop/{shopId}/employees/register` | 添加店员 |
| DELETE | `/api/seller/shop/{shopId}/employees/{merchantId}` | 移除店员 |

**依赖**：eureka_shop（t_shop, shop_info, merchant_role）

---

### 3.8 AI 聊天模块（chat-service）

**职责**：基于 LangChain4j 的 AI 购物助手，通过结构化输出回复商品推荐和订单查询

**核心文件**：
- `controller/ChatController.java` - `POST /chat/chat`
- `AiService/Assistant.java` - LangChain4j @AiService 接口
- `tools/ProductTools.java` - 商品工具（getAllProducts, getProductDetails）
- `tools/OrderTools.java` - 订单工具（getOrderById, getAllOrders, getOrdersByStatus）
- `context/UserContext.java` - InheritableThreadLocal，异步安全
- `config/UserContextInterceptor.java` - 从 X-User-Id Header 注入上下文
- `dto/Data.java` - sealed interface，permits ProductData / OrderData
- `dto/AiResponse.java` - 响应体（message, reason, data 多态）

**API 端点**：
- `POST /chat/chat` - AI 对话（路径为 `/chat/chat`，非 `/chat`）

**AI 工具**：
- `getAllProducts(page)` → 调用 product-service
- `getProductDetails(productId)` → 调用 product-service
- `getOrderById(orderId)` → 调用 order-service
- `getAllOrders()` → 调用 order-service
- `getOrdersByStatus(status)` → 内存过滤

**模型配置**：DashScope starter + `glm-5.1`（GLM），Temperature=0.7，无联网搜索

**依赖**：LangChain4j 0.35.0、Redis

---

## 4. 数据流

### 4.1 用户登录

```
用户输入账号密码
    ↓
POST /api/user/auth/login
    ↓
[Gateway] 白名单放行 → 路由到 auth-service
    ↓
查询 eureka_auth.t_user → 校验 BCrypt 密码
    ↓
Sa-Token 生成 Token → 存入 Redis（key: satoken:login:token:{uuid}, value: USER:{id}）
    ↓
返回 { token, userInfo }
    ↓
前端存入 localStorage，后续请求 Header 携带 satoken
```

### 4.2 创建订单

```
用户点击下单
    ↓
POST /api/user/order/place（Header: satoken）
    ↓
[Gateway] 校验 Token → 解析 USER:123 → 注入 X-User-Id:123
    ↓
order-service 调用 ProductFeignClient.reserveStock() 预占库存（FOR UPDATE）
    ↓
写入 t_order + t_user_order → 关联 ShopFeignClient
    ↓
事务提交后（afterCommit）发送 Redis Stream 事件
    ↓
OrderEventConsumer 消费 STOCK_CONFIRM → 确认库存扣减
```

### 4.3 订单后续流程

```
支付 POST /pay → PAID → 发 STOCK_CONFIRM 事件
    ↓
发货 POST /ship → SHIPPED → 发 LOGISTICS_CREATE 事件 → 创建物流
    ↓
收货 PUT /deliver → DELIVERED
    ↓
超时（OrderTimeoutTask 每1分钟）→ PENDING 超时 → CANCELLED → 发 STOCK_RESTORE 事件
    ↓
退货 → RETURN_PENDING → RETURNING → RETURNED → 发 STOCK_RESTORE 事件（幂等锁）
```

### 4.4 微服务间调用（Feign）

| 调用方 | 被调用方 | 用途 |
|--------|---------|------|
| order-service | product-service | 库存校验、预占、扣减、恢复 |
| order-service | shop-service | 获取店铺信息 |
| order-service | contact-service | 获取收货人信息 |
| order-service | logistics-service | 创建/查询物流 |
| product-service | shop-service | 获取店铺名称（Caffeine 缓存 10min） |
| shop-service | auth-service | 注册员工账号 |
| chat-service | product-service | AI 查询商品 |
| chat-service | order-service | AI 查询订单 |

---

## 5. 关键实体与模型

### 5.1 数据库

| 数据库 | 表名 | 说明 |
|--------|------|------|
| eureka_auth | t_user | 消费者用户 |
| eureka_auth | user_info | 用户扩展信息 |
| eureka_auth | t_merchant | 商家用户（含店员） |
| eureka_auth | merchant_info | 商家扩展信息 |
| eureka_product | products | 商品 |
| eureka_product | salable_products | 可售商品清单 |
| eureka_product | product_image_info | 商品图片 |
| eureka_product | product_reservation | 库存预占记录 |
| eureka_order | t_order | 订单 |
| eureka_order | t_user_order | 用户-订单关联 |
| eureka_order | deleted_orders | 删除订单备份 |
| eureka_contact | t_contact | 用户联系人 |
| eureka_contact | user_contact | 用户-联系人关联 |
| eureka_contact | shop_address | 商家地址 |
| eureka_contact | shop_address_rel | 商家-地址关联 |
| eureka_shop | t_shop | 店铺 |
| eureka_shop | shop_info | 店铺信息 |
| eureka_shop | merchant_role | 商家角色（店长/店员） |
| eureka_logistics | logistics | 物流 |

### 5.2 核心实体

| 实体 | 所在模块 | 说明 |
|------|---------|------|
| User / UserInfo | auth-service | 消费者用户 + 扩展信息 |
| Merchant / MerchantInfo | auth-service | 商家用户 + 扩展信息 |
| Product | product-service | 商品（price: BigDecimal, stock: Integer） |
| ProductImageInfo | product-service | 商品图片 |
| ProductReservation | product-service | 库存预占（FOR UPDATE 行级锁） |
| Order | order-service | 订单 + 状态机 TRANSITIONS Map |
| DeletedOrder | order-service | 删除前备份 |
| Contact | contact-service | 用户联系人 |
| ShopAddress | contact-service | 商家地址（含 addressType） |
| Shop | shop-service | 店铺 |
| ShopInfo | shop-service | 店铺信息 |
| MerchantRole | shop-service | 商家角色（role=1 店长, 2 店员） |
| Logistics | logistics-service | 物流 |

### 5.3 统一契约模块（common-api）

**核心文件**：
- `response/ApiResponse.java` - 统一响应（code + message + data，泛型 T）
- `util/SnowflakeIdGenerator.java` - 雪花 ID（Hutool，workerId=1, datacenterId=1）
- `feign/ProductFeignClient.java` - 商品服务 Feign（12 个方法）
- `feign/OrderFeignClient.java` - 订单服务 Feign
- `feign/ShopFeignClient.java` - 店铺服务 Feign
- `feign/ContactFeignClient.java` - 联系人服务 Feign
- `feign/LogisticsFeignClient.java` - 物流服务 Feign
- `feign/AuthFeignClient.java` - 认证服务 Feign
- `dto/*/ProductDTO.java` / `StockDeductRequest.java` / `StockReserveRequest.java`
- `dto/*/ShopInfoDTO.java` / `ShopDTO.java` / `MerchantRoleDTO.java`
- `dto/*/OrderDTO.java` / `OrderAbstractSellerDTO.java` / `ShipOrderRequest.java`
- `dto/*/ContactDTO.java` / `LogisticsRequest.java`

**注意**：早期 README 描述有 4 个独立 API 模块（product-api / order-api / contact-api / logistics-api），实际**只有一个 `common-api` 模块**。

### 5.4 关键配置

| 配置项 | 值 | 说明 |
|--------|-----|------|
| Eureka | http://admin:admin@localhost:8761/eureka/ | 注册中心（需 basic auth） |
| Sa-Token 超时 | 86400s（1天） | Token 有效期 |
| Sa-Token 临时超时 | 1800s（30分钟无操作过期） | 滑动过期 |
| 密码加密 | BCrypt strength=12 | 约 300ms 每次 |
| IP 限流 | 300 次 / 60 秒 / IP | Redis 分布式 |
| 订单超时 | 30 分钟 | PENDING → 自动取消 |
| 预占超时 | 30 分钟 | RESERVED → 自动释放（每2分钟清理） |
| 订单 ID | 日期8位+序号5位+随机5位字母 | 18位，Redis INCR 生成 |
| 店铺缓存 | Caffeine 10分钟 / 1000条 | product-service 缓存 shopInfo |

---

## 6. 修改影响链

| 文件 | 影响 |
|------|------|
| Gateway 过滤器 / 路由配置 | 所有 API 的认证和路由 |
| AuthController / AuthService | 登录/注册逻辑 |
| User/Merchant 实体 | 登录返回数据结构 |
| ProductService / ProductReservationService | 库存管理、订单创建 |
| ReservationCleanupTask | 定时清理过期预占 |
| OrderService / OrderStatusMachine | 订单全生命周期（状态机+事件） |
| OrderEventConsumer | Redis Stream 异步事件处理 |
| FileFallbackDaemon | Redis 不可用时的消息兜底 |
| OrderTimeoutTask | 超时未支付自动取消 |
| RedisOrderIdGenerator | 订单 ID 生成规则 |
| UserContactController | 用户联系人管理 |
| MerchantContactController | 商家地址管理 |
| LogisticsService | 物流信息 |
| ShopService / MerchantRoleService | 店铺管理 + 角色权限 |
| Assistant (AiService) + Tools | AI 对话与工具调用 |
| UserContext + Interceptor | 用户上下文异步传递 |
| Feign 客户端（common-api） | 所有跨服务 Feign 调用 |
| 前端 request.js | Token 注入 |
| 前端路由配置文件 | 页面路由 |

---

## 7. 开发指令

### 后端启动
```bash
# Windows
cd AI-Shopping-backend_Eureka
start-end.bat

# 或手动（Maven）
cd eureka-server   ; mvn spring-boot:run
cd gateway-service ; mvn spring-boot:run
cd auth-service    ; mvn spring-boot:run
# 其余服务无启动顺序依赖
```

**启动顺序**：Eureka → Gateway → Auth → 其他

### 前端启动
```bash
# Windows
start-frontier.bat

# 手动
cd AI-Shopping-frontier/frontier-user   ; npm run dev
cd AI-Shopping-frontier/frontier-seller ; npm run dev
```

### 端口

| 服务 | 端口 |
|------|------|
| eureka-server | 8761 |
| gateway-service | 8080 |
| auth-service | 8086 |
| product-service | 8081 |
| shop-service | 8087 |
| contact-service | 8083 |
| logistics-service | 8084 |
| order-service | 8082 |
| chat-service | 8085 |
| frontier-user | 5173 |
| frontier-seller | 5174 |

---

## 8. 角色权限

| 角色 | ID 前缀 | 路径 | 说明 |
|------|---------|------|------|
| 用户 | `USER:` | `/api/user/**` | 普通消费者 |
| 店长 | `MERCHANT:` | `/api/seller/**` | role=1，店铺全部权限 |
| 店员 | `MERCHANT:` | `/api/seller/**` | role=2，只读权限 |

**权限校验**：
- 认证层：Gateway `SaTokenAuthGlobalFilter` — Token 校验 + 路径权限
- 业务层：shop-service `checkShopOwner()` / `checkShopAccess()` — 店铺级别权限

---

> ✅ 本索引表可直接嵌入 AI 对话开头使用。
