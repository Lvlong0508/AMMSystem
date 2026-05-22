# contact-service 架构统一规范设计

## 1. 目标

将 contact-service 的代码规范对齐到 auth-service 的既有标准，消除两个服务之间的架构差异，降低维护成本。

## 2. 范围

### 2.1 变更清单

| # | 变更项 | 当前状态 | 目标状态 | 涉及文件数 |
|---|--------|---------|---------|-----------|
| 1 | 响应格式 | 手写 `Map<String, Object>` | `common-api` 的 `ApiResponse<T>` | 5 |
| 2 | common-api 依赖 | 无 | 引入 `common-api` | 1 |
| 3 | userId 类型 | `int`/`Integer` | `Long`（适配 auth 雪花 ID） | 7 |
| 4 | shopId 类型 | `String` | **不涉及**（保持 String） | 0 |
| 5 | 本地 ApiResponse | 自实现但未使用 | 删除，统一用 common-api | 1 |
| 6 | 全局异常处理 | 返回 `Map<String, Object>` | 返回 `ApiResponse<Void>` | 1 |
| 7 | MyBatis SQL 日志 | 无 | 补充 `log-impl` 配置 | 1 |
| 8 | 日志方式 | `LoggerFactory` | `@Slf4j`（统一） | 1 |
| 9 | ContactFeignClient | 路径冲突 + 返回 `Map` | 修复路径 + 返回 `ApiResponse<ContactDTO>` | 1 |
| 10 | application.yml | 基础配置 | 补充 MyBatis 日志 | 1 |

### 2.2 非本次范围

- contact 自身 ID（`t_contact.id`、`shop_address.id`）保持 Integer 自增
- shopId 保持 `String` 类型，不做变更
- auth-service 的 `Date` → `LocalDateTime` 迁移（后续单独处理）
- 测试目录创建（其他服务也无测试目录）
- 业务逻辑变更

## 3. 详细设计

### 3.1 依赖变更 — `pom.xml`

在 `<dependencies>` 末尾追加：

```xml
<dependency>
    <groupId>com.gzasc</groupId>
    <artifactId>common-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

同时删除本地 `ApiResponse.java`（`com.gzasc.aishopping.contact.dto.ApiResponse`），统一使用 `com.gzasc.aishopping.common.response.ApiResponse`。

### 3.2 类型变更映射

| 层 | UserContact 系列（变更） | ShopAddress 系列（不变） |
|----|------------------------|------------------------|
| Controller | `X-User-Id` 解析: `Integer.parseInt()` → `Long.parseLong()` | shopId 保持 `String`，不变 |
| Service (接口) | `int userId` → `Long userId` | `String shopId`，不变 |
| Service (实现) | `int userId` → `Long userId` | `String shopId`，不变 |
| Mapper (参数) | `int userId` → `Long userId` | `String shopId`，不变 |
| Mapper (SQL) | `#{userId}` 类型不变 | 不变 |
| service 中关联 ID | `contactId`/`id` 保持 `int` | `addressId`/`id` 保持 `int` |

### 3.3 Controller 响应格式变更

所有 Controller 方法，将：

```java
// 旧
return Map.of("code", 200, "message", "操作成功", "data", data);
return Map.of("code", 400, "message", "错误信息");

// 新
return ApiResponse.success(data);
return ApiResponse.error(400, "错误信息");
```

涉及：

| Controller | 方法数 | 变更要点 |
|-----------|-------|---------|
| `UserContactController` | 6 个端点 | 全部替换 + `getUserId()` 改为解析 `Long` |
| `MerchantContactController` | 7 个端点 | 全部替换返回（shopId 保持 `String`，不变） |
| `InternalContactController` | 1 个端点 | 替换返回 + 异常改全局处理器处理 |
| `GlobalExceptionHandler` | 3 个处理 | 返回 `ApiResponse<Void>` + 日志改为 `@Slf4j` |

### 3.4 GlobalExceptionHandler 变更

```java
// 旧
@ExceptionHandler(ContactException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public Map<String, Object> handleContactException(ContactException e) { ... }

// 新
@ExceptionHandler(ContactException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ApiResponse<Void> handleContactException(ContactException e) { ... }
```

同时 logger 从 `LoggerFactory` 改为 `@Slf4j` 注解。

### 3.5 application.yml 补充

在 `mybatis.configuration` 下追加：

```yaml
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    default-executor-type: simple
```

### 3.6 ContactFeignClient 修复（common-api）

路径冲突：`getContactById` 和 `getContactByIdWithUser` 映射到同一路径 `/api/seller/contact/get/{id}`。

修复方案：

```java
@FeignClient(name = "contact-service")
public interface ContactFeignClient {

    @GetMapping("/internal/contact/{id}")
    ApiResponse<ContactDTO> getContactById(@PathVariable("id") Integer id);
}
```

### 3.7 InternalContactController 对齐

当前内部端点 `GET /internal/contact/{id}` 返回 `Map`，改为返回 `ApiResponse<Contact>`，同时将异常抛给全局处理器统一处理（移除 `try-catch`）。

## 4. 影响分析

### 4.1 数据库变更

`eureka_contact` 库仅涉及一张关联表的字段类型变更：

| 表 | 字段 | 旧类型 | 新类型 |
|---|------|-------|-------|
| `user_contact` | `user_id` | INT | BIGINT |

contact 和 shop_address 的主键 `id` 保持不变（INT AUTO_INCREMENT）。shop_address_rel.shop_id 保持 VARCHAR 不变。

### 4.2 跨服务影响

| 服务 | 影响 | 处理 |
|-----|------|------|
| auth-service | 无 | — |
| common-api | ContactFeignClient 返回值变更 | 调用方需适配 `ApiResponse<ContactDTO>` |
| 其他引用 contact 的服务 | 无（contact ID 类型不变） | — |

### 4.3 向后兼容

本次变更为**非兼容变更**：
- 响应格式从 `{code, message, data}`（Key 小写）变为 `ApiResponse`（字段名代码中保持一致）
- userId 从 `Integer` → `Long`，数据库字段 `user_contact.user_id` 需同步迁移
- 建议：接触用户端的 API 响应格式实际结构保持一致（同为 `code/message/data` 结构），前端无需改动

## 5. 实现顺序

| 步骤 | 内容 | 注意事项 |
|------|------|---------|
| 1 | common-api: 修复 ContactFeignClient | 先确保 Feign 接口正确 |
| 2 | contact-service: pom.xml 加依赖 | 引入 common-api |
| 3 | contact-service: userId 类型 `int` → `Long` | UserContact 系列全链路 |
| 4 | contact-service: 响应格式替换 | Controller + ExceptionHandler |
| 5 | contact-service: 清理本地 ApiResponse | 确认无其他引用 |
| 6 | contact-service: application.yml 补充 | MyBatis 日志 |
| 7 | 数据库变更 | user_contact.user_id → BIGINT |
| 8 | 编译验证 | mvn compile 全量通过 |

## 6. 回退方案

- Git 回退：`git revert` 本次提交即可
- 数据库：字段 `BIGINT` 向下兼容 `INT` 的值范围，数据无丢失风险
