# gateway-service 测试用例文档

> 版本: 1.0 | 日期: 2026-05-28 | 项目: AI-Shopping 智能购物平台

---

## 1. 概述

本文档为 **gateway-service** 模块的测试用例说明，覆盖 API 网关的核心职责：IP 限流、Sa-Token 认证鉴权、白名单放行、全局异常处理、路由分发和 CORS 配置。

**服务坐标**: `com.gzasc:aishopping:gateway-service` | **端口**: 8080 | **包路径**: `com.gzasc.aishopping.gateway`

### 1.1 组件清单

| 组件 | 类型 | Order | 说明 |
|------|------|-------|------|
| `IpRateLimitFilter` | GlobalFilter | -200 | 基于 Caffeine 本地缓存的 IP 限流 |
| `SaTokenAuthGlobalFilter` | GlobalFilter | -100 | Sa-Token 令牌校验 + 角色鉴权 |
| `AuthServiceImpl` | Service | — | 白名单匹配、Token 校验、权限判断 |
| `GlobalErrorWebExceptionHandler` | ErrorWebExceptionHandler | -1 | 全局异常统一响应 |
| `AuthWhitelistProperties` | @ConfigurationProperties | — | 9 条白名单路径 |
| `IpRateLimitProperties` | @ConfigurationProperties | — | 限流阈值 300/60s |

### 1.2 路由概览

| 分类 | 数量 | 目标服务 | 特点 |
|------|------|---------|------|
| 用户端 | 7 | auth/product/order/contact/logistics/chat/shop | 前缀 `/api/user/` |
| 商家端 | 8 | auth/product/order/contact/address/logistics/chat/shop | 前缀 `/api/seller/` |
| 内部 | 4 | shop/auth/product/order | 前缀 `/internal/`, StripPrefix=1 |
| CORS | 全局 | — | `/**` 全来源允许 + credentials |

---

## 2. 测试环境

### 2.1 环境配置

| 项目 | 值 |
|------|-----|
| JDK | 17 |
| Spring Cloud Gateway | 4.x (Spring Boot 3.2.3) |
| Caffeine | 本地缓存 (IpRateLimitFilter) |
| Redis | 需 Mock 或嵌入式 (Sa-Token Token 校验) |
| Eureka | 测试时禁用 (`eureka.client.enabled=false`) |

### 2.2 测试依赖

| 依赖 | 用途 |
|------|------|
| `spring-boot-starter-test` | JUnit 5 + Mockito |
| `reactor-test` | WebFlux `StepVerifier` |
| `@WebFluxTest` / `@SpringBootTest` | 切片 / 全量测试 |

### 2.3 测试数据约定

| 项目 | 值 |
|------|-----|
| 有效用户 Token loginId | `USER:u001` |
| 有效商家 Token loginId | `MERCHANT:m001` |
| 过期 Token | `expired-token` (Redis 无此 key) |
| 测试限流阈值 | maxRequests=5, timeWindowSeconds=60 (application-test.yml) |
| 默认限流阈值 | maxRequests=300, timeWindowSeconds=60 (application.yml) |

---

## 3. 测试用例表

### 3.1 IP 限流过滤器 — IpRateLimitFilter

**Order = -200，在所有过滤器之前执行。基于 Caffeine 缓存，过期时间 = timeWindowSeconds。**

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| GW-IP-001 | 正常请求未达阈值 | maxRequests=5, 请求次数 < 5 | 1. 构造 GET /api/user/auth/login 请求<br>2. 发送 4 次请求，每次间隔 100ms<br>3. 检查每次响应 | 返回 200 (或下游响应)，不被限流拦截 | P0 |
| GW-IP-002 | 请求超过阈值触发限流 | maxRequests=5, 已累计 5 次请求 | 1. 发送第 6 次请求到同一 IP<br>2. 捕获响应 | 返回 429 TOO_MANY_REQUESTS，body 为 `{"code":429,"message":"请求过于频繁，请稍后再试","data":null}` | P0 |
| GW-IP-003 | 不同 IP 独立计数 | maxRequests=5 | 1. 用 IP-A 发 5 次请求<br>2. 用 IP-B 发 1 次请求<br>3. 分别检查响应 | IP-A 第 6 次被限流，IP-B 可以正常请求 | P1 |
| GW-IP-004 | 窗口过期后计数重置 | maxRequests=5, timeWindowSeconds=1 | 1. 发 6 次请求触发限流 (429)<br>2. 等待 1.5 秒（超过窗口）<br>3. 再次发送 1 次请求 | 限流解除，请求正常通过 | P1 |
| GW-IP-005 | X-Forwarded-For 优先获取 IP | — | 1. 构造请求，Header 设 `X-Forwarded-For: 10.0.0.1`<br>2. 同时设 `X-Real-IP: 10.0.0.2`<br>3. 发送请求 | 限流计数器以 10.0.0.1 为准 | P1 |
| GW-IP-006 | X-Real-IP 作为第二 IP 来源 | — | 1. 不带 X-Forwarded-For<br>2. Header 设 `X-Real-IP: 10.0.0.3`<br>3. 发送请求 | 限流计数器以 10.0.0.3 为准 | P2 |
| GW-IP-007 | 无代理 Header 时使用 RemoteAddress | — | 1. 构造请求，不带 X-Forwarded-For 和 X-Real-IP<br>2. 设置 RemoteAddress 为 192.168.1.1 | 限流计数器以 192.168.1.1 为准 | P2 |
| GW-IP-008 | X-Forwarded-For 多 IP 取第一个 | — | 1. Header 设 `X-Forwarded-For: 10.0.0.1, 10.0.0.2, 10.0.0.3`<br>2. 发送请求 | 限流计数器以 10.0.0.1 为准（取逗号分割第一个） | P2 |
| GW-IP-009 | 自定义 maxRequests 生效 | maxRequests=1 | 1. 配置 `ip-rate-limit.max-requests=1`<br>2. 发送第 2 次请求 | 返回 429 限流响应 | P1 |
| GW-IP-010 | JSON 序列化异常回退 | ObjectMapper 抛异常 | 1. Mock ObjectMapper.writeValueAsString 抛出 JsonProcessingException<br>2. 限流触发 | 返回 fallback `{"code":500,"message":"系统错误"}` | P2 |

### 3.2 Sa-Token 认证过滤器 — SaTokenAuthGlobalFilter

**Order = -100，在 IpRateLimitFilter 之后执行。负责 Token 校验和角色鉴权。**

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| GW-SA-001 | 有效 Token 通过认证 | Redis 中存在 key `satoken:login:token:valid-token` → value `USER:u001` | 1. 请求 Header 携带 `satoken: valid-token`<br>2. 访问 `/api/user/product/all` | 认证通过，请求转发到下游；下游 Header 中注入 `userId=USER:u001` 和 `satoken=valid-token` | P0 |
| GW-SA-002 | 无 Token 返回 401 | — | 1. 请求不携带 Header `satoken`<br>2. 访问 `/api/user/product/all` | 抛出 GatewayAuthException(401, "未登录")，全局异常处理器返回 `{"code":401,"message":"未登录","data":null}` | P0 |
| GW-SA-003 | Token 为空字符串返回 401 | — | 1. Header 携带 `satoken: ` (空字符串)<br>2. 访问任意非白名单路径 | 抛出 GatewayAuthException(401, "未登录") | P1 |
| GW-SA-004 | Token 无效或过期返回 401 | Redis 中无 `satoken:login:token:invalid-token` | 1. Header 携带 `satoken: invalid-token`<br>2. 访问任意非白名单路径 | 抛出 GatewayAuthException(401, "登录已过期，请重新登录") | P0 |
| GW-SA-005 | Token 对应 value 为空字符串 | Redis key 存在但值为 `""` | 1. Header 携带有效 token，但 Redis value 为空字符串<br>2. 访问任意非白名单路径 | 抛出 GatewayAuthException(401, "登录已过期，请重新登录") | P2 |

### 3.3 白名单放行

**白名单共 9 条路径，放行时不进行 Token 校验和角色鉴权。**

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| GW-WL-001 | 用户登录白名单放行 | — | 1. POST `/api/user/auth/login`<br>2. 不带 satoken Header | 请求通过过滤器，转发到 auth-service | P0 |
| GW-WL-002 | 用户注册白名单放行 | — | 1. POST `/api/user/auth/register`<br>2. 不带 satoken Header | 请求通过过滤器，转发到 auth-service | P0 |
| GW-WL-003 | 用户检查用户名白名单 | — | 1. GET `/api/user/auth/check-username?username=test`<br>2. 不带 satoken Header | 请求通过过滤器，转发到 auth-service | P1 |
| GW-WL-004 | 用户检查手机号白名单 | — | 1. GET `/api/user/auth/check-phone?phone=13800138000`<br>2. 不带 satoken Header | 请求通过过滤器，转发到 auth-service | P1 |
| GW-WL-005 | 商家登录白名单放行 | — | 1. POST `/api/seller/auth/login`<br>2. 不带 satoken Header | 请求通过过滤器，转发到 auth-service | P0 |
| GW-WL-006 | 商家注册白名单放行 | — | 1. POST `/api/seller/auth/register`<br>2. 不带 satoken Header | 请求通过过滤器，转发到 auth-service | P0 |
| GW-WL-007 | 商家店铺注册白名单放行 | — | 1. POST `/api/seller/shop/register`<br>2. 不带 satoken Header | 请求通过过滤器，转发到 shop-service | P0 |
| GW-WL-008 | 商家检查用户名白名单 | — | 1. GET `/api/seller/auth/check-username?username=test`<br>2. 不带 satoken Header | 请求通过过滤器，转发到 auth-service | P1 |
| GW-WL-009 | 商家检查手机号白名单 | — | 1. GET `/api/seller/auth/check-phone?phone=13800138000`<br>2. 不带 satoken Header | 请求通过过滤器，转发到 auth-service | P1 |
| GW-WL-010 | 白名单路径带额外路径段 | — | 1. POST `/api/user/auth/login/extra`<br>2. 不带 satoken Header | AntPathMatcher 不匹配，应返回 401 | P2 |
| GW-WL-011 | 白名单路径大小写敏感 | — | 1. POST `/api/user/auth/Login` (大写 L)<br>2. 不带 satoken Header | AntPathMatcher 默认区分大小写，不匹配，应返回 401 | P2 |

### 3.4 角色鉴权 (USER / MERCHANT / 店长)

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| GW-RL-001 | USER 角色访问用户端 API 成功 | Redis loginId=`USER:u001` | 1. Header 携带有效 USER token<br>2. GET `/api/user/product/all` | hasPermission 返回 true，请求通过 | P0 |
| GW-RL-002 | USER 角色访问商家端 API 被拒 | Redis loginId=`USER:u001` | 1. Header 携带有效 USER token<br>2. GET `/api/seller/product/all` | 抛出 GatewayAuthException(403, "无权限访问该资源") | P0 |
| GW-RL-003 | MERCHANT 角色访问商家端 API 成功 | Redis loginId=`MERCHANT:m001` | 1. Header 携带有效 MERCHANT token<br>2. GET `/api/seller/product/all` | hasPermission 返回 true，请求通过 | P0 |
| GW-RL-004 | MERCHANT 角色访问用户端 API 被拒 | Redis loginId=`MERCHANT:m001` | 1. Header 携带有效 MERCHANT token<br>2. GET `/api/user/order/list` | 抛出 GatewayAuthException(403, "无权限访问该资源") | P0 |
| GW-RL-005 | 店长访问店铺管理 API 成功 | loginId=`MERCHANT:m001`, Header `X-Merchant-Role=1`, `X-Shop-Id=100` | 1. Header 携带上述信息<br>2. PUT `/api/seller/shop/manage/update` | checkShopOwnerPermission 通过，请求放行 | P1 |
| GW-RL-006 | 非店长角色访问管理 API 被拒 | loginId=`MERCHANT:m001`, Header `X-Merchant-Role=2` (店员) | 1. Header 携带 `X-Merchant-Role: 2`<br>2. PUT `/api/seller/shop/manage/update` | 返回 403 "无权限访问该资源" | P1 |
| GW-RL-007 | 无 X-Merchant-Role Header 访问管理 API | loginId=`MERCHANT:m001`，不携带 X-Merchant-Role | 1. PUT `/api/seller/shop/manage/update` | checkShopOwnerPermission 返回 false，抛出 403 | P1 |
| GW-RL-008 | 无 X-Shop-Id Header 访问管理 API | loginId=`MERCHANT:m001`, X-Merchant-Role=1 | 1. PUT `/api/seller/shop/manage/update` | checkShopOwnerPermission 返回 false，抛出 403 | P1 |
| GW-RL-009 | 店员访问店铺查询 API 成功 | loginId=`MERCHANT:m001`, X-Merchant-Role=2 | 1. GET `/api/seller/shop/query/list` | isShopOwnerOnlyApi 返回 false，放行 | P1 |
| GW-RL-010 | 店员访问店铺发货 API 成功 | loginId=`MERCHANT:m001`, X-Merchant-Role=2, X-Shop-Id=100 | 1. POST `/api/seller/shop/manage/100/ship` | isShopOwnerOnlyApi 返回 false (`/manage/**/ship` 除外)，放行 | P2 |
| GW-RL-011 | 店长访问商品编辑 API | loginId=`MERCHANT:m001`, X-Merchant-Role=1, X-Shop-Id=100 | 1. PUT `/api/seller/shop/100/products/200` | isShopOwnerOnlyApi 匹配 `/*/products/*`，需店长权限 | P1 |
| GW-RL-012 | 店员访问员工注册 API 被拒 | loginId=`MERCHANT:m001`, X-Merchant-Role=2 | 1. POST `/api/seller/shop/100/employees/register` | isShopOwnerOnlyApi 匹配，需店长权限，返回 403 | P2 |
| GW-RL-013 | 店员访问员工管理 API 被拒 | loginId=`MERCHANT:m001`, X-Merchant-Role=2 | 1. DELETE `/api/seller/shop/100/employees/50` | isShopOwnerOnlyApi 匹配 `/*/employees/*`，返回 403 | P2 |
| GW-RL-014 | 非标准前缀路径直接放行 | loginId=任意，path=`/actuator/health` | 1. 访问 `/actuator/health`<br>2. 不带 satoken Token | path 不以 `/api/user/` 或 `/api/seller/` 开头，hasPermission 返回 true | P2 |
| GW-RL-015 | 角色前缀不匹配但路径允许 | loginId=`ANONYMOUS:xxx` | 1. 访问 `/actuator/info` | hasPermission 返回 true（非 user/seller 路径） | P2 |

### 3.5 全局异常处理 — GlobalErrorWebExceptionHandler

**Order = -1, 实现 ErrorWebExceptionHandler，捕获所有过滤器抛出的异常。**

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| GW-EX-001 | GatewayAuthException 401 | 无 Token 访问非白名单路径 | 1. GET `/api/user/order/list`<br>2. 不带 satoken Header | 返回 401，body `{"code":401,"message":"未登录","data":null}` | P0 |
| GW-EX-002 | GatewayAuthException 403 | USER 角色访问商家路径 | 1. GET `/api/seller/product/list`<br>2. Header 携带 USER token | 返回 403，body `{"code":403,"message":"无权限访问该资源","data":null}` | P0 |
| GW-EX-003 | ResponseStatusException 404 | 路由不存在的路径 | 1. GET `/api/user/nonexistent`<br>2. 携带有效 token | 返回 404，body 可能为 `{"code":404,"message":null,"data":null}` 或 `{"code":404,"message":"404 NOT_FOUND","data":null}`（ResponseStatusException 的 reason 可能为 null） | P1 |
| GW-EX-004 | 未知异常返回 500 | 过滤器内抛出非认证异常 | 1. 模拟 IpRateLimitFilter 内 NPE<br>2. 发送请求 | 返回 500，body `{"code":500,"message":"系统错误，请稍后重试","data":null}` | P1 |
| GW-EX-005 | GatewayAuthException 自定义 code | 构造 code=429 | 1. 直接 new GatewayAuthException(429, "自定义消息")<br>2. 模拟抛出 | 返回 429，body 使用该 code 和 message | P2 |
| GW-EX-006 | JSON 序列化异常回退 | ObjectMapper 抛 JsonProcessingException | 1. Mock ObjectMapper 抛出异常<br>2. 触发异常处理器 | 返回 fallback `{"code":500,"message":"系统错误"}` | P2 |

### 3.6 路由规则

**基于 application.yml 配置，验证请求正确分发到目标服务。**

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| GW-RT-001 | 用户认证路由 | Eureka 注册 auth-service | 1. POST `/api/user/auth/login`<br>2. 请求体为登录 JSON | 路由到 auth-service，不进行 RewritePath | P0 |
| GW-RT-002 | 用户商品路由 RewritePath | Eureka 注册 product-service | 1. GET `/api/user/product/all`<br>2. 携带有效 token | 路由到 product-service，路径重写为 `/api/user/product/all` | P0 |
| GW-RT-003 | 用户聊天路由 RewritePath 特殊规则 | Eureka 注册 chat-service | 1. POST `/api/user/chat/send`<br>2. 携带有效 token | 路由到 chat-service，路径重写为 `/chat/send` (注意 chat 服务路径特殊) | P1 |
| GW-RT-004 | 商家认证路由 | Eureka 注册 auth-service | 1. POST `/api/seller/auth/login`<br>2. 请求体为登录 JSON | 路由到 auth-service | P0 |
| GW-RT-005 | 商家店铺路由 (无 RewritePath) | Eureka 注册 shop-service | 1. POST `/api/seller/shop/register`<br>2. 携带有效 token | 路由到 shop-service，路径完整传递 | P0 |
| GW-RT-006 | 内部路由 StripPrefix=1 | Eureka 注册 shop-service | 1. GET `/internal/shop/api/shops`<br>2. 内部调用 | StripPrefix 去掉 `/internal`，转发到 `/shop/api/shops` | P1 |
| GW-RT-007 | 内部认证路由 StripPrefix | Eureka 注册 auth-service | 1. POST `/internal/auth/api/user/register`<br>2. 内部调用 | StripPrefix 去掉 `/internal`，转发到 `/auth/api/user/register` | P1 |
| GW-RT-008 | 不存在的路由返回 404 | — | 1. GET `/api/invalid/path`<br>2. 携带有效 token | 无匹配路由，返回 404 | P1 |
| GW-RT-009 | 用户订单路由 RewritePath | Eureka 注册 order-service | 1. POST `/api/user/order/place`<br>2. 携带有效 token | 路由到 order-service，路径重写为 `/api/user/order/place` | P1 |
| GW-RT-010 | 用户店铺路由 (无 RewritePath) | Eureka 注册 shop-service | 1. GET `/api/user/shop/list`<br>2. 携带有效 token | 路由到 shop-service，路径完整传递 | P1 |
| GW-RT-011 | 商家地址路由 | Eureka 注册 contact-service | 1. GET `/api/seller/address/list`<br>2. 携带有效 token | 路由到 contact-service，路径重写为 `/api/seller/address/list` | P1 |
| GW-RT-012 | 商家物流路由 | Eureka 注册 logistics-service | 1. POST `/api/seller/logistics/create`<br>2. 携带有效 token | 路由到 logistics-service | P1 |

### 3.7 CORS 配置

**配置: `allowed-origin-patterns=*`, `allowed-methods=*`, `allowed-headers=*`, `allow-credentials=true`**

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| GW-CORS-001 | OPTIONS 预检请求直接放行 | 任意来源 | 1. OPTIONS `/api/user/auth/login`<br>2. Origin: `http://localhost:3000`<br>3. Access-Control-Request-Method: POST | 返回 200，响应含 `Access-Control-Allow-Origin`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Credentials: true` | P0 |
| GW-CORS-002 | 任意来源允许跨域 | — | 1. GET `/api/user/product/all`<br>2. Origin: `https://任意域名.com` | 响应含 `Access-Control-Allow-Origin: *` | P1 |
| GW-CORS-003 | 带凭证的跨域请求 | — | 1. GET `/api/user/order/list`<br>2. Origin: `http://localhost:3000`<br>3. withCredentials: true | `Access-Control-Allow-Credentials: true` | P1 |
| GW-CORS-004 | 非 OPTIONS 同源请求不影响 | — | 1. GET `/api/user/product/all`<br>2. 不带 Origin Header | 正常业务响应，无 CORS Header 也可 | P2 |
| GW-CORS-005 | max-age 配置正确 | — | 1. OPTIONS 预检请求 | 响应含 `Access-Control-Max-Age: 3600` | P2 |

### 3.8 过滤器执行顺序

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| GW-ORD-001 | IpRateLimitFilter 先于 SaTokenAuthGlobalFilter 执行 | IP 限流阈值=0 | 1. 配置 maxRequests=0<br>2. 访问任意路径（即使携带有效 token） | 返回 429，限流器先拦截，不会进入认证过滤器 | P1 |
| GW-ORD-002 | 限流通过后进入认证过滤器 | IP 正常，无 Token | 1. 发送第 1 次请求<br>2. 不带 satoken Header | IP 限流通过，认证过滤器返回 401 | P1 |
| GW-ORD-003 | 白名单不触发认证但仍触发限流 | IP 超限，白名单路径 | 1. 发送 6 次 POST 到 `/api/user/auth/login`<br>2. 不带 satoken Header | 第 6 次返回 429（限流在前，白名单在后） | P1 |

---

## 4. 测试要点总结

### 4.1 优先级分布

| 优先级 | 数量 | 说明 |
|--------|------|------|
| P0 | 15 | 核心认证/鉴权/限流/白名单/CORS 预检 |
| P1 | 19 | 重要边界/路由/角色细分场景 |
| P2 | 10 | 边缘场景/序列化回退/大小写/非标路径 |

### 4.2 关键风险点

1. **过滤器执行顺序**: IpRateLimitFilter(-200) 先于 SaTokenAuthGlobalFilter(-100)，若 IP 限流到达阈值，认证逻辑不执行
2. **X-Forwarded-For SSRF 风险**: 客户端可伪造 X-Forwarded-For Header 绕过 IP 限流，仅信任上游代理的 IP 才安全
3. **Redis 依赖**: Sa-Token 认证强依赖 Redis 可用性，Redis 故障将导致所有非白名单请求返回 401
4. **AntPathMatcher 行为**: 默认区分大小写，`/api/user/auth/Login` 不匹配白名单
5. **CORS 与凭证**: `allow-credentials=true` 与 `allowed-origin-patterns=*` 同时配置在浏览器中可能导致 CORS 错误（需确认浏览器兼容性）
6. **内部路由安全**: `/internal/` 路由仅有 StripPrefix 而无额外认证，需确保仅内部网络可达
7. **店长权限粒度**: `isShopOwnerOnlyApi` 的路径模式匹配可能遗漏或过度匹配，需关注路径规范

### 4.3 测试建议

- **单元测试**: 对 `AuthServiceImpl` 的 `hasPermission`、`extractRole`、`isShopOwnerOnlyApi`、`isWhiteList` 进行纯逻辑测试，Mock Redis
- **切片测试**: 对 `IpRateLimitFilter` 使用 `@WebFluxTest` + `@MockBean` 验证限流边界
- **集成测试**: 使用 `@SpringBootTest` 加载完整上下文，Mock Redis + 禁用 Eureka，验证过滤器链交互
- **API 测试**: 使用 WebTestClient 验证完整请求/响应链路，包括 Header 注入
- **路由测试**: 验证所有 19 条路由的 Predicate 和 Filter 配置正确，可使用 `spring.cloud.gateway.routes` 配置模拟目标 URI

### 4.4 用例汇总

| 模块 | 用例数 |
|------|--------|
| 3.1 IP 限流 | 10 |
| 3.2 Sa-Token 认证 | 5 |
| 3.3 白名单 | 11 |
| 3.4 角色鉴权 | 15 |
| 3.5 全局异常 | 6 |
| 3.6 路由规则 | 12 |
| 3.7 CORS | 5 |
| 3.8 过滤器顺序 | 3 |
| **合计** | **67** |
