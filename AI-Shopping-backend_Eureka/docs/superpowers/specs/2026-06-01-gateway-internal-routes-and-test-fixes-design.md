# Gateway 内部路由保护 & 测试修复设计

## 概述

修复三个问题：
1. 网关移除多余的 `/internal/**` 路由，消除外部暴露面
2. 清理端到端测试产生的脏数据
3. 修复既存单元测试故障（6 个 `AuthServiceImplTest` + 1 个 `SaTokenAuthGlobalFilterTest`）

---

## 1. 内部路由处理

### 背景

- Feign 客户端通过 `@FeignClient(name = "xxx-service")` + Eureka **直接调用目标服务，不经网关**
- 网关上的 4 个 `/internal/**` 路由是多余的额外暴露入口
- 当前 `SaTokenAuthGlobalFilter` 未放行 `/internal/**`，这些路由即使存在也无法正常使用（返回 401）

### 方案：移除路由

最简单的方案：**注释掉 application.yml 中 4 个 internal 路由**：

```yaml
# ========== 内部路由 ==========
# 内部服务间通过 Eureka 直连，不需要经过网关，注释掉以防止外部访问
# - id: internal-shop
#   uri: lb://shop-service
#   predicates:
#     - Path=/internal/shop/**
#   filters:
#     - StripPrefix=1
# ... (internal-auth, internal-product, internal-order 同理)
```

**影响分析**：
- 外部请求 `/internal/**` → 404（无匹配路由），不暴露任何内部路径
- Feign 客户端直连 Eureka → 完全不受影响，正常通信
- 无需修改 `SaTokenAuthGlobalFilter.java`、`AuthWhitelistProperties.java`、`AuthService.java`
- 无需内部 token、无需 Feign 拦截器

---

## 2. 测试数据清理

清理端到端测试过程中写入的测试账号数据。通过 MySQL 查询识别并删除以 `int_test_` 前缀的测试账号。

---

## 3. 修复既存测试错误

### AuthServiceImplTest

6 个失败的 `hasPermission` 用例：
- 测试传递 `"USER:u001"` / `"MERCHANT:m001"` 给 `hasPermission()`
- 但源码 `hasPermission()` 的 `accountType` 参数现在是纯类型字符串（如 `"USER"`、`"MERCHANT"`），不是 `"USER:u001"` 格式
- `"USER".equals("USER:u001")` → false，导致断言失败

**修复**: 将 6 个 `assertTrue` 调用的第一个参数改为纯 accountType：

| 测试方法 | 旧参数 | 新参数 |
|---------|--------|--------|
| `hasPermission_userAccessUserApi_returnsTrue` | `"USER:u001"` | `"USER"` |
| `hasPermission_merchantAccessSellerApi_returnsTrue` | `"MERCHANT:m001"` | `"MERCHANT"` |
| `hasPermission_shopOwnerAccessManageApi_returnsTrue` | `"MERCHANT:m001"` | `"MERCHANT"` |
| `hasPermission_staffAccessQueryApi_returnsTrue` | `"MERCHANT:m001"` | `"MERCHANT"` |
| `hasPermission_staffAccessShipApi_returnsTrue` | `"MERCHANT:m001"` | `"MERCHANT"` |
| `hasPermission_shopOwnerAccessProductEdit_returnsTrue` | `"MERCHANT:m001"` | `"MERCHANT"` |

注意：`assertFalse` 用例传 `"USER:u001"` 时 `"MERCHANT".equals("USER:u001")` 已经是 false，行为正确，不需要改。

### SaTokenAuthGlobalFilterTest

`testMerchantAccessSellerApi_passes` 使用弱断言：
```java
.expectStatus().value(s -> assertNotEquals(HttpStatus.FORBIDDEN.value(), s.intValue()));
```
这只检查不是 403，但不验证是否是成功状态（可能是 401 或其他）。

**修复**: 改为
```java
.expectStatus().isOk();
```

---

## 4. GatewayFullIntegrationTest 更新

- `internalRouteWithoutToken_returns401` / `internalRouteWithToken_passes`：由于 internal 路由已从网关配置移除，这两个测试用例改为期望 **404**（路由未匹配），不再涉及认证逻辑

---

## 变更文件清单

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `application.yml` | 修改 | 注释掉 4 个 internal 路由配置 |
| `AuthServiceImplTest.java` | 修改 | 6 个用例参数修正 |
| `SaTokenAuthGlobalFilterTest.java` | 修改 | 弱断言修复 |
| `GatewayFullIntegrationTest.java` | 修改 | 内部路由测试用例更新为 404 |

---

## 测试策略

1. `AuthServiceImplTest`: 31 个用例全部通过
2. `SaTokenAuthGlobalFilterTest`: 7 个用例全部通过
3. `GatewayFullIntegrationTest`: 21 个用例全部通过
4. 运行 `mvn test -pl gateway-service` 确认 gateway-service 全部测试通过
