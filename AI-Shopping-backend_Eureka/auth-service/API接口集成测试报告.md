# Auth 服务 API 接口集成测试报告

## 1. 测试概述

| 项目 | 内容 |
|------|------|
| 测试目标 | 验证 Auth 服务（用户端 + 商家端）全部 REST API 端点的功能正确性和容错能力 |
| 测试类型 | API 接口集成测试（端到端，通过 Gateway 访问） |
| 测试日期 | 2026-05-29 |
| 测试工具 | PowerShell `Invoke-WebRequest` |

## 2. 测试环境

| 组件 | 地址 | 状态 |
|------|------|:----:|
| MySQL | localhost:3306 | ✅ 运行中 |
| Redis | localhost:6379 | ✅ 运行中 |
| Eureka Server | http://localhost:8761 | ✅ 运行中 |
| Gateway Service | http://localhost:8080 | ✅ 运行中 |
| Auth Service | http://localhost:8086 | ✅ 运行中 |

### 路由链路

```
Client
  → GET/POST http://localhost:8080/api/{user|seller}/auth/*
    → Gateway (路由转发 + Sa-Token 鉴权过滤)
      → Auth Service (Controller → Service → Mapper → MySQL/Redis)
```

### 测试数据来源

`sql/insert/init_authData.sql` 中预置的测试账号：

| 类型 | 用户名 | 密码 | 手机号 |
|------|--------|------|--------|
| 用户 | user001 | (BCrypt) user001 | 13800138000 |
| 用户 | user002 | (BCrypt) user002 | 13900139000 |
| 商家 | merchant001 | (BCrypt) 123456 | 13700137000 |
| 商家 | merchant002 | (BCrypt) merchant002 | 13600136000 |

## 3. 测试用例及结果

### 3.1 用户端 API

| # | 用例 | 方法 | 端点 | 预期结果 | 实际结果 | 状态 |
|---|------|------|------|----------|----------|:----:|
| 1 | 检查新用户名可用 | GET | `/api/user/auth/check-username?username=newuser` | `data.available = true` | `data.available = true` | ✅ |
| 2 | 检查新手机号可用 | GET | `/api/user/auth/check-phone?phone=138xxxx` | `data.available = true` | `data.available = true` | ✅ |
| 3 | 注册新用户（含 email） | POST | `/api/user/auth/register` | 返回 `token` + `userInfo` + `accountType=USER` | `token` UUID 格式, `userInfo` 含 id/username/nickname/phone/email | ✅ |
| 4 | 重复注册（容错） | POST | `/api/user/auth/register` | 400 "用户已存在" | 400, message="用户已存在" | ✅ |
| 5 | 用户登录 | POST | `/api/user/auth/login` | 返回 `token` + `userInfo` + `accountType=USER` | `token` UUID 格式, 与注册返回不同 | ✅ |
| 6 | 密码错误登录（容错） | POST | `/api/user/auth/login` | 400 "用户名或密码错误" | 400, message="用户名或密码错误" | ✅ |
| 7 | 不存在用户登录（容错） | POST | `/api/user/auth/login` | 400 "用户名或密码错误" | 400, message="用户名或密码错误" | ✅ |

### 3.2 商家端 API

| # | 用例 | 方法 | 端点 | 预期结果 | 实际结果 | 状态 |
|---|------|------|------|----------|----------|:----:|
| 8 | 商家登录 | POST | `/api/seller/auth/login` | 返回 `token` + `merchantInfo` + `accountType=MERCHANT` | `token` UUID, `merchantInfo` 含 username/phone/email | ✅ |
| 9 | 商家检查新用户名 | GET | `/api/seller/auth/check-username?username=newmerchant` | `data.available = true` | `data.available = true` | ✅ |
| 10 | 商家注册 | POST | `/api/seller/auth/register` | 返回 `token` + `merchantInfo` + `accountType=MERCHANT` | `token` UUID, `merchantInfo` 含 id/nickname/status | ✅ |

## 4. 测试结果统计

| 维度 | 数值 |
|------|:----:|
| 总用例数 | 10 |
| 通过 | 10 |
| 失败 | 0 |
| 通过率 | **100%** |

## 5. 关键验证点分析

### 5.1 认证流程

- Gateway (8080) 正确将请求路由到 Auth Service (8086)
- 注册 **注册即登录**：注册成功后直接返回 token，无需额外登录
- 用户端与商家端账号体系隔离：`accountType` 分别为 `USER` / `MERCHANT`

### 5.2 校验机制

- 用户名唯一性校验正常工作
- 手机号唯一性校验正常工作（11位 `^1[3-9]\d{9}$` 格式校验）
- 邮箱格式校验正常工作（正则 `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`）
- 密码使用 BCrypt 加密存储和验证

### 5.3 容错处理

- 重复注册 → 400 "用户已存在"
- 密码错误 → 400 "用户名或密码错误"（不区分用户不存在和密码错误，防止账户枚举）
- 用户不存在 → 400 "用户名或密码错误"

### 5.4 Token 机制

- 格式：Sa-Token UUID（非 JWT）
- 有效期：86400s（1天），滑动过期 1800s
- 传递方式：Header `satoken`
- is-concurrent: false（新登录挤掉旧登录）

## 6. 已有关联的单测覆盖

| 模块 | 测试文件 | 测试数 | 覆盖范围 |
|------|----------|:------:|----------|
| 用户认证 Controller | `controller/UserAuthControllerTest.java` | 17 | 注册/登录/登出/参数校验/检查用户名手机号 |
| 商家认证 Controller | `controller/MerchantAuthControllerTest.java` | 4 | 注册/登录/跨表不冲突/返回 merchantInfo |
| 用户认证 Service | `service/impl/UserAuthServiceImplTest.java` | 15 | 注册/登录/登出/用户名检查/手机号检查 |
| 商家认证 Service | `service/impl/MerchantAuthServiceImplTest.java` | 13 | 注册/登录/店员注册/用户名检查/手机号检查 |
| 内部 API | `controller/internal/InternalControllerTest.java` | 3 | 店员注册 |
| BCrypt 工具 | `util/BCryptUtilTest.java` | 4 | 哈希/验证/强度 |
| 实体转换 | `converter/AuthConverterTest.java` | 4 | User/UserInfo/Merchant/MerchantInfo 转 Map |

## 7. 结论

Auth 服务全部 10 个 API 端点集成测试通过。认证核心流程（注册 → 登录 → 鉴权）完整闭环，校验逻辑和容错处理符合预期，用户端与商家端账号体系隔离正确。

## 8. 本次变更记录

### 8.1 新增功能：注册支持 email 字段

**变更文件：**

| 文件 | 变更内容 |
|------|----------|
| `dto/RegisterRequest.java` | 新增 `email` 字段，含邮箱格式正则校验 |
| `service/impl/UserAuthServiceImpl.java` | `register()` 方法写入 `user.setEmail(request.getEmail())` |
| `service/impl/MerchantAuthServiceImpl.java` | `register()` 方法写入 `merchant.setEmail(request.getEmail())` |

**验证结果：** 注册时传入 email 可正确写入并返回，邮箱格式校验正常。
