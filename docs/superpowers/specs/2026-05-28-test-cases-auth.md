# auth-service 测试用例文档

> 版本: 1.0 | 日期: 2026-05-28 | 模块: auth-service (端口 8086)

---

## 1. 概述

本文档覆盖 auth-service 的全部对外接口及核心工具类，包含用户端 `/api/user/auth`、商家端 `/api/seller/auth`、内部接口 `/internal/auth` 以及 BCryptUtil 工具类。用例按业务场景分组，共 **41 个** 用例。

| 分组 | 数量 | 覆盖范围 |
|------|------|----------|
| 用户注册 | 8 | 正常注册、唯一性冲突、字段格式校验 |
| 用户登录 | 7 | 正常登录、密码错误、账户禁用、参数缺失 |
| 商家注册/登录 | 4 | 商家注册、登录、唯一性、accountType |
| 登出 | 2 | 有/无 Token 登出 |
| 用户名/手机号查重 | 5 | 可用/不可用状态、格式校验 |
| 店员注册 | 3 | 正常注册、唯一性冲突、默认密码 |
| BCryptUtil 工具类 | 4 | 哈希、匹配、强度 |
| 参数校验异常 | 7 | Controller 层 @Valid 校验 |

---

## 2. 测试环境

| 项目 | 说明 |
|------|------|
| 服务端口 | 8086 |
| 数据库 | eureka_auth |
| 基础路径(用户) | `/api/user/auth` |
| 基础路径(商家) | `/api/seller/auth` |
| 基础路径(内部) | `/internal/auth` |
| BCrypt 强度 | 12 |
| 预期异常 | AuthException(code=400), MethodArgumentNotValidException(400) |
| 认证框架 | Sa-Token (StpUtil) |

---

## 3. 测试用例表

### 3.1 用户注册

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| AU-001 | 用户注册成功 | 数据库中不存在该用户名和手机号 | 1. POST `/api/user/auth/register`<br>2. Body: `{"username":"testuser","password":"Abc123","nickname":"测试","phone":"13800138001"}` | 1. 返回 HTTP 200<br>2. ApiResponse.token 不为空<br>3. ApiResponse.accountType = "USER"<br>4. ApiResponse.userInfo.nickname = "测试"<br>5. 数据库 t_user 表新增记录,status=1 | P0 |
| AU-002 | 注册-用户名重复 | 数据库中已存在 username="testuser" | 1. 先执行 AU-001 注册成功<br>2. 再次 POST `/api/user/auth/register`<br>3. Body: `{"username":"testuser","password":"Def456","nickname":"重复","phone":"13800138002"}` | 1. 返回 HTTP 400<br>2. 异常 message 包含 "用户名已存在" | P0 |
| AU-003 | 注册-手机号重复 | 数据库中已存在 phone="13800138001" | 1. 先执行 AU-001 注册成功<br>2. 再次 POST `/api/user/auth/register`<br>3. Body: `{"username":"newuser","password":"Abc123","nickname":"重复","phone":"13800138001"}` | 1. 返回 HTTP 400<br>2. 异常 message 包含 "手机号已存在" | P1 |
| AU-004 | 注册-用户名过短 | — | POST `/api/user/auth/register` Body: `{"username":"ab","password":"Abc123","phone":"13800138001"}` | 返回 HTTP 400, message 包含用户名长度要求 (BCryptUtil 拒绝 3 位以下) | P1 |
| AU-005 | 注册-用户名含特殊符号 | — | POST `/api/user/auth/register` Body: `{"username":"user@name!","password":"Abc123","phone":"13800138001"}` | 返回 HTTP 400, 提示用户名只能包含字母数字下划线 | P2 |
| AU-006 | 注册-密码格式错误(只含字母) | — | POST `/api/user/auth/register` Body: `{"username":"validuser","password":"abcdef","phone":"13800138001"}` | 返回 HTTP 400, Service 层 BCryptUtil.isValidPasswordFormat 校验失败, 抛 AuthException 提示密码必须包含字母和数字 | P1 |
| AU-007 | 注册-密码格式错误(只含数字) | — | POST `/api/user/auth/register` Body: `{"username":"validuser","password":"123456","phone":"13800138001"}` | 返回 HTTP 400, Service 层 BCryptUtil.isValidPasswordFormat 校验失败, 抛 AuthException 提示密码必须包含字母和数字 | P1 |
| AU-008 | 注册-手机号格式错误 | — | POST `/api/user/auth/register` Body: `{"username":"validuser","password":"Abc123","phone":"12345678901"}` | 返回 HTTP 400, 提示手机号格式不正确 (非 1[3-9] 开头) | P1 |

### 3.2 用户登录

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| AU-009 | 用户登录成功 | 数据库中已注册用户 testuser/Abc123 | POST `/api/user/auth/login` Body: `{"username":"testuser","password":"Abc123"}` | 1. HTTP 200<br>2. ApiResponse.token 不为空 (Sa-Token)<br>3. ApiResponse.accountType = "USER"<br>4. ApiResponse.userInfo.nickname 不为空 | P0 |
| AU-010 | 登录-密码错误 | 用户 testuser 密码为 Abc123 | POST `/api/user/auth/login` Body: `{"username":"testuser","password":"WrongPass1"}` | 1. HTTP 400<br>2. message 提示 "用户名或密码错误" | P0 |
| AU-011 | 登录-用户名不存在 | 数据库中无该用户名 | POST `/api/user/auth/login` Body: `{"username":"nonexistent","password":"Abc123"}` | 1. HTTP 400<br>2. message 提示 "用户名或密码错误" | P0 |
| AU-012 | 登录-账户已禁用 | 该用户 status = 0 | 1. 通过 SQL 将 testuser 的 status 设为 0<br>2. POST `/api/user/auth/login` Body: `{"username":"testuser","password":"Abc123"}` | 1. HTTP 400<br>2. message 提示 "账号已被禁用" 或类似 | P1 |
| AU-013 | 登录-用户名为空 | — | POST `/api/user/auth/login` Body: `{"username":"","password":"Abc123"}` | HTTP 400, @NotBlank 校验触发 | P2 |
| AU-014 | 登录-密码为空 | — | POST `/api/user/auth/login` Body: `{"username":"testuser","password":""}` | HTTP 400, @NotBlank 校验触发 | P2 |
| AU-015 | 登录-区分用户/商家 accountType | 用户 testuser 和商家 seller1 都存在 | 1. POST `/api/user/auth/login` Body: `{"username":"testuser","password":"Abc123"}`<br>2. POST `/api/seller/auth/login` Body: `{"username":"seller1","password":"Abc123"}` | 步骤1 的 accountType = "USER"；步骤2 的 accountType = "MERCHANT" | P1 |

### 3.3 商家注册/登录

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| AU-016 | 商家注册成功 | 数据库中不存在该商家用户名和手机号 | POST `/api/seller/auth/register` Body: `{"username":"seller1","password":"Seller123","nickname":"商家一号","phone":"13900139001"}` | 1. HTTP 200<br>2. ApiResponse.token 不为空<br>3. ApiResponse.accountType = "MERCHANT"<br>4. ApiResponse.merchantInfo.nickname = "商家一号" | P0 |
| AU-017 | 商家登录成功 | 商家 seller1 已注册 | POST `/api/seller/auth/login` Body: `{"username":"seller1","password":"Seller123"}` | HTTP 200, token 不为空, accountType = "MERCHANT" | P0 |
| AU-018 | 商家注册-用户名重复(跨表不冲突) | 用户端已存在 username="testuser" | POST `/api/seller/auth/register` Body: `{"username":"testuser","password":"Seller123","phone":"13900139002"}` | 注册成功 (用户和商家是独立表,用户名不跨表互斥) | P1 |
| AU-019 | 商家登录-返回 merchantInfo | 商家 seller1 已注册 | POST `/api/seller/auth/login` Body: `{"username":"seller1","password":"Seller123"}` | ApiResponse.merchantInfo 结构包含 nickname, avatar 等字段 | P1 |

### 3.4 登出

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| AU-020 | 用户登出成功 | 已获取有效 Sa-Token | 1. 先通过登录拿到 token<br>2. POST `/api/user/auth/logout` Header: `Authorization: Bearer <token>` | 1. HTTP 200<br>2. Sa-Token 使该 token 失效<br>3. 再次使用该 token 访问需鉴权的接口返回 401 | P0 |
| AU-021 | 无 Token 登出 | 未提供 Token | POST `/api/seller/auth/logout` (无 Authorization Header) | HTTP 200 (StpUtil.logout 内部处理,无 token 不抛异常) | P2 |

### 3.5 用户名/手机号查重

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| AU-022 | 用户名可用 | 数据库中不存在该用户名 | GET `/api/user/auth/check-username?username=newuser` | HTTP 200, ApiResponse.available = true | P1 |
| AU-023 | 用户名不可用 | 数据库中已存在 username="testuser" | GET `/api/user/auth/check-username?username=testuser` | HTTP 200, ApiResponse.available = false | P1 |
| AU-024 | 手机号可用 | 数据库中不存在该手机号 | GET `/api/user/auth/check-phone?phone=13700137000` | HTTP 200, ApiResponse.available = true | P1 |
| AU-025 | 手机号不可用 | 数据库中已存在 phone="13800138001" | GET `/api/user/auth/check-phone?phone=13800138001` | HTTP 200, ApiResponse.available = false | P1 |
| AU-026 | 商家端查重接口可用 | — | GET `/api/seller/auth/check-username?username=seller1` | 同 AU-022/AU-023 逻辑, 在 t_merchant 表中查询 | P2 |
| AU-026b | 商家端手机号查重 | — | GET `/api/seller/auth/check-phone?phone=13900139001` | HTTP 200, 逻辑同 AU-024/AU-025 在 t_merchant 表中查询 | P2 |

### 3.6 店员注册（内部接口）

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| AU-027 | 店员注册成功 | — | POST `/internal/auth/register-employee` Body: `{"username":"emp01","password":"Emp123","nickname":"店员A","phone":"13200132001"}` | 1. HTTP 200<br>2. ApiResponse.merchantId 为有效的雪花 ID<br>3. t_merchant 表新增记录,status=1<br>4. 密码 BCrypt 加密存储 | P0 |
| AU-028 | 店员注册-用户名重复 | 已存在员工 emp01 | POST `/internal/auth/register-employee` Body: `{"username":"emp01","password":"Emp456","phone":"13200132002"}` | HTTP 400, message 提示 "用户名已存在" | P1 |
| AU-029 | 店员注册-默认密码场景 | 商家端调用且 password 未传 | POST `/internal/auth/register-employee` Body: `{"username":"emp02","nickname":"店员B","phone":"13200132003"}` | 密码被设置为默认 "123456", BCrypt 加密写入数据库 | P1 |

### 3.7 BCryptUtil 工具类

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| AU-030 | hashPassword 生成有效 BCrypt 哈希 | — | 调用 `BCryptUtil.hashPassword("Abc123")` | 1. 返回字符串以 `$2a$` 开头<br>2. 长度为 60 字符<br>3. 每次调用结果不同 (不同 salt) | P0 |
| AU-031 | 正确密码验证通过 | — | 1. hash = BCryptUtil.hashPassword("Abc123")<br>2. BCryptUtil.verifyPassword("Abc123", hash) | verifyPassword 返回 true | P0 |
| AU-032 | 错误密码验证失败 | — | 1. hash = BCryptUtil.hashPassword("Abc123")<br>2. BCryptUtil.verifyPassword("WrongPass1", hash) | verifyPassword 返回 false | P0 |
| AU-033 | BCrypt 强度参数为 12 | — | 检查 BCryptUtil.hashPassword 实现 | hashPassword 调用 `BCrypt.gensalt(12)` 或等价, rounds=12 | P1 |

### 3.8 参数校验异常

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| AU-034 | RegisterRequest 用户名为 null | — | POST `/api/user/auth/register` Body: `{"password":"Abc123","phone":"13800138001"}` | HTTP 400, MethodArgumentNotValidException 处理, message 包含 "用户名" | P1 |
| AU-035 | RegisterRequest 密码为 null | — | POST `/api/user/auth/register` Body: `{"username":"testuser","phone":"13800138001"}` | HTTP 400, MethodArgumentNotValidException 处理 | P1 |
| AU-036 | LoginRequest 用户名为 null | — | POST `/api/user/auth/login` Body: `{"password":"Abc123"}` | HTTP 400, @NotBlank 校验触发 | P1 |
| AU-037 | LoginRequest 密码为 null | — | POST `/api/user/auth/login` Body: `{"username":"testuser"}` | HTTP 400, @NotBlank 校验触发 | P1 |
| AU-038 | RegisterRequest 手机号格式校验不通过 | — | POST `/api/user/auth/register` Body: `{"username":"testuser","password":"Abc123","phone":"138001380"}` | HTTP 400, 提示手机号格式错误 (正则 1[3-9]\\d{9}) | P2 |
| AU-039 | BCryptUtil 校验用户名格式-合法 | — | 调用 `BCryptUtil.isValidUsernameFormat("user_123")` | 返回 true | P2 |
| AU-040 | BCryptUtil 校验用户名格式-非法 | — | 调用 `BCryptUtil.isValidUsernameFormat("user name")` | 返回 false (含空格/特殊符号) | P2 |

---

## 4. 测试要点总结

### 4.1 业务流覆盖率

| 模块 | P0 | P1 | P2 | 合计 |
|------|----|----|----|------|
| 用户注册 | 1 | 4 | 3 | 8 |
| 用户登录 | 3 | 2 | 2 | 7 |
| 商家注册/登录 | 2 | 2 | 0 | 4 |
| 登出 | 1 | 0 | 1 | 2 |
| 用户名/手机号查重 | 0 | 4 | 2 | 6 |
| 店员注册 | 1 | 2 | 0 | 3 |
| BCryptUtil 工具类 | 3 | 1 | 0 | 4 |
| 参数校验异常 | 0 | 5 | 2 | 7 |
| **合计** | **11** | **20** | **10** | **41** |

### 4.2 边界与关键点

1. **账户 type 路由**：用户端 `/api/user/auth/*` 操作 t_user/t_user_info，商家端 `/api/seller/auth/*` 操作 t_merchant/t_merchant_info，需分别验证 accountType 和返回结构
2. **唯一性隔离**：用户和商家的用户名/手机号在各表独立，不跨表互斥 (AU-018)
3. **BCrypt 强度**：密码强度为 12，每次 hash 结果不同，不影响 verify
4. **Sa-Token 生命周期**：logout 后 token 立即失效，无 token 时 logout 不抛异常 (AU-021)
5. **默认密码**：店员注册不传 password 时兜底为 "123456" (AU-029)
6. **雪花 ID**：所有注册操作 (用户/商家/员工) 使用雪花算法生成分布式 ID
