# Product-Service 对齐 Auth-Service 设计方案

## 背景

product-service（商品服务）与 auth-service（认证服务）属于同一微服务群，但两套代码在响应格式、异常体系、DTO 风格、配置等方面存在不一致。本设计文档定义 product-service 向 auth-service 对齐的完整方案。

## 范围

仅修改 product-service，仅参考 auth-service 的模式。common-api 模块需要配合改动（ApiResponse 迁移）。

## 变更清单

### 1. ApiResponse 迁移到 common-api

**原因**：auth-service 使用 `Map<String, Object>` 返回统一格式（code/message/data）。保留 `ApiResponse<T>` 的泛型优势，但将其提取到公共模块统一管理，让所有服务都可以引用。

- **操作**：将 product-service 的 `ApiResponse.java` 移到 `common-api` 模块，包路径 `com.gzasc.aishopping.common.response.ApiResponse`
- **product-service 引用**：改 import 指向 common-api
- **涉及文件**：
  - 新建：`common-api/.../response/ApiResponse.java`
  - 删除：`product-service/.../common/ApiResponse.java`
  - 修改（改 import）：`GlobalExceptionHandler.java`, `ProductSellerController.java`, `ProductUserController.java`

### 2. 异常体系简化（4 类 → 1 类）

**原因**：auth-service 只有 1 个 `AuthException`，职责清晰。product-service 的 4 个异常类中，`ProductServiceException` 仅作为中间层，`ProductNotFoundException`/`ProductOnSaleException` 只是消息不同的壳。

- **操作**：
  - 保留 `ProductException`（带 `code` + `message` 字段，保留 `getCode()` 方法）
  - 删除 `ProductServiceException.java`, `ProductNotFoundException.java`, `ProductOnSaleException.java`
  - 将所有 throws/throw 引用改为 `ProductException`
- **涉及文件**：
  - 删除 3 个异常文件
  - 修改：`ProductServiceImpl.java`, `ProductSellerController.java`, `ProductUserController.java`, `GlobalExceptionHandler.java`

### 3. DTO 统一为 @Data（Lombok）

**原因**：auth-service 所有 DTO 统一使用 `@Data`。product-service 部分 DTO 手写 getter/setter，风格不统一。

**涉及 DTO（4 个）**：

| DTO | 当前 | 改为 |
|-----|------|------|
| `CreateProductRequest.java` | 手写 getter/setter + `jakarta.validation` | `@Data` + 保留注解 |
| `UpdateProductRequest.java` | 手写 getter/setter | `@Data` |
| `StockRequest.java` | 手写 getter/setter + `jakarta.validation` | `@Data` + 保留注解 |
| `ListProductRequest.java` | 手写 getter/setter | `@Data` |

### 4. 内联 DTO 抽取为独立文件

**原因**：`InternalProductController` 内部定义了 `StockDeductRequest` 静态内部类，破坏了 DTO 的独立性。

- **操作**：抽取为独立文件 `dto/StockDeductRequest.java`（与 `common-api` 中的 `StockDeductRequest` 不同，这个用于 product-service 内部接口参数）
- **涉及**：`InternalProductController.java` + 新建 `StockDeductRequest.java`

### 5. application.yml 补齐

对齐 auth-service 的配置风格：

```yaml
mybatis:
  configuration:
    map-underscore-to-camel-case: true
    default-executor-type: simple     # 新增
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 新增
```

### 6. pom.xml 补齐

| 依赖 | 操作 |
|------|------|
| `HikariCP` | 新增显式声明（当前由 spring-boot-starter-jdbc 隐式引入） |
| `spring-boot-starter-test` (test scope) | 新增 |
| `h2` (test scope) | 新增 |

### 7. GlobalExceptionHandler 精简

- `ProductServiceException` handler → 删除（异常已删除）
- 使用 common-api 的 `ApiResponse`（import 变更）

### 8. Converter 简化

保留 `ProductConverter`，精简冗余方法：

- 保留核心转换：`toAbstractWithImageDTO`, `toDetailWithImageDTO`, `toAbstractWithImageDTOList`, `toDetailWithImageDTOList`
- 删除未使用的方法（在 Service 接口中没有对应的调用链的方法）

### 9. Controller import 更新

三个 controller 需要将 `ApiResponse` 的 import 从本地包改为 common-api。

## 不变项

| 模块 | 说明 |
|------|------|
| `ProductCache` | 保留，后续用于 Redis 接入 |
| `ProductConverter` | 保留（精简后） |
| Service 接口+实现模式 | 与 auth 一致，不变 |
| Mapper 注解风格 | 与 auth 一致，不变 |
| Controller 的 `@RequiredArgsConstructor` | 与 auth 一致，不变 |
| `@Transactional` 使用 | 与 auth 一致，不变 |
| SnowflakeIdGenerator 使用 | 不变 |
| 默认图片资源 | 不变 |

## 变更文件清单

### 新增文件
1. `common-api/src/main/java/com/gzasc/aishopping/common/response/ApiResponse.java`
2. `product-service/src/main/java/com/gzasc/aishopping/product/dto/StockDeductRequest.java`

### 删除文件
1. `product-service/.../common/ApiResponse.java`
2. `product-service/.../exception/ProductServiceException.java`
3. `product-service/.../exception/ProductNotFoundException.java`
4. `product-service/.../exception/ProductOnSaleException.java`

### 修改文件
1. `product-service/pom.xml`
2. `product-service/src/main/resources/application.yml`
3. `product-service/.../controller/GlobalExceptionHandler.java`
4. `product-service/.../controller/ProductSellerController.java`
5. `product-service/.../controller/ProductUserController.java`
6. `product-service/.../controller/internal/InternalProductController.java`
7. `product-service/.../service/impl/ProductServiceImpl.java`
8. `product-service/.../converter/ProductConverter.java`
9. `product-service/.../dto/CreateProductRequest.java`
10. `product-service/.../dto/UpdateProductRequest.java`
11. `product-service/.../dto/StockRequest.java`
12. `product-service/.../dto/ListProductRequest.java`

## 影响分析

| 维度 | 影响 |
|------|------|
| API 契约 | **无变化** — 返回的字段结构相同（code/message/data） |
| 引入方 | **无影响** — 仅 import 路径变化，返回数据的 JSON 结构不变 |
| 编译 | 需先编译 common-api（新增文件），再编译 product-service |
| 测试 | 无测试，不需处理 |
