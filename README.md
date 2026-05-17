# AI-Shopping 项目快速索引表

> 本文件为 AI 对话快速理解项目的索引文档，可直接嵌入对话开头使用。

---

## 1. 项目概述

**一句话说明**：基于 Spring Cloud Eureka 的 AI 智能购物平台，支持用户购物和商家入驻双端功能。

**技术栈**：
- 后端：Java 17 + Spring Boot 3.2.3 + Spring Cloud 2023.0.0 + MyBatis
- 前端：Vue 3 + Vite + Tailwind CSS
- 认证：Sa-Token（Redis 存储）
- AI：LangChain4j 0.35.0
- 注册中心：Eureka
- 网关：Spring Cloud Gateway
- 数据库：MySQL（多库分离）
- 缓存：Redis

---

## 2. 目录结构图

```
AI-Shopping/
├── AI-Shopping-backend_Eureka/          # 后端微服务根目录
│   ├── pom.xml                          # 父 POM（版本统一管理）
│   ├── eureka-server/                  # 服务注册中心（端口8761）
│   ├── gateway-service/                # API网关（端口8080）
│   ├── common-api/                     # 公共API（Feign接口、DTO）
│   ├── auth-service/                   # 认证服务（端口8086）
│   ├── product-service/                # 商品服务（端口8081）
│   ├── shop-service/                   # 店铺服务（端口8087）
│   ├── order-service/                  # 订单服务（端口8082）
│   ├── contact-service/                # 联系人和地址服务（端口8083）
│   ├── logistics-service/             # 物流服务（端口8084）
│   ├── chat-service/                   # AI聊天服务（端口8085）
│   └── sql/                            # 数据库初始化脚本
│       ├── init/                       # 建表脚本
│       └── insert/                     # 数据插入脚本
│
├── AI-Shopping-frontier/               # 前端根目录
│   ├── frontier-user/                  # 用户端前端（Vue 3 + Vite）
│   │   ├── src/
│   │   │   ├── api/                    # API 请求封装
│   │   │   ├── components/            # 公共组件
│   │   │   ├── views/                  # 页面视图
│   │   │   ├── router/                 # 路由配置
│   │   │   ├── config/                 # 配置文件
│   │   │   └── utils/                  # 工具函数
│   │   └── vite.config.js
│   │
│   └── frontier-seller/                # 商家端前端
│       ├── src/
│       │   ├── api/
│       │   ├── views/
│       │   ├── merchant/               # 商家业务组件
│       │   ├── router/
│       │   ├── config/
│       │   └── utils/
│       └── vite.config.js
│
├── start-frontier.bat                  # 启动前端脚本
├── stop-frontier.bat                   # 停止前端脚本
├── AI-Shopping-backend_Eureka/start-end.bat   # 启动后端脚本
├── AI-Shopping-backend_Eureka/stop-end.bat    # 停止后端脚本
└── pom.xml                             # 根 POM
```

---

## 3. 功能区块划分

### 3.1 认证模块（auth-service）

**职责**：用户和商家的注册、登录、登出、Token 校验

**核心文件**：
- `auth-service/src/main/java/com/gzasc/aishopping/auth/controller/AuthController.java` - 认证控制器
- `auth-service/src/main/java/com/gzasc/aishopping/auth/service/AuthService.java` - 认证服务接口
- `auth-service/src/main/java/com/gzasc/aishopping/auth/service/impl/AuthServiceImpl.java` - 认证服务实现
- `auth-service/src/main/java/com/gzasc/aishopping/auth/mapper/user/UserMapper.java` - 用户数据访问
- `auth-service/src/main/java/com/gzasc/aishopping/auth/mapper/merchant/MerchantMapper.java` - 商家数据访问
- `auth-service/src/main/java/com/gzasc/aishopping/auth/model/User.java` - 用户实体
- `auth-service/src/main/java/com/gzasc/aishopping/auth/model/Merchant.java` - 商家实体
- `common-api/src/main/java/com/gzasc/aishopping/common/util/SnowflakeIdGenerator.java` - 雪花算法ID生成

**对外接口**：
- `POST /api/user/auth/register` - 用户注册
- `POST /api/user/auth/login` - 用户登录
- `POST /api/user/auth/logout` - 用户登出
- `GET /api/user/auth/info` - 获取用户信息
- `POST /api/seller/auth/register` - 商家注册
- `POST /api/seller/auth/login` - 商家登录

**依赖**：Redis（Token存储）、eureka_user/eureka_merchant 数据库

**影响**：修改用户/商家实体需同步修改前端登录逻辑

---

### 3.2 API网关模块（gateway-service）

**职责**：请求路由、Token 校验、IP 限流、角色权限控制、CORS

**核心文件**：
- `gateway-service/src/main/java/com/gzasc/aishopping/gateway/filter/SaTokenAuthGlobalFilter.java` - Sa-Token 认证过滤器
- `gateway-service/src/main/java/com/gzasc/aishopping/gateway/filter/IpRateLimitFilter.java` - IP 限流过滤器
- `gateway-service/src/main/resources/application.yml` - 网关配置（路由规则）

**关键逻辑**：
- 白名单路径放行：`/api/user/auth/login`, `/api/user/auth/register`, `/api/seller/auth/*`
- Token 格式：`satoken:login:token:{token值}` 存储在 Redis
- 登录ID格式：`USER:{userId}` 或 `MERCHANT:{merchantId}`
- 路由规则：用户端 `/api/user/**` → 对应服务，商家端 `/api/seller/**` → 对应服务

**依赖**：Redis（Token验证）

**影响**：修改路由规则需同步修改前端 API 调用路径

---

### 3.3 商品模块（product-service）

**职责**：商品 CRUD、库存管理、商品搜索

**核心文件**：
- `product-service/src/main/java/com/gzasc/aishopping/product/controller/ProductUserController.java` - 用户端商品控制器
- `product-service/src/main/java/com/gzasc/aishopping/product/controller/ProductSellerController.java` - 商家端商品控制器
- `product-service/src/main/java/com/gzasc/aishopping/product/service/ProductService.java` - 商品服务
- `product-service/src/main/java/com/gzasc/aishopping/product/model/Product.java` - 商品实体
- `product-service/src/main/java/com/gzasc/aishopping/product/mapper/ProductMapper.java` - 商品Mapper

**对外接口**：
- `GET /api/user/product/all` - 获取所有商品
- `GET /api/user/product/{productId}` - 获取商品详情
- `GET /api/user/product/search?name=xxx` - 搜索商品
- `POST /api/seller/product/add` - 添加商品
- `PUT /api/seller/product/update` - 更新商品
- `DELETE /api/seller/product/{productId}` - 删除商品

**依赖**：eureka_product 数据库

**影响**：商品数据变更影响订单创建（库存校验）

---

### 3.4 订单模块（order-service）

**职责**：订单创建、查询、取消、状态管理、用户订单关联

**核心文件**：
- `order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderUserController.java` - 用户端订单控制器
- `order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderSellerController.java` - 商家端订单控制器
- `order-service/src/main/java/com/gzasc/aishopping/order/controller/internal/InternalOrderController.java` - 内部订单控制器
- `order-service/src/main/java/com/gzasc/aishopping/order/service/OrderService.java` - 订单服务接口
- `order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java` - 订单服务实现
- `order-service/src/main/java/com/gzasc/aishopping/order/model/Order.java` - 订单实体
- `order-service/src/main/java/com/gzasc/aishopping/order/model/UserOrder.java` - 用户订单关联实体

**订单状态**：PENDING（待支付）→ PAID（待发货）→ SHIPPED（已发货）→ DELIVERED（已送达）| CANCELLED（已取消）| RETURNED（已退货）

**对外接口**：
- `GET /api/user/order/{orderId}` - 获取订单详情
- `GET /api/user/order/list` - 获取用户订单列表
- `POST /api/user/order/place` - 创建订单
- `DELETE /api/user/order/{orderId}` - 取消订单
- `PUT /api/user/order/{orderId}/status?status=xxx` - 更新订单状态

**Feign调用**：
- `ProductFeignClient` - 调用商品服务校验库存
- `ShopFeignClient` - 关联订单到店铺

**依赖**：eureka_order 数据库、product-service（Feign）、shop-service（Feign）

**影响**：订单创建触发库存扣减；订单取消恢复库存

---

### 3.5 联系人/地址模块（contact-service）

**职责**：用户联系人管理、商家发货/退货地址管理

**核心文件**：
- `contact-service/src/main/java/com/gzasc/aishopping/contact/controller/ContactController.java` - 用户联系人控制器
- `contact-service/src/main/java/com/gzasc/aishopping/contact/controller/ShopAddressSellerController.java` - 商家地址控制器
- `contact-service/src/main/java/com/gzasc/aishopping/contact/service/ContactService.java` - 联系人服务
- `contact-service/src/main/java/com/gzasc/aishopping/contact/model/Contact.java` - 联系人实体
- `contact-service/src/main/java/com/gzasc/aishopping/contact/model/ShopAddress.java` - 商家地址实体

**对外接口**：
- `GET /api/user/contact/list` - 获取用户联系人列表
- `POST /api/user/contact/add` - 添加联系人
- `DELETE /api/user/contact/{id}` - 删除联系人
- `GET /api/seller/address/list` - 获取商家地址列表
- `POST /api/seller/address/add` - 添加商家地址

**依赖**：eureka_contact 数据库

**影响**：订单创建需要联系人ID

---

### 3.6 物流模块（logistics-service）

**职责**：物流信息管理、发货

**核心文件**：
- `logistics-service/src/main/java/com/gzasc/aishopping/logistics/controller/LogisticsController.java` - 物流控制器
- `logistics-service/src/main/java/com/gzasc/aishopping/logistics/service/LogisticsService.java` - 物流服务
- `logistics-service/src/main/java/com/gzasc/aishopping/logistics/model/Logistics.java` - 物流实体

**对外接口**：
- `POST /api/user/logistics/create` - 创建物流信息
- `GET /api/user/logistics/{id}` - 获取物流信息

**依赖**：eureka_logistics 数据库

---

### 3.7 店铺模块（shop-service）

**职责**：店铺注册、店铺管理、店铺商品、店铺订单关联

**核心文件**：
- `shop-service/src/main/java/com/gzasc/aishopping/shop/controller/ShopUserController.java` - 用户端店铺控制器
- `shop-service/src/main/java/com/gzasc/aishopping/shop/controller/ShopSellerController.java` - 商家端店铺控制器
- `shop-service/src/main/java/com/gzasc/aishopping/shop/service/ShopService.java` - 店铺服务
- `shop-service/src/main/java/com/gzasc/aishopping/shop/model/Shop.java` - 店铺实体

**对外接口**：
- `POST /api/seller/shop/register` - 注册店铺
- `GET /api/seller/shop/list` - 获取商家店铺列表
- `GET /api/user/shop/list` - 获取店铺列表

**依赖**：eureka_shop 数据库

---

### 3.8 AI聊天模块（chat-service）

**职责**：AI 智能购物助手（基于 LangChain4j）

**核心文件**：
- `chat-service/src/main/java/com/gzasc/aishopping/chat/controller/ChatController.java` - 聊天控制器
- `chat-service/src/main/java/com/gzasc/aishopping/chat/AiService/Assistant.java` - AI 助手服务
- `chat-service/src/main/java/com/gzasc/aishopping/chat/tools/ProductTools.java` - 商品查询工具
- `chat-service/src/main/java/com/gzasc/aishopping/chat/tools/OrderTools.java` - 订单查询工具

**对外接口**：
- `POST /chat/chat` - AI 对话接口

**依赖**：LangChain4j（AI框架）

---

### 3.9 前端模块（frontier-user / frontier-seller）

**用户端（frontier-user）**：

核心页面/组件：
- `src/views/Login/Login.vue` - 登录页面
- `src/components/ChatWindow/ChatWindow.vue` - AI 聊天窗口（首页）
- `src/components/Contact/ContactManager.vue` - 联系人管理
- `src/components/OrderManager/OrderManager.vue` - 订单管理
- `src/components/Order/OrderDialog.vue` - 订单详情对话框
- `src/components/ProductCard/ProductCard.vue` - 商品卡片
- `src/components/Payment/PaymentDialog.vue` - 支付对话框

路由（`src/router/index.js`）：
- `/login` - 登录页
- `/` - 聊天首页（核心）
- `/contact` - 联系人管理
- `/order` - 订单管理

API 封装（`src/api/`）：
- `auth.js` - 认证 API
- `product.js` - 商品 API
- `order.js` - 订单 API
- `contact.js` - 联系人 API
- `logistics.js` - 物流 API
- `chat.js` - 聊天 API
- `request.js` - axios 封装（Token 自动注入）

**商家端（frontier-seller）**：

核心页面：
- `src/views/Login/Login.vue` - 登录页面
- `src/merchant/MerchantShip/MerchantShip.vue` - 发货管理（首页）
- `src/views/shop/ShopRegister.vue` - 店铺注册
- `src/views/shop/ShopList.vue` - 店铺列表
- `src/views/shop/ShopProducts.vue` - 店铺商品管理
- `src/views/shop/ShopOrders.vue` - 店铺订单管理
- `src/views/shop/ShopEmployees.vue` - 员工管理
- `src/views/shop/ShopAddresses.vue` - 地址管理

路由（`src/router/index.js`）：
- `/login` - 登录
- `/ship` - 发货管理
- `/shop/register` - 店铺注册
- `/shop/list` - 店铺列表
- `/shop/:shopId/products` - 商品管理
- `/shop/:shopId/orders` - 订单管理

---

## 4. 数据流与交互图

### 4.1 用户登录流程

```
用户输入账号密码
    ↓
POST /api/user/auth/login
    ↓
[Gateway 8080] → 路由到 auth-service（白名单放行）
    ↓
AuthService.userLogin() → 查询 eureka_user.t_user
    ↓
生成 Token（Sa-Token）→ 存储到 Redis（key: satoken:login:token:{uuid}）
    ↓
返回 { token, userInfo }
    ↓
前端存储 token 到 localStorage
    ↓
后续请求在 Header 携带 satoken
```

### 4.2 创建订单流程

```
用户选择商品 → 点击下单
    ↓
POST /api/user/order/place（Header: satoken）
    ↓
[Gateway] 校验 Token → 解析 loginId = "USER:123"
    ↓
添加 Header: X-User-Id: 123 → 路由到 order-service
    ↓
OrderUserController.placeOrder()
    ↓
Feign 调用 product-service 校验库存
    ↓
OrderService.createOrder() → 写入 eureka_order.t_order
    ↓
OrderService.createUserOrder() → 写入 eureka_order.t_user_order
    ↓
Feign 调用 shop-service 关联订单到店铺
    ↓
返回订单创建成功
```

### 4.3 核心调用链

```
HTTP 请求
    ↓
[Gateway:8080] → SaTokenAuthGlobalFilter 校验
    ↓ (通过)
路由到对应微服务（lb://service-name）
    ↓
Controller 接收请求（通过 Header: X-User-Id 获取用户ID）
    ↓
Service 处理业务逻辑
    ↓
Mapper 操作数据库
    ↓
返回结果
```

### 4.4 微服务间调用（Feign）

- **order-service** 调用 **product-service**：`ProductFeignClient.getProductById()`
- **order-service** 调用 **shop-service**：`ShopFeignClient.getShopIdByProductId()`
- **contact-service** 被 **shop-service** 内部调用查询商家地址

---

## 5. 关键实体与模型

### 5.1 数据库表

| 数据库 | 表名 | 说明 |
|--------|------|------|
| eureka_user | user_info | 用户基础信息（昵称、头像） |
| eureka_user | t_user | 消费者用户表 |
| eureka_merchant | t_merchant | 商家用户表 |
| eureka_product | products | 商品表 |
| eureka_order | t_order | 订单表 |
| eureka_order | t_user_order | 用户订单关联表 |
| eureka_order | deleted_orders | 已删除订单备份表 |
| eureka_contact | t_contact | 用户联系人表 |
| eureka_contact | user_contact | 用户-联系人关联表 |
| eureka_contact | shop_address | 商家地址表 |
| eureka_contact | shop_address_rel | 商店地址关联表 |
| eureka_logistics | logistics | 物流信息表 |

### 5.2 核心实体类

| 实体 | 路径 |
|------|------|
| User | `auth-service/.../model/User.java` |
| Merchant | `auth-service/.../model/Merchant.java` |
| Product | `product-service/.../model/Product.java` |
| Order | `order-service/.../model/Order.java` |
| UserOrder | `order-service/.../model/UserOrder.java` |
| Contact | `contact-service/.../model/Contact.java` |
| ShopAddress | `contact-service/.../model/ShopAddress.java` |
| Shop | `shop-service/.../model/Shop.java` |
| Logistics | `logistics-service/.../model/Logistics.java` |

### 5.3 关键配置

| 配置项 | 位置 | 说明 |
|--------|------|------|
| Redis | `gateway-service/application.yml` | 主机: localhost:6379 |
| Eureka | 各服务 application.yml | 注册地址: http://admin:admin@localhost:8761/eureka/ |
| Sa-Token | `application.yml` | 超时: 2592000秒（30天） |
| IP限流 | `gateway-service` | 每60秒最多300次请求 |

---

## 6. 修改影响链快速参考

| 修改文件/组件 | 可能影响 |
|--------------|---------|
| `gateway-service/.../SaTokenAuthGlobalFilter.java` | 所有需要认证的 API |
| `auth-service/.../AuthController.java` | 前端登录/注册逻辑 |
| `auth-service/.../User.java` 或 `Merchant.java` | 登录返回数据结构、前端解析 |
| `product-service/.../ProductService.java` | 订单创建时的库存校验 |
| `order-service/.../OrderUserController.java` | 用户端订单功能 |
| `order-service/.../OrderServiceImpl.java` | 订单创建、取消（库存恢复） |
| `contact-service/.../ContactController.java` | 用户端联系人管理 |
| `common-api/.../ProductFeignClient.java` | order-service 对商品服务的调用 |
| `common-api/.../ShopFeignClient.java` | order-service 对店铺服务的调用 |
| `frontier-user/src/api/request.js` | 所有前端 API 请求（Token 注入） |
| `frontier-user/src/router/index.js` | 用户端页面路由 |
| `frontier-seller/src/router/index.js` | 商家端页面路由 |

---

## 7. 常用开发指令

### 7.1 后端启动

```bash
# 方式1：使用启动脚本
cd AI-Shopping-backend_Eureka
start-end.bat

# 方式2：手动逐个启动（Maven）
cd eureka-server && mvn spring-boot:run
cd gateway-service && mvn spring-boot:run
cd auth-service && mvn spring-boot:run
# ... 其他服务
```

**启动顺序**：Eureka → Gateway → Auth → 其他服务

### 7.2 前端启动

```bash
# 方式1：使用启动脚本
start-frontier.bat

# 方式2：手动启动
cd AI-Shopping-frontier/frontier-user && npm run dev
cd AI-Shopping-frontier/frontier-seller && npm run dev
```

**前端端口**：
- 用户端：http://localhost:5173
- 商家端：http://localhost:5174

### 7.3 数据库初始化

```bash
# 执行 SQL 脚本（MySQL）
source AI-Shopping-backend_Eureka/sql/init/auth-init.sql
source AI-Shopping-backend_Eureka/sql/init/01-product-init.sql
source AI-Shopping-backend_Eureka/sql/init/02-order-init.sql
source AI-Shopping-backend_Eureka/sql/init/03-contact-init.sql
source AI-Shopping-backend_Eureka/sql/init/04-logistics-init.sql
```

---

## 8. 服务端口一览

| 服务 | 端口 | 用途 |
|------|------|------|
| eureka-server | 8761 | 服务注册中心 |
| gateway-service | 8080 | API 网关（前端请求入口） |
| auth-service | 8086 | 认证服务 |
| product-service | 8081 | 商品服务 |
| shop-service | 8087 | 店铺服务 |
| contact-service | 8083 | 联系人/地址服务 |
| logistics-service | 8084 | 物流服务 |
| order-service | 8082 | 订单服务 |
| chat-service | 8085 | AI 聊天服务 |
| frontier-user | 5173 | 用户端前端 |
| frontier-seller | 5174 | 商家端前端 |

---

## 9. 角色权限说明

| 角色 | ID 前缀 | 可访问路径 | 说明 |
|------|---------|------------|------|
| 用户 | `USER:` | `/api/user/**` | 普通消费者 |
| 商家 | `MERCHANT:` | `/api/seller/**` | 店铺经营者 |

**权限校验位置**：`SaTokenAuthGlobalFilter.hasPermission()`（gateway-service）

---

> ✅ 本索引表可直接嵌入 AI 对话开头使用，后续开发者在理解项目时可直接引用此文档。