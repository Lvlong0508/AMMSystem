# Gateway 内部路由移除 & 测试修复 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**目标：** 移除网关多余 internal 路由消除外部暴露面，修复 6+1 个既存测试故障，清理测试脏数据

**架构：** 4 个简单变更（yml + 2 个测试文件 + 1 个新测试调整）+ DB 清理，无架构改动

**技术栈：** Spring Cloud Gateway, Spring Boot Test, MySQL, JUnit 5

---

### Task 1: 注释掉 application.yml 中的 internal 路由

**文件：**
- 修改：`gateway-service/src/main/resources/application.yml:142-174`

- [ ] **Step 1: 注释 4 个 internal 路由块**

将 yml 中 `# ========== 内部路由 ==========` 及其下方的 4 个路由定义 (`internal-shop`, `internal-auth`, `internal-product`, `internal-order`) 全部注释掉。

修改后：

```yaml
# ========== 内部路由 ==========
# 内部服务间通过 Eureka 直连，不需要经过网关，注释掉以防止外部直接访问
# - id: internal-shop
#   uri: lb://shop-service
#   predicates:
#     - Path=/internal/shop/**
#   filters:
#     - StripPrefix=1
#
# - id: internal-auth
#   uri: lb://auth-service
#   predicates:
#     - Path=/internal/auth/**
#   filters:
#     - StripPrefix=1
#
# - id: internal-product
#   uri: lb://product-service
#   predicates:
#     - Path=/internal/product/**
#   filters:
#     - StripPrefix=1
#
# - id: internal-order
#   uri: lb://order-service
#   predicates:
#     - Path=/internal/order/**
#   filters:
#     - StripPrefix=1
```

- [ ] **Step 2: 确认无其他引用**

```bash
rg '/internal/' gateway-service/src/main  # 确认只有 routes 配置涉及 /internal/
```

预期：只匹配到 yml 中的路由配置和被注释掉的部分，无代码层引用。

---

### Task 2: 修复 AuthServiceImplTest 6 个失败用例

**文件：**
- 修改：`gateway-service/src/test/java/com/gzasc/aishopping/gateway/service/AuthServiceImplTest.java:60,72,84,89,121,127,133,138`

- [ ] **Step 1: 修改 6 个 assertTrue 的 accountType 参数**

将 `"USER:u001"` 改为 `"USER"`，`"MERCHANT:m001"` 改为 `"MERCHANT"`：

| 行号 | 原代码 | 新代码 |
|------|--------|--------|
| 60 | `hasPermission("USER:u001", "/api/user/product/all", request)` | `hasPermission("USER", "/api/user/product/all", request)` |
| 72 | `hasPermission("MERCHANT:m001", "/api/seller/product/list", request)` | `hasPermission("MERCHANT", "/api/seller/product/list", request)` |
| 84/89 | `hasPermission("MERCHANT:m001", "/api/seller/shop/manage/update", request)` | `hasPermission("MERCHANT", "/api/seller/shop/manage/update", request)` |
| 121 | `hasPermission("MERCHANT:m001", "/api/seller/shop/query/list", request)` | `hasPermission("MERCHANT", "/api/seller/shop/query/list", request)` |
| 127 | `hasPermission("MERCHANT:m001", "/api/seller/shop/manage/100/ship", request)` | `hasPermission("MERCHANT", "/api/seller/shop/manage/100/ship", request)` |
| 133/138 | `hasPermission("MERCHANT:m001", "/api/seller/shop/100/products/200", request)` | `hasPermission("MERCHANT", "/api/seller/shop/100/products/200", request)` |

注意：`assertFalse` 用例不改（它们本来就是 false）。

- [ ] **Step 2: 运行测试验证**

```bash
mvn test -pl gateway-service -Dtest=AuthServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：31 tests run, 0 failures, 0 errors

---

### Task 3: 修复 SaTokenAuthGlobalFilterTest 弱断言

**文件：**
- 修改：`gateway-service/src/test/java/com/gzasc/aishopping/gateway/filter/SaTokenAuthGlobalFilterTest.java:119`

- [ ] **Step 1: 替换弱断言**

原代码（第 119 行）：
```java
.expectStatus().value(s -> assertNotEquals(HttpStatus.FORBIDDEN.value(), s.intValue()));
```

改为：
```java
.expectStatus().isOk();
```

并删除不再需要的导入 `import static org.junit.jupiter.api.Assertions.assertNotEquals;`（第 16 行），确认 `assertNotEquals` 无其他引用。

- [ ] **Step 2: 运行测试验证**

```bash
mvn test -pl gateway-service -Dtest=SaTokenAuthGlobalFilterTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：7 tests run, 0 failures, 0 errors

---

### Task 4: 更新 GatewayFullIntegrationTest 内部路由测试

**文件：**
- 修改：`gateway-service/src/test/java/com/gzasc/aishopping/gateway/GatewayFullIntegrationTest.java:250-270`

- [ ] **Step 1: 更新 internal 路由测试用例**

将 `internalRouteWithoutToken_returns401` 改为期望 404：

```java
@Test
@DisplayName("GW-INT-INT-001: 内部路由已移除，返回 404")
void internalRoute_returns404() {
    webTestClient.get().uri("/internal/auth/check")
            .exchange()
            .expectStatus().isNotFound();
}
```

删除 `internalRouteWithToken_passes` 方法（internal 路由已移除，不再需要 Token 测试用例）。

- [ ] **Step 2: 运行测试验证**

```bash
mvn test -pl gateway-service -Dtest=GatewayFullIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：21 tests run, 0 failures, 0 errors（用例数从 21 减为 20，如果删了一个。或者保留 21 如果合并了）

实际保持 21 个用例：替换原 2 个 internal 测试为 2 个 404 测试。

---

### Task 5: 清理测试脏数据

**文件：**
- 无代码文件修改

- [ ] **Step 1: 查询测试产生的账号**

```sql
SELECT id, username, nickname FROM user WHERE username LIKE 'int_test_%';
SELECT id, username, nickname FROM merchant WHERE username LIKE 'int_test_%';
```

- [ ] **Step 2: 删除测试账号**

```sql
DELETE FROM user WHERE username LIKE 'int_test_%';
DELETE FROM merchant WHERE username LIKE 'int_test_%';
```

提交事务确认。

---

### Task 6: 全量回归验证

- [ ] **Step 1: 运行 gateway-service 所有测试**

```bash
mvn test -pl gateway-service -Dsurefire.failIfNoSpecifiedTests=false
```

预期：所有测试通过，0 failures

- [ ] **Step 2: 提交**

```bash
git add -A
git commit -m "fix(gateway): 移除 internal 路由消除外部暴露面，修复 7 个测试故障"
```
