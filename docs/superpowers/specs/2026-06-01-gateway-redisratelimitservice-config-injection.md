# Gateway 服务 Bug 修复：配置注入与 Token 验证一致性

## 概述

修复 gateway-service 中的两个 Bug：

- **Bug #5**: `RedisRateLimitService` 限流参数硬编码，未使用 `IpRateLimitProperties` 配置类
- **Bug #4**: `AuthServiceImpl.validateToken()` 手动查询 Redis 绕过 Sa-Token 体系，导致 session 过期时返回 403 替代 401

---

## Bug #5：RedisRateLimitService 配置注入

### 问题

`RedisRateLimitService` 中限流参数为硬编码常量：

```java
private static final long MAX_REQUESTS = 300;
private static final long WINDOW_SECONDS = 60;
```

项目已存在 `IpRateLimitProperties` 配置类（`@ConfigurationProperties(prefix = "ip-rate-limit")`），但未被使用。修改 `application.yml` 的 `ip-rate-limit.*` 不会生效。

### 方案

构造器注入 `IpRateLimitProperties`，替代硬编码常量。`KEY_PREFIX` 保留为常量。

| 文件 | 变更 |
|------|------|
| `RedisRateLimitService.java` | 删除 `MAX_REQUESTS`、`WINDOW_SECONDS` 常量；新增 `IpRateLimitProperties` 构造器注入；`isAllowed()` 中调用配置值 |

---

## Bug #4：validateToken 一致性修复

### 问题

`AuthServiceImpl.validateToken()` 手动查询 Redis：

```java
String tokenKey = "satoken:login:token:" + token;
String loginId = stringRedisTemplate.opsForValue().get(tokenKey);
```

而 `getAccountType()` 使用 Sa-Token API（`StpUtil.getTokenSessionByToken`），两套路径不一致。当 Sa-Token session 过期但 token key 仍在 Redis 中时：
- `validateToken()` → 通过（key 存在）
- `getAccountType()` → 返回 null（session 过期）
- `hasPermission(null, path, request)` → false
- 用户收到 **403"无权限"**（错误）而非 **401"登录已过期"**（正确）

### 方案

| 文件 | 变更 |
|------|------|
| `AuthServiceImpl.java` | `validateToken()` 改用 `StpUtil.getLoginIdByToken(token)`，移除手动 Redis 查询；删除 `StringRedisTemplate` 依赖 |
| `SaTokenAuthGlobalFilter.java` | `getAccountType()` 返回 null 时抛出 `GatewayAuthException(401, "登录已过期，请重新登录")` |

---

## 影响范围

| 维度 | 说明 |
|------|------|
| API 变动 | 无，仅内部实现变更 |
| 配置入口 | `application.yml` 中 `ip-rate-limit.*` |
| 测试影响 | `IpRateLimitFilterTest` 不变（原有 properties 覆盖测试路径更真实） |
| 测试文件 | 无需新增或修改 |
