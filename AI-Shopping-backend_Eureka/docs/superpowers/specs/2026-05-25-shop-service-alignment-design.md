# Shop Service 架构对齐方案

## 目标

将 shop-service 的架构模式对齐到 auth-service 的标准，消除技术债和架构不一致。

---

## 背景

当前两个服务在同一个微服务体系中，但 shop-service 的编码风格、异常处理、响应格式与 auth-service 存在显著差异：

- auth-service 返回 `ApiResponse<T>` 统一泛型响应，shop-service 返回 `Map<String, Object>`
- auth-service 使用 `@Valid` + DTO 参数校验，shop-service 在 Controller 中 `if` 硬编码校验
- auth-service 有 `@RestControllerAdvice` 全局异常处理，shop-service 异常直接吐给前端
- auth-service 使用 Snowflake Long 主键，shop-service 使用 UUID String
- shop-service 的 Feign 调用写在 Controller 层（本应封装在 Service 层）

---

## 变更范围

### 批次规划 (P1 → P5)

各批次之间**无硬依赖**，可独立开发、测试、上线。

---

### P1：响应格式 + 全局异常处理

**影响文件：**

| 文件 | 操作 |
|------|------|
| `shop-service/src/main/java/.../exception/ShopException.java` | **新建** — 自定义业务异常 |
| `shop-service/src/main/java/.../controller/GlobalExceptionHandler.java` | **新建** — `@RestControllerAdvice` 统一处理 |
| `shop-service/.../controller/ShopManageController.java` | **修改** — 返回值改为 `ApiResponse<T>`，异常 throw 代替返回 Map |
| `shop-service/.../controller/ShopQueryController.java` | **修改** — 同 |
| `shop-service/.../controller/ShopUserController.java` | **修改** — 同 |
| `shop-service/.../controller/Internal/InternalShopController.java` | **修改** — 同 |
| `shop-service/pom.xml` | **修改** — 添加 `spring-boot-starter-validation` |

**核心改动：**

```java
// ShopException.java — 与 AuthException 模式一致
public class ShopException extends RuntimeException {
    private int code = 400;
    public ShopException(String message) { super(message); }
    public ShopException(int code, String message) { super(message); this.code = code; }
    public int getCode() { return code; }
}
```

```java
// GlobalExceptionHandler.java — 与 Auth 模式一致
@RestControllerAdvice
public class GlobalExceptionHandler {
    // ShopException → 400
    // MethodArgumentNotValidException → 400
    // Exception → 500
}
```

**Controller 变更示例（P1 阶段，仅改返回格式）：**

```java
// 之前
@PostMapping("/shop/register")
public Map<String, Object> createShop(@RequestBody Shop shop, @RequestHeader("X-User-Id") String userId) {
    if (shop == null || shop.getName() == null || shop.getName().trim().isEmpty()) {
        return Map.of("success", false, "message", "店铺名称不能为空");
    }
    // ...
    return Map.of("success", true, "message", "创建店铺成功", "id", shop.getId());
}

// 之后（P1）
@PostMapping("/shop/register")
public ApiResponse<Map<String, Object>> createShop(@RequestBody Shop shop,
                                                    @RequestHeader("X-User-Id") String userId) {
    // TODO(P2): 替换为 DTO + @Valid 校验
    if (shop == null || shop.getName() == null || shop.getName().trim().isEmpty()) {
        throw new ShopException("店铺名称不能为空");
    }
    // ...
    return ApiResponse.success("创建店铺成功", Map.of("id", shop.getId()));
}
```

> P2 阶段再移除手写校验、替换为 `@Valid CreateShopRequest`。

---

### P2：DTO 引入 + 参数校验

**影响文件：**

| 文件 | 操作 |
|------|------|
| `shop-service/.../dto/CreateShopRequest.java` | **新建** — `@NotBlank name, description, logoId` |
| `shop-service/.../dto/UpdateShopRequest.java` | **新建** |
| `shop-service/.../dto/AddEmployeeRequest.java` | **新建** |
| `shop-service/.../dto/ShopManageResult.java` | **新建** |
| `shop-service/.../controller/ShopManageController.java` | **修改** — 入参替换为 DTO + `@Valid` |
| `shop-service/.../controller/ShopUserController.java` | **修改** — 响应部分替换为 DTO |
| `shop-service/.../service/ShopService.java` | **修改** — 接口方法签名使用 DTO |
| `shop-service/.../service/impl/ShopServiceImpl.java` | **修改** — 实现使用 DTO |

**DTO 示例：**

```java
// CreateShopRequest.java
@Data
public class CreateShopRequest {
    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称最长100个字符")
    private String name;

    @Size(max = 500, message = "店铺描述最长500个字符")
    private String description;

    private String logoId;
}
```

---

### P3：Service 层重构 — Feign 调用下移

**问题：** `ShopManageController` 直接调用 `productFeignClient` 和 `authFeignClient`，Controller 承担了业务编排职责。

**影响文件：**

| 文件 | 操作 |
|------|------|
| `shop-service/.../service/ShopService.java` | **修改** — 新增 `createProduct`, `updateProduct`, `deleteProduct`, `addEmployee`, `removeEmployee` 方法 |
| `shop-service/.../service/impl/ShopServiceImpl.java` | **修改** — 注入 `ProductFeignClient`, `AuthFeignClient` 实现上述方法 |
| `shop-service/.../controller/ShopManageController.java` | **修改** — 删除 Feign 注入，Controller 只调用 ShopService |
| `shop-service/.../controller/ShopQueryController.java` | **修改** — 同样将 ProductFeignClient 调用下移 |

**原则：**
- Controller 只做：路由 + 参数校验 + 调用 Service
- Service 做：业务逻辑 + 跨服务 Feign 调用 + 事务
- Mapper 做：单一数据访问

---

### P4：ID + 类型统一

**影响范围：**

| 项目 | 当前 | 改为 |
|------|------|------|
| `Shop.id` | `String` (UUID) | `Long` (Snowflake) |
| `ProductShop.id` | `String` (UUID) | `Long` (Snowflake) |
| `MerchantRole.merchantId` | `String` | `Long` |
| `MerchantRole.shopId` | `String` | `Long` |
| `MerchantRole.role` | `String "1"/"2"` | `Integer 1/2` |
| `ProductShop.productId` | `String` | `Long` |
| `ProductShop.shopId` | `String` | `Long` |
| `MerchantRole.assignedBy` | `String` | `Long` |
| `Shop.merchantId` | `String` | `Long` |

**影响文件：** Model 3 个 + Mapper 3 个 + Service 3 个 + Controller 4 个 + common-api 中的 Feign 接口签名

**数据库迁移（P4 单独执行）：**

```sql
-- 新建 Long 类型 ID 列
ALTER TABLE shops ADD COLUMN id_new BIGINT;
ALTER TABLE shops ADD COLUMN merchant_id_new BIGINT;
ALTER TABLE shops CHANGE COLUMN id id_old VARCHAR(64);
ALTER TABLE shops CHANGE COLUMN merchant_id merchant_id_old VARCHAR(64);
-- 类似处理所有关联表 product_shops, merchant_roles
-- 数据迁移完成后再改名
```

**变更原则：** P4 是风险最高的批次，建议单独建分支，先完成数据库迁移脚本和回滚脚本，验证无误后再改代码。

---

### P5：配置清理

| 文件 | 操作 |
|------|------|
| `shop-service/.../config/DataSourceConfig.java` | **删除** — 改用 `application.yml` 自动配置 |
| `shop-service/src/main/resources/mapper/ShopMapper.xml` | **删除** — 空的 XML，已使用注解 |
| `shop-service/src/main/resources/mapper/ProductShopMapper.xml` | **删除** |
| `shop-service/src/main/resources/mapper/MerchantRoleMapper.xml` | **删除** |
| `shop-service/src/main/resources/application.yml` | **修改** — 添加完整数据源配置（替代 DataSourceConfig） |

**说明：** auth-service 没有手动 DataSourceConfig，直接走 Spring Boot 自动配置。shop-service 的 DataSourceConfig 只是手动创建了 HikariCP 连接池，没有特殊逻辑，删除后可简化维护。

---

## 依赖关系

```
P1 (响应+异常) ──┬── P2 (DTO+校验) ──┬── P3 (Feign下移)
                 │                   │
                 │                   └── P4 (ID+类型) 无依赖
                 │
                 └── P5 (配置清理) 无依赖
```

**批次间无阻塞依赖**，但建议顺序：P1 → P2 → P3 → P4 → P5。

---

## 验证策略

| 批次 | 验证方式 |
|------|---------|
| P1 | 启动后调用任一 API，确认返回 `ApiResponse` JSON 格式而非 `Map`；传非法参数确认返回 400 |
| P2 | 传空 name 创建店铺，确认返回参数校验错误信息而非内部 500 |
| P3 | 完整的创建店铺+添加商品+添加员工流程，确认结果与改动前一致 |
| P4 | 数据库迁移脚本先在测试库跑通；所有 CRUD 接口回归 |
| P5 | 启动日志无报错，所有接口正常响应 |

---

## 回滚策略

- 每批次独立 PR，回滚只需 `git revert` 对应提交
- P4 数据库变更必须提供回滚 SQL（DROP NEW COLUMNS + RENAME BACK）
- P5 删除文件前先确认是否有其他引用
