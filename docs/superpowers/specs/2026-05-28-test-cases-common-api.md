# common-api 测试用例文档

> 版本: 1.0 | 日期: 2026-05-28

## 1. 概述

本文档覆盖 `common-api` 模块的全部可测试单元，包括：`ApiResponse` 通用响应封装、`SnowflakeIdGenerator` ID 生成器、11 个 DTO 的序列化/反序列化，以及 8 个 Feign 接口的端点和方法签名校验。该模块为纯公共模块，无启动类、无 Controller，被所有微服务引用。

**模块路径**: `AI-Shopping-backend_Eureka/common-api/`
**基包**: `com.gzasc.aishopping.common`

---

## 2. 测试用例表

### 2.1 ApiResponse 通用响应

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-001 | success(data) 返回 200 | 无 | 调用 `ApiResponse.success("hello")` | code=200, message="操作成功", data="hello" | P0 |
| CM-002 | success(message, data) 自定义消息 | 无 | 调用 `ApiResponse.success("自定义消息", 100)` | code=200, message="自定义消息", data=100 | P0 |
| CM-003 | success(null data) | 无 | 调用 `ApiResponse.success(null)` | code=200, message="操作成功", data=null | P1 |
| CM-004 | error(code, message) 自定义错误码 | 无 | 调用 `ApiResponse.error(401, "未授权")` | code=401, message="未授权", data=null | P0 |
| CM-005 | error(message) 默认 500 | 无 | 调用 `ApiResponse.error("服务异常")` | code=500, message="服务异常", data=null | P0 |
| CM-006 | 泛型类型擦除下成功构造复杂类型 | 无 | 调用 `ApiResponse.success(List.of("a","b"))` | code=200, data 为 List, 长度 2 | P1 |
| CM-007 | 默认无参构造创建空对象 | 无 | `new ApiResponse<>()` | code=0, message=null, data=null | P2 |
| CM-008 | 全参构造赋值正确 | 无 | `new ApiResponse<>(201, "创建成功", obj)` | 三个字段与构造参数一致 | P1 |
| CM-009 | 序列化与反序列化 | 无 | 对象→JSON→对象 | 字段值完全一致, 类型保留 | P1 |
| CM-010 | Serializable 接口校验 | 无 | 反射检查 `ApiResponse` | 实现 `java.io.Serializable` 接口 | P1 |

### 2.2 SnowflakeIdGenerator

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-011 | nextId() 返回正数 long | 无 | 调用 `SnowflakeIdGenerator.nextId()` | 返回值 > 0, 类型为 long | P0 |
| CM-012 | nextIdStr() 返回数字字符串 | 无 | 调用 `SnowflakeIdGenerator.nextIdStr()` | 返回值可被 `Long.parseLong()` 正确解析 | P0 |
| CM-013 | 连续生成 ID 不重复 | 无 | 生成 10000 个 ID 放入 Set | Set.size() == 10000 | P0 |
| CM-014 | 连续 ID 递增趋势 | 无 | 生成 100 个 ID, 校验是否递增 | 后一个 ID > 前一个 ID | P1 |
| CM-015 | nextId() 和 nextIdStr() 对应相同值 | 无 | 连续调用两次, 转为 String 比较 | nextIdStr() 不早于 nextId() 对应的字符串值 | P2 |
| CM-016 | 多线程并发生成无重复 | 无 | 10 线程各生成 1000 个 ID 收集入 Set | Set.size() == 10000 | P1 |
| CM-017 | Hutool workerId/datacenterId 配置正确 | 无 | 反射读取 Snowflake 实例构造参数 | workerId=1, datacenterId=1 | P2 |

### 2.3 DTO 序列化/反序列化

#### 2.3.1 ContactDTO

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-018 | 全字段 set/get | 无 | set 所有字段后 get | 各字段值一致 | P0 |
| CM-019 | Jackson 序列化 JSON | 无 | 对象转 JSON | JSON 包含: id, name, phone, address, createdAt, updatedAt<br/>⚠️ 依赖要求: 需在 common-api/pom.xml 中添加 jackson-datatype-jsr310 依赖，否则 LocalDateTime 序列化/反序列化会失败。 | P1 |
| CM-020 | Jackson 反序列化 | 无 | JSON 转对象 | 各字段值恢复正确<br/>⚠️ 依赖要求: 需在 common-api/pom.xml 中添加 jackson-datatype-jsr310 依赖，否则 LocalDateTime 反序列化会失败。 | P1 |
| CM-021 | LocalDateTime 时间格式序列化 | 对象含有 createdAt | 序列化含时间的 ContactDTO | JSON 中 createdAt 为合法 ISO 时间字符串<br/>⚠️ 依赖要求: 需在 common-api/pom.xml 中添加 jackson-datatype-jsr310 依赖，否则 LocalDateTime 序列化会失败。 | P1 |
| CM-022 | id=0 默认值 | 无构造赋值 | new ContactDTO() | id=0 (int 默认值) | P2 |
| CM-023 | Serializable 接口校验 | 无 | 反射检查 | 实现 `Serializable` | P2 |

#### 2.3.2 LogisticsRequest

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-024 | 全参构造 | 无 | `LogisticsRequest("O001","DELIVERY",1,"SF123")` | 各字段正确赋值 | P0 |
| CM-025 | 无参构造 + setter | 无 | new + set 全部字段 | get 时值与 set 一致 | P0 |
| CM-026 | type 字段任意字符串 | 无 | type="RETURN" / "DELIVERY" / "REFUND" | 正常赋值, 无枚举校验 | P1 |
| CM-027 | contactId 为 null | 无 | contactId=null | 允许 null, 无 NPE | P1 |
| CM-028 | Jackson 序列化/反序列化 | 无 | JSON 双向转换 | 字段值一致 | P1 |
| CM-029 | Serializable 接口校验 | 无 | 反射检查 | 实现 `Serializable` | P2 |

#### 2.3.3 OrderDTO

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-030 | 全字段赋值 | 无 | 设置全部 8 个字段 | get 值与设置一致 | P0 |
| CM-031 | 状态常量值 | 无 | 校验常量 | PENDING=PAID=CANCELLED=SHIPPED=DELIVERED=RETURNED 六常量非空 | P1 |
| CM-032 | orderDate Timestamp 类型 | 无 | set orderDate 为当前时间 | get 返回相同 Timestamp | P1 |
| CM-033 | Jackson 反序列化含 Timestamp 的 JSON | 无 | JSON 含 `"orderDate":"2026-05-28T10:00:00"` | 反序列化为 Timestamp 对象（反序列化正常；序列化时 java.sql.Timestamp 默认输出 epoch 毫秒而非 ISO 字符串） | P1 |
| CM-034 | contactId 为 null 边界 | 无 | contactId=null | 不抛异常 | P1 |
| CM-035 | 枚举状态字符串不合法 | 无 | orderStatus="INVALID" | 不校验, 可任意赋值 | P2 |
| CM-036 | Serializable 接口校验 | 无 | 反射检查 | 实现 `Serializable` | P2 |

#### 2.3.4 OrderAbstractSellerDTO

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-037 | 全字段赋值 | 无 | 设置 5 个字段 | get 一致 | P0 |
| CM-038 | contactId 为 null | 无 | contactId=null | 不抛异常 | P1 |
| CM-039 | Jackson 序列化/反序列化 | 无 | JSON 双向转换 | 字段值一致 | P1 |
| CM-040 | Serializable 接口校验 | 无 | 反射检查 | 实现 `Serializable` | P2 |

#### 2.3.5 ShipOrderRequest

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-041 | 全字段赋值 | 无 | 设置 4 个字段 | get 一致 | P0 |
| CM-042 | shippingDate 字符串格式灵活 | 无 | shippingDate="2026-05-28" 或 "2026/05/28" | 无格式校验, 任意字符串 | P1 |
| CM-043 | contactId 为 null | 无 | contactId=null | 不抛异常 | P1 |
| CM-044 | Jackson 序列化/反序列化 | 无 | JSON 双向转换 | 字段值一致 | P1 |

#### 2.3.6 ProductDTO

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-045 | 全参构造 | 无 | new ProductDTO(id,name,price,tags,desc,stock,shopId,created,updated) | 各字段正确 | P0 |
| CM-046 | 无参构造 + setter | 无 | new + set 全部字段 | get 一致 | P0 |
| CM-047 | price 精度 | 无 | price=99.99 | 无精度损失 | P1 |
| CM-048 | stock 负数边界 | 无 | stock=-1 | 无校验, 可赋值 | P1 |
| CM-049 | Jackson 序列化/反序列化 | 无 | JSON 含 Date 类型字段 | 双向转换成功 | P1 |
| CM-050 | tags 逗号分隔字符串 | 无 | tags="tag1,tag2" | 无额外处理, 原样存储 | P2 |
| CM-051 | Serializable 接口校验 | 无 | 反射检查 | 实现 `Serializable` | P2 |

#### 2.3.7 StockDeductRequest

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-052 | 全参构造 | 无 | new StockDeductRequest("P001", 5) | productId="P001", quantity=5 | P0 |
| CM-053 | 无参构造 + setter | 无 | new + set | get 一致 | P0 |
| CM-054 | quantity 为 0 边界 | 无 | quantity=0 | 无校验, 可赋值 | P1 |
| CM-055 | quantity 为负值 | 无 | quantity=-10 | 无校验, 可赋值 | P1 |
| CM-056 | Jackson 序列化/反序列化 | 无 | JSON 双向转换 | 字段一致 | P1 |
| CM-057 | Serializable 接口校验 | 无 | 反射检查 | 实现 `Serializable` | P2 |

#### 2.3.8 StockReserveRequest

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-058 | 全参构造 | 无 | new StockReserveRequest("O001","P001",3) | 各字段正确 | P0 |
| CM-059 | 无参构造 + setter | 无 | new + set | get 一致 | P0 |
| CM-060 | Jackson 序列化/反序列化 | 无 | JSON 双向转换 | 字段一致 | P1 |
| CM-061 | Serializable 接口校验 | 无 | 反射检查 | 实现 `Serializable` | P2 |

#### 2.3.9 MerchantRoleDTO

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-062 | 全参构造 | 无 | new MerchantRoleDTO(id,merchantId,shopId,role,assignedBy,createdAt) | 各字段正确 | P0 |
| CM-063 | 无参构造 + setter | 无 | new + set | get 一致 | P0 |
| CM-064 | role 字段任意字符串 | 无 | role="OWNER"/"STAFF"/"xxx" | 无枚举校验 | P1 |
| CM-065 | Jackson 序列化/反序列化 | 无 | JSON 含 Date 字段 | 双向转换成功 | P1 |
| CM-066 | createdAt 为 null | 无 | createdAt=null | 不抛异常 | P2 |

#### 2.3.10 ShopDTO

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-067 | 全参构造 | 无 | new ShopDTO(...) 8 个参数 | 各字段正确 | P0 |
| CM-068 | 无参构造 + setter | 无 | new + set 全部字段 | get 一致 | P0 |
| CM-069 | status 字段含义 | 无 | status=0/1/2 | 无枚举校验, 任意 Integer | P1 |
| CM-070 | status 为 null | 无 | status=null | 不抛异常 | P1 |
| CM-071 | Jackson 序列化/反序列化 | 无 | JSON 双向转换 | 字段一致 | P1 |

#### 2.3.11 ShopInfoDTO

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-072 | 全参构造 | 无 | new ShopInfoDTO(id,name,desc,logoUrl) | 各字段正确 | P0 |
| CM-073 | 无参构造 + setter | 无 | new + set | get 一致 | P0 |
| CM-074 | @JsonProperty("logourl") 序列化 | 对象 logoUrl="http://img/1.png" | 序列化 JSON | JSON 中键为 `logourl`, 非 `logoUrl` | P1 |
| CM-075 | @JsonProperty("logourl") 反序列化 | JSON `{"logourl":"http://img/1.png"}` | 反序列化 | getLogoUrl() 返回正确 URL | P1 |
| CM-076 | @JsonProperty 与 setter 兼容 | JSON 同时含 `logoUrl` 和 `logourl` | 反序列化 | 以 `logourl` 为准 | P1 |

### 2.4 Feign 接口定义

#### 2.4.1 AuthFeignClient

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-077 | FeignClient name 与 contextId | 无 | 校验注解 | name="auth-service", contextId="authFeignClient" | P0 |
| CM-078 | POST 路径与请求体 | 无 | 校验方法签名 | POST `/internal/auth/register-employee`, 参数 `@RequestBody Map` | P0 |
| CM-079 | 返回类型 | 无 | 校验返回类型 | `Map<String, Object>` | P1 |

#### 2.4.2 UserInfoFeignClient

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-080 | FeignClient 配置 | 无 | 校验注解 | name="auth-service", contextId="userInfoFeignClient", path="/internal/userinfo" | P0 |
| CM-081 | POST 路径拼接 | 无 | 校验 path+@PostMapping | 完整路径为 `/internal/userinfo/create` | P0 |
| CM-082 | 泛型参数严格度 | 无 | 校验参数类型 | `Map<String, String>`, 非宽松 `Map` | P1 |
| CM-083 | 两个 Auth Feign 不冲突 | 无 | contextId 不同 | contextId 分别为 "authFeignClient" 与 "userInfoFeignClient" | P1 |

#### 2.4.3 ContactFeignClient

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-084 | FeignClient 配置 | 无 | 校验注解 | name="contact-service", 无 contextId | P0 |
| CM-085 | GET 路径与 PathVariable | 无 | 校验方法 | GET `/internal/contact/{id}`, `@PathVariable("id") Integer id` | P0 |
| CM-086 | id 参数类型 | 无 | Integer 传 null | 路径上 Integer 传 null 会报错, 需注意非空 | P1 |

#### 2.4.4 LogisticsFeignClient

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-087 | createLogistics 返回 ApiResponse | 无 | 校验方法 | POST, 返回 `ApiResponse<Map<String, Object>>` | P0 |
| CM-088 | getLogisticsByOrder 返回列表 | 无 | 校验方法 | GET, 返回 `ApiResponse<List<Map<String, Object>>>` | P0 |
| CM-089 | getLatestLogistics 带查询参数 | 无 | 校验方法 | GET, 含 `@RequestParam("type")` | P1 |
| CM-090 | 三个方法路径前缀一致 | 无 | 校验路径 | 均以 `/internal/logistics/` 开头 | P1 |

#### 2.4.5 OrderFeignClient

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-091 | getOrderById 带 Header | 无 | 校验方法 | GET, `@RequestHeader("X-User-Id")` | P0 |
| CM-092 | getAllOrders 无参数 | 无 | 校验方法 | GET, 仅 `@RequestHeader`, 无请求体 | P0 |
| CM-093 | 返回类型 Object | 无 | 校验返回类型 | 均为 `Object` (非泛型), 调用方需自行转换 | P1 |

#### 2.4.6 ProductFeignClient

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-094 | 12 个方法总数 | 无 | 统计方法数 | 恰好 12 个方法 | P0 |
| CM-095 | 外部查询路径 /api/user/product | 无 | 检查 getAllProducts, getProductByIdExternal | 路径为 `/api/user/product/...` | P0 |
| CM-096 | 内部查询路径 /internal/product | 无 | 检查其余方法 | 路径为 `/internal/product/...` | P0 |
| CM-097 | 库存操作 POST 路径 | 无 | 检查 deduct/restore/reserve/confirm/release | 5 个库存方法 POST 路径正确 | P0 |
| CM-098 | createProduct 与 getProductsByShopId 返回 ApiResponse | 无 | 校验返回类型 | `ApiResponse<Map>` / `ApiResponse<List<Map>>` | P1 |
| CM-099 | 其余方法返回 Map | 无 | 校验返回类型 | `Map<String, Object>` | P1 |
| CM-100 | getProductsByShopId 分页参数 | 无 | 校验 | page, size 两个 `@RequestParam` | P1 |

#### 2.4.7 ShopFeignClient

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-101 | getShopInfo 返回 ShopInfoDTO | 无 | 校验方法 | `ApiResponse<ShopInfoDTO>` | P0 |
| CM-102 | batchGetShopInfo 批量查询 | 无 | 校验方法 | POST, `@RequestBody Set<Long>`, 返回 `ApiResponse<Map<Long, ShopInfoDTO>>` | P0 |
| CM-103 | getMerchantRoles 独立路径 | 无 | 校验方法 | GET `/internal/shop/employees/roles/{merchantId}`, 参数 Long | P0 |
| CM-104 | FeignClient 名称统一 | 无 | 校验 name | "shop-service" | P1 |

#### 2.4.8 ShopFeignClientForRoles

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CM-105 | 独立 contextId | 无 | 校验注解 | contextId="shopFeignClientForRoles" | P0 |
| CM-106 | 与 ShopFeignClient 路径不冲突 | 无 | 检查路径 | 相同 name + 不同 contextId, 共存无冲突 | P0 |
| CM-107 | 返回类型 Map | 无 | 校验返回类型 | `Map<String, Object>` | P1 |

---

## 3. 测试要点总结

### 3.1 优先级分布

| 优先级 | 数量 | 占比 |
|--------|------|------|
| P0 | 30 | 28.0% |
| P1 | 54 | 50.5% |
| P2 | 23 | 21.5% |
| **合计** | **107** | **100%** |

### 3.2 按模块分布

| 模块 | 用例数 | P0 |
|------|--------|----|
| ApiResponse | 10 | 3 |
| SnowflakeIdGenerator | 7 | 3 |
| ContactDTO | 6 | 1 |
| LogisticsRequest | 6 | 2 |
| OrderDTO | 7 | 2 |
| OrderAbstractSellerDTO | 4 | 1 |
| ShipOrderRequest | 4 | 1 |
| ProductDTO | 7 | 2 |
| StockDeductRequest | 6 | 2 |
| StockReserveRequest | 4 | 1 |
| MerchantRoleDTO | 5 | 1 |
| ShopDTO | 5 | 2 |
| ShopInfoDTO | 5 | 1 |
| AuthFeignClient | 3 | 2 |
| UserInfoFeignClient | 4 | 2 |
| ContactFeignClient | 3 | 2 |
| LogisticsFeignClient | 4 | 2 |
| OrderFeignClient | 3 | 2 |
| ProductFeignClient | 7 | 4 |
| ShopFeignClient | 4 | 3 |
| ShopFeignClientForRoles | 3 | 2 |

### 3.3 关键测试关注点

1. **ApiResponse** — 核心响应封装，所有服务均依赖，4 个静态工厂方法和序列化是 P0 级测试。
2. **SnowflakeIdGenerator** — 分布式 ID 生成，唯一性（含多线程）是测试重点。
3. **DTO** — 共 11 个 DTO，测试重点是：全参/无参构造、Jackson 序列化/反序列化、`Serializable` 接口实现。
4. **Feign 接口** — 8 个 Client，测试重点是：`@FeignClient` 注解的 name/contextId 配置、HTTP 方法与路径、参数注解和返回类型。需特别注意同一 service name 下不同 contextId 的隔离。
5. **特殊 JSON 映射** — `ShopInfoDTO.logoUrl` 使用 `@JsonProperty("logourl")`，需验证序列化和反序列化行为。
6. **无校验设计** — ShipOrderRequest 不实现 Serializable、StockDeductRequest.quantity 无负数校验等，均为有意设计，测试应验证无校验约束。

### 3.4 测试技术方案建议

| 类型 | 框架/工具 | 说明 |
|------|-----------|------|
| 单元测试 | JUnit 5 + AssertJ | ApiResponse、SnowflakeIdGenerator、DTO 构造/getter/setter |
| JSON 序列化 | Jackson ObjectMapper | DTO 的 toJson/fromJson 测试 |
| Feign 接口验证 | 编译期注解检查 + 反射 | 校验注解元数据与方法签名 |
| 参数化测试 | JUnit 5 @ParameterizedTest | 批量验证 DTO 字段 |
| 多线程测试 | CountDownLatch + ConcurrentHashMap | SnowflakeIdGenerator 并发唯一性 |
