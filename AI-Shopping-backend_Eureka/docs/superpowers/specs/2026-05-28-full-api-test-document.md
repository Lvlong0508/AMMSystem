# AI-Shopping 微服务全量接口测试文档

> 版本：v1.1
> 日期：2026-05-28
> 覆盖范围：Gateway / Auth / Product / Order / Contact / Logistics / Shop（共 7 个微服务）

---

## 目录

1. [通用约定](#1-通用约定)
2. [Gateway 服务](#2-gateway-服务)
3. [Auth 服务](#3-auth-服务)
4. [Product 服务](#4-product-服务)
5. [Order 服务](#5-order-服务)
6. [Contact 服务](#6-contact-服务)
7. [Logistics 服务](#7-logistics-服务)
8. [Shop 服务](#8-shop-服务)

---

## 1. 通用约定

### 1.1 响应格式

所有接口统一返回 `ApiResponse<T>`：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 业务错误 / 参数校验失败 |
| 401 | 未登录 / Token 过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 429 | 请求频率超限 |
| 500 | 系统错误 |

### 1.2 认证方式

- **用户端**：Gateway 拦截 `satoken` header，校验后注入 `X-User-Id`
- **商家端**：Gateway 拦截 `satoken` header，校验后注入 `X-User-Id`
- **联系人/地址服务**：直接读取 `X-User-Id` / `X-Shop-Id` header
- **白名单路径**：登录/注册/用户名检查/店铺注册 无需 token

### 1.3 测试环境要求

| 组件 | 要求 |
|------|------|
| MySQL | eureka_auth / eureka_product / eureka_order / eureka_contact / eureka_logistics / eureka_shop |
| Redis | localhost:6379，用于 Sa-Token 和订单号生成 |
| Eureka | http://admin:admin@localhost:8761/eureka/ |
| 服务启动顺序 | eureka → gateway → auth → product → order → contact → logistics → shop |

---

## 2. Gateway 服务

### 2.1 服务概述

| 项目 | 值 |
|------|-----|
| 端口 | 8080 |
| 应用名 | gateway-service |
| 底层依赖 | Spring Cloud Gateway + Eureka + Redis + Caffeine |
| 过滤器链 | IpRateLimitFilter(Order=-200) → SaTokenAuthGlobalFilter(Order=-100) → Route |

### 2.2 认证白名单（无需 Token）

| # | 路径 |
|---|------|
| 1 | `POST /api/user/auth/login` |
| 2 | `POST /api/user/auth/register` |
| 3 | `GET /api/user/auth/check-username` |
| 4 | `GET /api/user/auth/check-phone` |
| 5 | `POST /api/seller/auth/login` |
| 6 | `POST /api/seller/auth/register` |
| 7 | `GET /api/seller/auth/check-username` |
| 8 | `GET /api/seller/auth/check-phone` |
| 9 | `POST /api/seller/shop/register` |

### 2.3 路由转发测试

#### 用户端路由（7 条）

| 测试 | 请求 | 预期目标 | 预期转发路径 |
|------|------|---------|-------------|
| GW-R01 | `GET /api/user/auth/login` | auth-service:8086 | `/api/user/auth/login` |
| GW-R02 | `POST /api/user/product/all` | product-service:8081 | `/api/user/product/all` |
| GW-R03 | `GET /api/user/order/list` | order-service:8082 | `/api/user/order/list` |
| GW-R04 | `POST /api/user/contact/create` | contact-service:8083 | `/api/user/contact/create` |
| GW-R05 | `GET /api/user/logistics/list` | logistics-service:8084 | `/api/user/logistics/list` |
| GW-R06 | `POST /api/user/chat` | chat-service:8085 | `/chat` |
| GW-R06a | `GET /api/user/chat/history?userId=1` | chat-service:8085 | `/chat/history?userId=1` |
| GW-R07 | `GET /api/user/shop/list` | shop-service:8087 | `/api/user/shop/list` |

**验证点**：请求到达目标服务的正确 Controller、响应中无网关错误；chat 路由子路径（如 `/history`）正确传递

#### 商家端路由（8 条）

| 测试 | 请求 | 预期目标 | 预期转发路径 |
|------|------|---------|-------------|
| GW-R08 | `GET /api/seller/auth/login` | auth-service:8086 | `/api/seller/auth/login` |
| GW-R09 | `POST /api/seller/product/create` | product-service:8081 | `/api/seller/product/create` |
| GW-R10 | `PUT /api/seller/order/{id}/ship` | order-service:8082 | `/api/seller/order/{id}/ship` |
| GW-R11 | `GET /api/seller/contact/list` | contact-service:8083 | `/api/seller/contact/list` |
| GW-R12 | `POST /api/merchant/address/create` | contact-service:8083 | `/api/merchant/address/create` |
| GW-R13 | `GET /api/seller/logistics/list` | logistics-service:8084 | `/api/seller/logistics/list` |
| GW-R14 | `POST /api/seller/chat` | chat-service:8085 | `/chat` |
| GW-R14a | `GET /api/seller/chat/history?shopId=1` | chat-service:8085 | `/chat/history?shopId=1` |
| GW-R15 | `GET /api/seller/shop/merchant/1` | shop-service:8087 | `/api/seller/shop/merchant/1` |

#### 内部路由（4 条，StripPrefix=1）

| 测试 | 请求 | 预期转发路径 |
|------|------|-------------|
| GW-R16 | `POST /internal/product/deduct-stock` | `/product/deduct-stock` |
| GW-R17 | `GET /internal/product/batch?ids=1,2` | `/product/batch?ids=1,2` |
| GW-R18 | `GET /internal/logistics/order/xxx/latest?type=DELIVERY` | `/logistics/order/xxx/latest?type=DELIVERY` |
| GW-R19 | `GET /internal/shop/info/1` | `/shop/info/1` |

### 2.4 认证拦截测试

| 测试 | 路径 | Token 状态 | Redis 数据 | 预期 |
|------|------|-----------|-----------|------|
| GW-A01 | `GET /api/user/product/all` | 无 | - | 401 `未登录` |
| GW-A02 | `GET /api/user/product/all` | `satoken: ""` | - | 401 `未登录` |
| GW-A03 | `GET /api/user/product/all` | `satoken: valid-uuid` | `satoken:login:token:valid-uuid` → `USER:1` | 200，传递 userId |
| GW-A04 | `GET /api/user/product/all` | `satoken: expired-uuid` | key 不存在 | 401 `登录已过期` |
| GW-A05 | `GET /api/user/product/all` | `satoken: fake-uuid` | key 不存在 | 401 `登录已过期` |

### 2.5 角色权限测试

| 测试 | 路径 | loginId | 额外 Header | 预期 |
|------|------|---------|------------|------|
| GW-P01 | `/api/user/order/list` | `MERCHANT:1` | - | 403 `无权限` |
| GW-P02 | `/api/seller/product/list` | `USER:1` | - | 403 `无权限` |
| GW-P03 | `/api/user/order/list` | `USER:1` | - | 放行 |
| GW-P04 | `/api/seller/product/list` | `MERCHANT:1` | - | 放行 |
| GW-P05 | `/api/seller/shop/manage/settings` | `MERCHANT:2` | X-Merchant-Role: null | 403 `无权限` |
| GW-P06 | `/api/seller/shop/manage/settings` | `MERCHANT:2` | X-Merchant-Role: 2 | 403 `非店长` |
| GW-P07 | `/api/seller/shop/manage/settings` | `MERCHANT:3` | X-Merchant-Role: 1, X-Shop-Id: 10 | 放行 |
| GW-P08 | `/api/seller/shop/manage/settings` | `MERCHANT:3` | X-Merchant-Role: 1, 无 X-Shop-Id | 403 |

### 2.6 IP 限流测试

| 测试 | 条件 | 预期 |
|------|------|------|
| GW-L01 | 同一 IP 60s 内 50 次请求 | 全部放行 |
| GW-L02 | 同一 IP 60s 内 301 次请求 | 第 301 次返回 429 |
| GW-L03 | 等待 60s 后同一 IP | 计数器重置，正常放行 |
| GW-L04 | 两个不同 IP 各 300 次 | 互不影响，全部放行 |

### 2.7 CORS / OPTIONS 测试

| 测试 | 请求 | 预期 |
|------|------|------|
| GW-C01 | `OPTIONS /api/user/product/all` | 直接放行，返回 CORS 头 |
| GW-C02 | 响应头验证 | `Access-Control-Allow-Origin: *`、`Access-Control-Allow-Methods: *` |

---

## 3. Auth 服务

### 3.1 服务概述

| 项目 | 值 |
|------|-----|
| 端口 | 8086 |
| 数据库 | eureka_auth（t_user / t_merchant / user_info / merchant_info） |
| 认证 | Sa-Token（Redis 存储） |
| 密码 | BCrypt（strength=12） |
| ID | 雪花算法 |

### 3.2 用户认证 API（/api/user/auth）

#### 注册

| 测试 | 场景 | 请求体 | 预期 |
|------|------|--------|------|
| AU-R01 | 正常注册 | `{username:"test1", password:"Abc123", phone:"13800138000"}` | 200，返回 token + userInfo |
| AU-R02 | 重复用户名 | 同上，再次请求 | 400 `用户名已存在` |
| AU-R03 | 空用户名 | `{username:"", password:"Abc123"}` | 400 参数校验错误 |
| AU-R04 | 用户名字符校验 | `{username:"user@name", password:"Abc123"}` | 400 `只能包含字母、数字、下划线` |
| AU-R05 | 密码强度校验 | `{username:"test2", password:"123"}` | 400 参数校验错误 |
| AU-R06 | 空密码 | `{username:"test2", password:""}` | 400 `密码不能为空` |
| AU-R07 | 注册时带昵称 | `+ nickname:"用户昵称"` | 创建 user_info 记录，userInfo 含 nickname |
| AU-R08 | 重复手机号 | 不同用户名使用已注册手机号 | 400 `手机号已被注册` |

#### 登录

| 测试 | 场景 | 输入 | 预期 |
|------|------|------|------|
| AU-L01 | 正常登录 | 正确的 username + password | 200，返回 token + accountType |
| AU-L02 | 密码错误 | 正确用户名 + 错误密码 | 400 `用户名或密码错误` |
| AU-L03 | 用户名不存在 | 不存在的用户名 | 400 `用户名或密码错误`（防枚举） |
| AU-L04 | 空用户名 | `{username:""}` | 400 参数校验错误 |
| AU-L05 | 已禁用账号 | status=0 的账号 | 400 `账号已被禁用` |

#### 登出 / 检查

| 测试 | 场景 | 输入 | 预期 |
|------|------|------|------|
| AU-O01 | 登出（有 token） | 带有效 satoken | 200 `登出成功` |
| AU-O02 | 检查用户名-可用 | `?username=newuser` | `{available: true}` |
| AU-O03 | 检查用户名-不可用 | `?username=test1` | `{available: false}` |
| AU-O04 | 检查手机号-可用 | `?phone=13900000001` | `{available: true}` |
| AU-O05 | 检查手机号-已注册 | `?phone=13800138000` | `{available: false}` |

### 3.3 商家认证 API（/api/seller/auth）

商家注册/登录/登出/检查的测试场景与用户端一致，但消息文案不同：
- 重复用户名 → `商家用户名已存在`
- 登录成功 → `accountType: "MERCHANT"`
- 返回字段：`merchantInfo`（非 userInfo）

### 3.4 内部接口（/internal/auth）

| 测试 | 场景 | 请求体 | 预期 |
|------|------|--------|------|
| AU-I01 | 正常注册店员 | `{username:"emp1"}` | 200，默认密码 "123456"，返回 merchantId |
| AU-I02 | 注册店员带自定义密码 | `{username:"emp2", password:"CustomP@ss1"}` | 200，使用自定义密码 |
| AU-I03 | 注册店员重复用户名 | 已存在的店员用户名 | 400 `用户名已存在` |

### 3.5 BCrypt / Sa-Token 专项测试

| 测试 | 验证点 |
|------|--------|
| AU-B01 | 密码密文以 `$2a$12$` 开头（salt rounds=12） |
| AU-B02 | `verifyPassword(null, hash)` → false |
| AU-B03 | `verifyPassword(pw, null)` → false |
| AU-B04 | 登录后 token 在 Redis 中存在，key 格式 `satoken:login:token:{uuid}` |
| AU-B05 | 同一账号 is-concurrent=false：新登录使旧 token 失效 |

---

## 4. Product 服务

### 4.1 服务概述

| 项目 | 值 |
|------|-----|
| 端口 | 8081 |
| 数据库 | eureka_product（products / product_images / salable_products / product_reservations） |
| 预留超时 | 30 分钟 |
| 定时任务 | 每 2 分钟清理过期预占 |
| Feign 调用 | shop-service（获取店铺信息） |

### 4.2 用户端 API（/api/user/product）

| 测试 | 场景 | 请求 | 预期 |
|------|------|------|------|
| PU-Q01 | 空列表查询 | `GET /all?page=0`（无可售商品） | 200，空 products 列表 |
| PU-Q02 | 分页查询 | `GET /all?page=0` | 200，每页 20 条 |
| PU-Q03 | 查询存在商品 | `GET /{productId}` | 200，返回完整 DTO（含 imageUrl + shop） |
| PU-Q04 | 查询不存在商品 | `GET /999999` | 404 `商品不存在` |
| PU-Q05 | 搜索匹配 | `GET /search?name=手机` | 200，返回模糊匹配结果 |
| PU-Q06 | 搜索无匹配 | `GET /search?name=不存在的商品` | 200，空列表 |
| PU-Q07 | 价格区间 | `GET /price-range?minPrice=10&maxPrice=100&page=0` | 200，返回范围内商品 |
| PU-Q08 | 价格区间-零范围 | `minPrice=50&maxPrice=50` | 200，返回价格=50 的商品 |
| PU-Q09 | 下架商品不可见 | 先上架商品A再下架，`GET /all` | 商品A不在结果中 |
| PU-Q10 | 默认图片 | 商品 imageId=0 | 返回默认图片 URL |

### 4.3 商家端 API（/api/seller/product）

| 测试 | 场景 | 请求 | 预期 |
|------|------|------|------|
| PS-C01 | 创建商品（含图片） | `POST /create` | 200，返回商品ID，DB 有 products + product_images 记录 |
| PS-C02 | 创建商品-名称空 | `{name:""}` | 400 `商品名称不能为空` |
| PS-C03 | 创建商品-价格≤0 | `{price:-1}` | 400 `@Positive` 校验 |
| PS-C04 | 创建商品-图片空 | `{imageUrl:""}` | 400 `商品图片不能为空` |
| PS-C05 | 更新商品（部分字段） | `PUT /{id}` | 未传字段保持原值 |
| PS-C06 | 更新商品（新图片） | `PUT /{id}` + 新 imageUrl | 图片表更新 URL |
| PS-C07 | 更新不存在商品 | `PUT /{id}`（id 不存在） | 404 `商品不存在` |
| PS-C08 | 删除商品（已下架） | 先下架 → `DELETE /{id}` | 200，DB 删除 |
| PS-C09 | 删除商品（上架中） | 直接 `DELETE /{id}`（未下架） | 400 `请先下架` |
| PS-C10 | 上架 | `POST /{id}/list` | products.is_sale=true + salable_products 插入 |
| PS-C11 | 下架 | `POST /{id}/unlist` | products.is_sale=false + salable_products 删除 |
| PS-C12 | 重复上架 | 两次 `list` | 第二次失败（主键冲突） |
| PS-C13 | 批量查询（商家端） | `GET /batch?ids=1,2,3` | 返回含 isSale 的抽象 DTO |

### 4.4 内部库存 API（/internal/product）

| 测试 | 场景 | 输入 | 预期 |
|------|------|------|------|
| PI-D01 | 扣减库存-充足 | `{"productId":"1","quantity":1}` | `{success:true}`, stock-1 |
| PI-D02 | 扣减库存-不足 | quantity > 当前库存 | `{success:false, message:"库存不足"}` |
| PI-D03 | 恢复库存 | `{"productId":"1","quantity":1}` | `{success:true}`, stock+1 |
| PI-R01 | 预占库存-充足 | `{"orderId":"O1","productId":"1","quantity":1}` | `{success:true}`, RESERVED 记录 |
| PI-R02 | 预占库存-不足 | 预占 > 可用库存 | `{success:false, message:"库存不足"}` |
| PI-R03 | 确认预占 | `POST /confirm-reservation?orderId=O1` | 状态→CONFIRMED，stock 扣减 |
| PI-R04 | 释放预占 | `POST /release-reservation?orderId=O1` | 状态→RELEASED |
| PI-R05 | 重复释放 | 释放已 RELEASED 的预占 | 幂等，不报错 |
| PI-R06 | 预占过期清理 | 等待 30 分钟以上 | 定时任务自动释放 |

### 4.5 并发测试

| 测试 | 场景 | 预期 |
|------|------|------|
| PC-C01 | 并发扣减同一商品 | 最终库存 ≥ 0，无负库存 |
| PC-C02 | 并发预占同一商品（充足） | 预占总量 ≤ 库存 |

---

## 5. Order 服务

### 5.1 服务概述

| 项目 | 值 |
|------|-----|
| 端口 | 8082 |
| 数据库 | eureka_order（t_order / deleted_orders） |
| 订单 ID | Redis 生成，格式 `yyyyMMdd` + 5位序列 + 5位字母 |
| 超时取消 | 每 60s 扫描 PENDING 超时订单（默认 30 分钟） |
| 事件系统 | Redis Stream（order:events）+ 文件兜底 |
| Feign 调用 | product-service / logistics-service / contact-service |

### 5.2 订单状态机

```
PENDING → PAID → SHIPPED → DELIVERED → DELETED
  │        │        │           │
  │        │        └── RETURN_PENDING → RETURNING → RETURNED
  │        │
  └────────┴── CANCELLED → DELETED
```

### 5.3 用户端 API（/api/user/order）

#### 下单

| 测试 | 场景 | 前置 | 预期 |
|------|------|------|------|
| OU-P01 | 正常下单 | 商品存在，库存充足 | 200，返回 orderId，状态 PENDING，预占库存 |
| OU-P02 | 商品不存在 | productId 无效 | 400 `商品不存在（O-003）` |
| OU-P03 | 库存不足 | quantity > 库存 | 400 `库存不足（O-005）` |
| OU-P04 | 商品ID为空 | `productId:null` | 400 `商品信息为空（O-001）` |
| OU-P05 | 联系人ID为空 | `contactId:null` | 400 `收货人信息为空（O-002）` |
| OU-P06 | 数量为0 | `quantity:0` | 400 `购买数量必须大于0（O-004）` |

#### 支付

| 测试 | 场景 | 前置 | 预期 |
|------|------|------|------|
| OU-PAY01 | 正常支付 | 订单 PENDING | CAS PENDING→PAID 成功，发送 STOCK_CONFIRM |
| OU-PAY02 | 重复支付 | 订单已 PAID | CAS 失败，400 `支付失败` |
| OU-PAY03 | 支付已取消订单 | 订单 CANCELLED | 订单不存在或无权限 |
| OU-PAY04 | 并发支付 | 同一订单两请求 | 一个成功，一个 CAS 失败 |

#### 取消

| 测试 | 场景 | 前置 | 预期 |
|------|------|------|------|
| OU-C01 | 取消未支付订单 | 订单 PENDING | CAS PENDING→CANCELLED，释放预占 |
| OU-C02 | 取消已支付订单 | 订单 PAID | CAS PAID→CANCELLED，恢复库存 |
| OU-C03 | 取消已发货订单 | 订单 SHIPPED | 400 `状态已变更` |
| OU-C04 | 并发取消（PENDING） | 两请求同时取消 | 一个成功，一个 CAS 失败 |

#### 收货 / 退货申请 / 删除

| 测试 | 场景 | 前置 | 预期 |
|------|------|------|------|
| OU-D01 | 正常收货 | 订单 SHIPPED | 状态→DELIVERED |
| OU-D02 | 未发货收货 | 订单 PAID | transitionTo 失败，抛异常 |
| OU-R01 | 收货前申请退货 | 订单 SHIPPED | 状态→RETURN_PENDING |
| OU-R02 | 收货后申请退货 | 订单 DELIVERED | 状态→RETURN_PENDING |
| OU-R03 | 已取消申请退货 | 订单 CANCELLED | transitionTo 非法 |
| OU-RE01 | 删除完成订单 | 订单 DELIVERED | 备份到 deleted_orders，删除原记录 |
| OU-RE02 | 删除未支付订单 | 订单 PENDING | 400 `不允许删除` |

### 5.4 商家端 API（/api/seller/order）

| 测试 | 场景 | 前置 | 预期 |
|------|------|------|------|
| OS-S01 | 正常发货 | 订单 PAID，提供 trackingNumber | CAS PAID→SHIPPED，发送 LOGISTICS_CREATE |
| OS-S02 | 未支付发货 | 订单 PENDING | CAS 失败 |
| OS-S03 | 发货-错误的店铺 | shopId 不匹配 | 查询为 null，抛异常 |
| OS-A01 | 审核退货 | 订单 RETURN_PENDING | 状态→RETURNING |
| OS-A02 | 未申请退货时审核 | 订单 DELIVERED | transitionTo 非法 |
| OS-CR01 | 确认退货 | 订单 RETURNING | CAS RETURNING→RETURNED，发送 STOCK_RESTORE |
| OS-CR02 | 未审核确认退货 | 订单 RETURN_PENDING | CAS 失败 |

### 5.5 并发 / 边界测试

| 测试 | 场景 | 预期 |
|------|------|------|
| O-CONC01 | 并发支付 + 取消（同一 PENDING 订单） | 一个成功，一个 CAS 失败 |
| O-CONC02 | 并发取消（PAID）+ 发货 | 一个成功，一个 CAS 失败 |
| O-CONC03 | 并发删除同一订单 | 一个成功，一个查不到 |
| O-TO01 | 超时自动取消 | 等待 30 分钟后，PENDING 订单被定时任务取消 |
| O-EV01 | Redis 不可用 → 文件兜底 | 事件写入 data/failover/，恢复后重试成功 |

### 5.6 查询 API

| 测试 | 查询 | 预期 |
|------|------|------|
| OU-L01 | 用户查询订单列表 | 返回该用户所有订单摘要 |
| OU-L02 | 用户查询订单详情 | 返回完整详情 + 联系人 + 物流信息 |
| OS-L01 | 商家查询店铺订单列表 | 返回该店铺所有订单摘要 |
| OS-L02 | 商家查询订单详情 | 返回完整详情 + 联系人 + 物流信息 |
| OU-L03 | 其他用户查看 | selectOrderDetailByUser 返回 null |

---

## 6. Contact 服务

### 6.1 服务概述

| 项目 | 值 |
|------|-----|
| 端口 | 8083 |
| 数据库 | eureka_contact（t_contact / user_contact / shop_address / shop_address_rel） |
| 权限 | X-User-Id（用户端）/ X-Shop-Id（商家端） |

### 6.2 用户联系人 API（/api/user/contact）

| 测试 | 场景 | 输入 | 预期 |
|------|------|------|------|
| UC-C01 | 创建联系人 | `{name, phone, address}` + X-User-Id | 200，返回 id，t_contact+user_contact 有记录 |
| UC-C02 | 创建-无认证 | 无 X-User-Id | 401 `未登录` |
| UC-C03 | 创建-参数缺失 | 缺 name | 400 参数错误 |
| UC-C04 | 查询列表 | `GET /list` + X-User-Id | 200，返回该用户联系人列表 |
| UC-C05 | 查询-无认证 | 无 X-User-Id | 401 |
| UC-C06 | 更新联系人 | `PUT /update` 正确 id | 200 |
| UC-C07 | 更新-无权访问 | X-User-Id 不匹配 | 400 `地址不存在` |
| UC-C08 | 删除联系人 | `DELETE /delete/{id}` | 200，关联记录也被删除 |
| UC-C09 | 删除-无权访问 | 其他用户 | 400 `地址不存在` |
| UC-C10 | 设置默认 | `PUT /set-default/{id}` | is_default=1 |
| UC-C11 | 设置默认-不存 | id 不存在 | 400 `设置失败` |
| UC-C12 | 设置默认-清除旧默认 | 用户已有默认地址 A，设置 B 为默认 | A.is_default 自动清除 |

### 6.3 商家地址 API（/api/merchant/address）

| 测试 | 场景 | 输入 | 预期 |
|------|------|------|------|
| SA-C01 | 创建地址 | `{name,phone,address,addressType}` + X-Shop-Id | 200，返回 id |
| SA-C02 | 创建地址（设为默认） | + `isDefault:1` | 同类型其他默认被清除 |
| SA-C03 | 创建-无认证 | 无 X-Shop-Id | 401 |
| SA-C04 | 更新地址 | `PUT /update/{id}` | 200 |
| SA-C05 | 更新-不属于该店铺 | 其他店铺 | 400 |
| SA-C06 | 删除地址 | `DELETE /delete/{id}` | 200，关联删除 |
| SA-C07 | 查询列表 | `GET /list` | 返回店铺地址列表 |
| SA-C08 | 查默认发货地址 | `GET /ship-default` | 返回 type=1 且 is_default=1 的地址 |
| SA-C09 | 设置默认地址 | `PUT /set-default/{id}` | 同类型其他默认被清除 |
| SA-C10 | 设置默认-不属于该店铺 | 其他店铺 | 400 |

### 6.4 内部接口（/internal/contact）

| 测试 | 场景 | 输入 | 预期 |
|------|------|------|------|
| SI-G01 | 查询联系人（存在） | `GET /{id}` | 200，返回 Contact 对象 |
| SI-G02 | 查询联系人（不存在） | `GET /{id}` 无效 id | 400 `联系人不存在` |

---

## 7. Logistics 服务

### 7.1 服务概述

| 项目 | 值 |
|------|-----|
| 端口 | 8084 |
| 数据库 | eureka_logistics（logistics） |
| 物流类型 | DELIVERY（发货）/ RETURN（退货） |

### 7.2 物流 API（/logistics）

| 测试 | 场景 | 输入 | 预期 |
|------|------|------|------|
| LG-C01 | 创建物流 | `{orderId, type:"DELIVERY", contactId:1, trackingNumber:"SF123"}` | 200，返回物流记录 |
| LG-C02 | 创建-必填缺失 | 缺 orderId | 400 参数校验 |
| LG-C03 | 查询所有 | `GET /list` | 200，返回列表 |
| LG-C04 | 按快递单号搜索 | `GET /search/tracking?trackingNumber=SF123` | 200，匹配结果 |
| LG-C05 | 快递单号不存在 | `?trackingNumber=NOTEXIST` | 200，data 可能为 null |
| LG-C06 | 删除物流 | `DELETE /delete/{id}` | 200 |
| LG-C07 | 删除不存在 | `DELETE /delete/{id}` 无效 id | 400 抛异常 |
| LG-C08 | 按订单查询 | `GET /order/{orderId}` | 200，返回该订单所有物流 |
| LG-C09 | 查询最新物流 | `GET /order/{orderId}/latest?type=DELIVERY` | 200，返回最新一条 |

### 7.3 内部物流 API（/internal/logistics）

| 测试 | 场景 | 输入 | 预期 |
|------|------|------|------|
| LI-C01 | 内部创建物流 | `LogisticsRequest` | 200，返回 LogisticsResponse |
| LI-C02 | 内部按订单查询 | `GET /order/{orderId}` | 200 |
| LI-C03 | 内部查最新物流 | `GET /order/{orderId}/latest?type=DELIVERY` | 200 |

---

## 8. Shop 服务

### 8.1 服务概述

| 项目 | 值 |
|------|-----|
| 端口 | 8087 |
| 数据库 | eureka_shop（shops / shop_info / merchant_roles） |
| 角色 | 1=店长 / 2=店员 |

### 8.2 用户端 API（/api/user/shop）

| 测试 | 场景 | 请求 | 预期 |
|------|------|------|------|
| SU-L01 | 店铺列表 | `GET /list?page=1&size=10` + X-User-Id | 200，返回分页数据 |
| SU-L02 | 未登录查询 | 无 X-User-Id | 400 `请先登录` |
| SU-D01 | 店铺详情（活跃） | `GET /{shopId}` + X-User-Id | 200，返回 shop + shopInfo |
| SU-D02 | 已关闭店铺 | status=0 | 可能不返回或标记为关闭 |

### 8.3 商家端 API（/api/seller/shop）

#### 店铺管理

| 测试 | 场景 | 请求 | 预期 |
|------|------|------|------|
| SM-C01 | 创建店铺 | `POST /register` + X-User-Id | 200，返回店铺 ID |
| SM-C02 | 创建-名称空 | `{name:""}` | 400 参数校验 |
| SM-C03 | 查询商家店铺 | `GET /merchant/{merchantId}` | 200，返回 shopIds 列表 |
| SM-C04 | 查询店铺详情 | `GET /{shopId}` + X-User-Id | 200，含权限校验 |
| SM-C05 | 更新店铺 | `PUT /{shopId}` | 200 |
| SM-C06 | 关闭店铺 | `DELETE /{shopId}` | 200，status=0 |
| SM-C07 | 重新开店 | `PUT /{shopId}/open` | 200，status=1 |
| SM-C08 | 操作他人店铺 | X-User-Id 不是该店铺店长 | 抛异常 |

#### 员工管理

| 测试 | 场景 | 请求 | 预期 |
|------|------|------|------|
| SM-E01 | 添加店员 | `POST /{shopId}/employees/register` | 200，调用 auth-service 创建账号并关联角色 |
| SM-E02 | 查询员工列表 | `GET /{shopId}/employees` | 200，返回 employees + total |
| SM-E03 | 移除店员 | `DELETE /{shopId}/employees/{merchantId}` | 200，解除关联 |
| SM-E04 | 店员操作员工管理 | 店员 token + X-Merchant-Role: 2 | Gateway 返回 403 |
| SM-E05 | 添加已存在的店员 | 重复 employee | 抛异常 |
| SM-E06 | 移除不存在的店员 | merchantId 无效 | 抛异常 |

### 8.4 内部接口（/internal/shop）

| 测试 | 场景 | 输入 | 预期 |
|------|------|------|------|
| SI-R01 | 查询商家角色 | `GET /employees/roles/{merchantId}` | 200，返回 roles 列表 |
| SI-R02 | 不存在商家查询 | merchantId 无效 | 200，空列表 |
| SI-I01 | 查询店铺信息 | `GET /info/{shopId}` | 200，返回 ShopInfoDTO |
| SI-I02 | 批量查询店铺信息 | `POST /info/batch` body: [1,2,3] | 200，返回 Map<Long, ShopInfoDTO> |

---

## 附录 A：已知问题总结

| 优先级 | 服务 | 问题描述 | 影响 |
|--------|------|---------|------|
| ~~P0~~ | Contact | ~~`UserContactController.toContact(UpdateContactRequest)` 无限递归~~ | **已修复** commit `af1a8c0` |
| ~~P1~~ | Contact | ~~`setDefaultContact` 未清除其他默认标记~~ | **已修复** commit `af1a8c0` |
| ~~P2~~ | Gateway | ~~`user-chat` 路由 `RewritePath` 缺少捕获组~~ | **已修复** commit `af1a8c0` |
| ~~P2~~ | Product | ~~`shopInfoCache` 使用 `ConcurrentHashMap` 无过期策略~~ | **已修复** commit `af1a8c0` |
| P3 | Product | `ProductCache` 代码已存在但被注释，暂不生效 | 无影响 |

### 已排除的误报 / 非问题

| ~~P1~~ | Order | ~~并发取消时双倍恢复库存~~ | **误报**。CAS 乐观锁机制已正确防止：先尝试 PAID→CANCELLED 再尝试 PENDING→CANCELLED，两轮 CAS 不可能对同一状态同时成功 |
| ~~P1~~ | Order | ~~confirmReturn 未恢复库存~~ | **设计如此**。库存恢复通过 `afterCommit` → `STOCK_RESTORE` 事件异步处理（最终一致），非同步缺失 |

## 附录 B：变更记录

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v1.0 | 2026-05-28 | 初始版本 |
| v1.1 | 2026-05-28 | 附录 A 更新：P0/P1/P2 四个问题已修复（commit `af1a8c0`）；Section 6 移除 P0/P1 警告；Gateway chat 路由增加子路径验证测试 |

## 附录 C：环境准备清单

- [ ] MySQL 创建 6 个数据库并执行初始化脚本
- [ ] Redis 启动（默认端口 6379）
- [ ] Eureka Server 启动（端口 8761）
- [ ] 按顺序启动所有微服务
- [ ] 准备测试账号：用户 x1 个、商家 x1 个
- [ ] 准备测试商品数据
