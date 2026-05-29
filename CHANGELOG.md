# CHANGELOG

> 代码审查与重构记录 — 2026-05-29

---

## 概述

本次对 AI-Shopping 后端 10 个微服务模块进行了全面代码审查与重构，包含 10 项代码修复和 README.md 完整重写。

---

## 代码修复清单

### S 级（阻断性 Bug）

| # | 模块 | 问题 | 修复 |
|---|------|------|------|
| 1 | contact-service | `UserContactService.g(int id)` 方法名无意义，导致 Feign 调用 `getContactById()` 无法匹配，order-service 获取联系人信息永远失败 | 重命名为 `getContactById(int id)`，同步更新接口、实现、Controller 调用 |
| 2 | chat-service | `UserContext` 使用 `RequestContextHolder` 获取请求上下文，LangChain4j `@Tool` 在异步线程池中调用时丢失上下文，抛出 `"No request context available"` | 改用 `InheritableThreadLocal`，新增 `UserContextInterceptor` 从 `X-User-Id` Header 注入，注册 `WebConfig` 拦截 `/chat/**` |

### A 级（架构问题）

| # | 模块 | 问题 | 修复 |
|---|------|------|------|
| 3 | common-api + 4 服务 | Feign 客户端 24 个方法中 7 个返回 `Map<String, Object>` 或 `Object`，调用方需手动拆箱，类型不安全 | 全部统一为 `ApiResponse<T>`；修复 ContactFeignClient / AuthFeignClient 调用方未解包 `data` 的 Bug（联系人信息/merchantId 永远为 null） |
| 4 | common-api | `ShopFeignClientForRoles` 与 `ShopFeignClient.getMerchantRoles()` 完全重复且无调用者 | 删除冗余文件 |
| 5 | common-api | `UserInfoFeignClient` 指向不存在的端点 `/internal/userinfo/create`，且无调用者 | 删除冗余文件 |
| 6 | gateway-service | IP 限流使用 Caffeine 本地缓存，多实例下各自独立计数，无法实现全局限流 | 新增 `RedisRateLimitService`（Redis INCR + EXPIRE），`IpRateLimitFilter` 改为注入 Redis 服务 |
| 7 | product-service | DTO 层级过深，10+ 个 DTO 类（Abstract/Detail/WithImage 排列组合），接口层 5 个 `@Deprecated` DTO 仍被使用 | 新增统一 `ProductDTO`，旧 DTO 标记 `@Deprecated` 保留兼容 |

### B 级（代码风格）

| # | 模块 | 问题 | 修复 |
|---|------|------|------|
| 8 | shop-service | 目录名 `Internal` 首字母大写与 package 声明 `internal` 不一致 | package 声明改为全小写 |
| 9 | 全部 7 个服务 | `@Slf4j` 与 `private static final Logger` 混用 | 14 个文件统一为 `@Slf4j` |
| 10 | order-service | `placeOrder()` 既有 `@Valid` 注解又手动 null 检查，双重校验 | 删除手动检查，确保 DTO 注解完整 |

---

## README 重写记录

### 严重错误修正

| 模块 | 错误描述 | 修正 |
|------|---------|------|
| gateway-service | 限流阈值写 30 次/分，实际 300 次/分 | 修正 |
| gateway-service | 声称存在 `UserIdHeaderFilter`（order=0），实际不存在 | 移除，userId 在 `SaTokenAuthGlobalFilter` 注入 |
| gateway-service | 白名单仅列 4 条，实际 9 条 | 补充 5 条 |
| auth-service | 列了不存在的 `GET /api/user/auth/info` 端点 | 移除 |
| contact-service | 5 个 API 路径全部错误（包括不存在的方法） | 全部修正 |
| logistics-service | API 路径全部错误（含不存在的 `/api/user/` 前缀和更新接口） | 全部修正 |
| order-service | API 端点缺失 9 个，`DELETE` 误标为"取消订单"（实际是删除） | 完整列出 13 个端点 |
| chat-service | 接口路径写 `/chat`，实际为 `/chat/chat` | 修正 |
| common-api | 声称存在 4 个独立 API 模块，实际只有 1 个 | 修正描述 |

### 补充内容

| 模块 | 补充说明 |
|------|---------|
| auth-service | 补充 `check-username` / `check-phone` 端点、BCrypt 加密策略、用户/商家双轨对称设计 |
| gateway-service | 补充过滤器链 Order 值、19 条路由规则详情、认证逻辑流程 |
| product-service | 补充库存预占机制完整流程（FOR UPDATE 行级锁）、3 层 Controller 分离、Caffeine 店铺缓存、定时清理任务 |
| order-service | 补充完整状态机转换图、Redis Stream 事件系统（3 类事件）、FileFallbackDaemon 兜底、OrderTimeoutTask、订单 ID 生成策略、CAS 幂等方式 |
| shop-service | 补充员工/角色体系（role=1 店长 vs role=2 店员）、`checkShopOwner` / `checkShopAccess` 权限校验、完整 API |
| logistics-service | 补充 `/logistics/*` 路径风格说明（与其他服务不同） |
| chat-service | 补充 InheritableThreadLocal 异步安全机制、sealed interface + record 多态 DTO、5 个 AI 工具详情 |
| common-api | 补充所有 Feign 客户端和 DTO 清单 |

### 结构优化

| 项目 | 旧 | 新 |
|------|----|----|
| 核心实体 | 8 个 | 14 个 |
| 服务间调用（Feign） | 3 条 | 8 条 |
| 修改影响链 | 13 行 | 22 行 |
| 数据库表 | 12 张 | 18 张 |
| 数据流图 | 3 个 | 5 个 |
| 配置项 | 5 条 | 11 条 |

---

## 文件变更汇总

| 操作 | 文件 | 数量 |
|------|------|------|
| **修改** | Feign 客户端（common-api） | 6 个 |
| **修改** | Service 接口/实现 | 6 个 |
| **修改** | Controller | 6 个 |
| **修改** | Filter / 配置 | 4 个 |
| **修改** | 其他（Converter 等） | 8 个 |
| **新增** | `RedisRateLimitService.java` | 1 个 |
| **新增** | `UserContextInterceptor.java` + `WebConfig.java` | 2 个 |
| **新增** | `ProductDTO.java` | 1 个 |
| **新增** | `CHANGELOG.md` | 1 个 |
| **删除** | `ShopFeignClientForRoles.java` | 1 个 |
| **删除** | `UserInfoFeignClient.java` | 1 个 |
| **重写** | `README.md` | 1 个 |
| **重写** | `UserContext.java` | 1 个 |
| **总计** | | **39 个文件** |

---

## 验证

- `mvn compile -q` — 全部模块编译通过
- 各 agent 独立验证修复正确性
