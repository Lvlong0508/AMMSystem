# AI-Shopping 全量接口测试实施计划

> **For agentic workers:** 使用 superpowers:subagent-driven-development 或 superpowers:executing-plans 逐任务实现。步骤使用复选框 (`- [ ]`) 语法跟踪。

**目标:** 基于测试文档 (v1.1) 为 7 个微服务编写完整的自动化测试套件，覆盖 ~180+ API 测试用例。

**架构:** 每个服务独立 `@SpringBootTest` + `@AutoConfigureMockMvc`（Gateway 使用 `WebTestClient`）+ H2 内存数据库（MySQL 兼容模式）+ `@MockBean` 屏蔽 Feign/Redis 等外部依赖。并发测试使用 `CyclicBarrier` + 线程池模拟 CAS 竞争。

**Tech Stack:** Spring Boot 3.2.3, JUnit 5, Mockito 5.x, Spring Test, H2 (MySQL 模式), AssertJ

---

## 目录

1. [通用基础设施](#1-通用基础设施)
2. [Gateway 服务测试](#2-gateway-服务测试)
3. [Auth 服务测试](#3-auth-服务测试)
4. [Product 服务测试](#4-product-服务测试)
5. [Order 服务测试](#5-order-服务测试)
6. [Contact 服务测试](#6-contact-服务测试)
7. [Logistics 服务测试](#7-logistics-服务测试)
8. [Shop 服务测试](#8-shop-服务测试)
9. [文件创建汇总](#9-文件创建汇总)
10. [实施顺序建议](#10-实施顺序建议)

---

## 1. 通用基础设施

### 1.1 需要补充测试依赖的模块

以下 4 个模块需在 `pom.xml` 中添加依赖：

| 模块 | 需加依赖 |
|------|---------|
| `order-service` | `spring-boot-starter-test` (test), `h2` (test) |
| `contact-service` | `spring-boot-starter-test` (test), `h2` (test) |
| `logistics-service` | `spring-boot-starter-test` (test), `h2` (test) |
| `shop-service` | `spring-boot-starter-test` (test), `h2` (test) |

添加代码：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### 1.2 通用测试 Profile 模板

每个服务创建 `src/test/resources/application-test.yml`：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  autoconfigure:
    exclude:
      - org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration
      - org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration

mybatis:
  configuration:
    map-underscore-to-camel-case: true
```

### 1.3 Feign Client Mock 策略

- 所有 Feign 调用使用 `@MockBean` 注入 mock
- 返回类型统一用 `ApiResponse.success(data)` 包装
- 对返回 `Map<String, Object>` 的接口（ProductFeignClient），直接 `Map.of("success", true)` 构造
- 对 `batchGetShopInfo(Set<Long>)` 等批量接口，返回预构造的 Map

### 1.4 Redis Mock 策略

- `RedisAutoConfiguration` 在 test profile 中排除
- 对依赖 `StringRedisTemplate` 的服务（gateway, order），用 `@MockBean` 注入
- 对使用 `RedisConnectionFactory` 的 beans，在 `TestRedisConfig` 中提供 mock

```java
@TestConfiguration
public class TestRedisConfig {
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }
}
```

### 1.5 Sa-Token Mock 策略

- **Gateway 层**：mock `StringRedisTemplate.opsForValue().get()` 返回/不返回 loginId
- **Auth 服务层**：使用 `Mockito.mockStatic(StpUtil.class)` 在 `@BeforeEach` / `@AfterEach` 中控制
- **Controller 层**：使用 `@MockBean` mock Service 层，绕过 Sa-Token 调用

### 1.6 数据清理策略

使用 `@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)` 或 `@Sql(executionPhase = AFTER_TEST_METHOD)` 确保测试间数据隔离。

### 1.7 H2 兼容性说明

| MySQL 语法 | H2 兼容性 |
|-----------|----------|
| `AUTO_INCREMENT` | ✅ 支持 |
| `TIMESTAMP DEFAULT CURRENT_TIMESTAMP` | ✅ 支持 |
| `TINYINT` | ✅ 支持（映射为 byte） |
| `ON UPDATE CURRENT_TIMESTAMP` | ❌ 不支持——从 test schema 中移除 |
| `NOW() - INTERVAL 30 MINUTE` | ✅ 支持 |
| `UPDATE ... JOIN ... SET` | ✅ 支持 |
| `FOR UPDATE` | ✅ 支持行级锁 |
| `LIMIT x OFFSET y` | ✅ 支持 |

---

## 2. Gateway 服务测试

### 2.1 文件清单

```
gateway-service/src/test/
├── resources/
│   └── application-test.yml
└── java/com/gzasc/aishopping/gateway/
    ├── config/
    │   └── TestRedisConfig.java
    ├── filter/
    │   ├── SaTokenAuthGlobalFilterTest.java
    │   └── IpRateLimitFilterTest.java
    └── service/
        └── AuthServiceTest.java
```

### 2.2 测试配置

**application-test.yml（Gateway 特化）：**

```yaml
spring:
  cloud:
    gateway:
      enabled: true
    discovery:
      locator:
        enabled: false
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration

ip-rate-limit:
  max-requests: 5
  time-window-seconds: 60

auth:
  whitelist:
    paths:
      - /api/user/auth/login
      - /api/user/auth/register
      - /api/user/auth/check-username
      - /api/user/auth/check-phone
      - /api/seller/auth/login
      - /api/seller/auth/register
      - /api/seller/auth/check-username
      - /api/seller/auth/check-phone
      - /api/seller/shop/register
```

### 2.3 SaTokenAuthGlobalFilterTest.java

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class SaTokenAuthGlobalFilterTest {

    @Autowired private WebTestClient webTestClient;
    @Autowired private StringRedisTemplate stringRedisTemplate;
}
```

**路由转发测试（19 条）：**

| # | 方法 | 测试名称 | 验证点 |
|---|------|---------|--------|
| 1 | `GW-R01` | `userAuth_shouldBeForwarded` | GET `/api/user/auth/login` → 非 404 |
| 2 | `GW-R02` | `userProduct_shouldBeForwarded` | POST `/api/user/product/all` → 非 404 |
| 3 | `GW-R03` | `userOrder_shouldBeForwarded` | GET `/api/user/order/list` → 非 404 |
| 4 | `GW-R04` | `userContact_shouldBeForwarded` | POST `/api/user/contact/create` → 非 404 |
| 5 | `GW-R05` | `userLogistics_shouldBeForwarded` | GET `/api/user/logistics/list` → 非 404 |
| 6 | `GW-R06` | `userChat_shouldBeForwarded` | POST `/api/user/chat` → 非 404 |
| 7 | `GW-R06a` | `userChatSubPath_shouldBeForwarded` | GET `/api/user/chat/history?userId=1` → 非 404 |
| 8 | `GW-R07` | `userShop_shouldBeForwarded` | GET `/api/user/shop/list` → 非 404 |
| 9 | `GW-R08` | `sellerAuth_shouldBeForwarded` | GET `/api/seller/auth/login` → 非 404 |
| 10 | `GW-R09` | `sellerProduct_shouldBeForwarded` | POST `/api/seller/product/create` → 非 404 |
| 11 | `GW-R10` | `sellerOrder_shouldBeForwarded` | PUT `/api/seller/order/1/ship` → 非 404 |
| 12 | `GW-R11` | `sellerContact_shouldBeForwarded` | GET `/api/seller/contact/list` → 非 404 |
| 13 | `GW-R12` | `sellerAddress_shouldBeForwarded` | POST `/api/merchant/address/create` → 非 404 |
| 14 | `GW-R13` | `sellerLogistics_shouldBeForwarded` | GET `/api/seller/logistics/list` → 非 404 |
| 15 | `GW-R14` | `sellerChat_shouldBeForwarded` | POST `/api/seller/chat` → 非 404 |
| 16 | `GW-R14a` | `sellerChatSubPath_shouldBeForwarded` | GET `/api/seller/chat/history?shopId=1` → 非 404 |
| 17 | `GW-R15` | `sellerShop_shouldBeForwarded` | GET `/api/seller/shop/merchant/1` → 非 404 |
| 18 | `GW-R16` | `internalRoutes_stripPrefix` | POST `/internal/product/deduct-stock` → 非 404 |
| 19 | `GW-R17` | `internalLogistics_shouldBeForwarded` | GET `/internal/logistics/order/xxx/latest` → 非 404 |

**验证方式：** 后端服务不存在时 Gateway 返回 502，不等于 404 说明路由匹配成功。使用 `.expectStatus().value(status -> assertThat(status).isNotEqualTo(404))`。

**认证拦截测试：**

| # | 方法 | 测试场景 | Mock 设置 | 预期 |
|---|------|---------|----------|------|
| 20 | `GW-A01` | 无 Token | 无 | 401 |
| 21 | `GW-A02` | Token 为空 | `satoken: ""` | 401 |
| 22 | `GW-A03` | 有效 Token | `get("...")` → `"USER:1"` | 透传 |
| 23 | `GW-A04` | 过期 Token | `get(...)` → null | 401 |
| 24 | `GW-A05` | 伪造 Token | `get(...)` → null | 401 |

**角色权限测试：**

| # | 方法 | 测试场景 | Token 值 | Header | 预期 |
|---|------|---------|---------|--------|------|
| 25 | `GW-P01` | 用户访问商家 API | `USER:1` | - | 403 |
| 26 | `GW-P02` | 商家访问用户 API | `MERCHANT:1` | - | 403 |
| 27 | `GW-P03` | 用户访问用户 API | `USER:1` | - | 透传 |
| 28 | `GW-P04` | 商家访问商家 API | `MERCHANT:1` | - | 透传 |
| 29 | `GW-P05` | 店员操作管理 | `MERCHANT:2` | `X-Merchant-Role: 2` | 403 |
| 30 | `GW-P06` | 非店长操作管理 | `MERCHANT:2` | `X-Merchant-Role: 2, X-Shop-Id: 10` | 403 |
| 31 | `GW-P07` | 店长操作管理 | `MERCHANT:3` | `X-Merchant-Role: 1, X-Shop-Id: 10` | 透传 |
| 32 | `GW-P08` | 店长缺 Shop-Id | `MERCHANT:3` | `X-Merchant-Role: 1` | 403 |

**CORS 测试：**

| # | 方法 | 测试场景 | 预期 |
|---|------|---------|------|
| 33 | `GW-C01` | OPTIONS 跳过 auth | 不返回 401 |
| 34 | `GW-C02` | CORS 响应头 | `Access-Control-Allow-Origin: *` |

### 2.4 IpRateLimitFilterTest.java

| # | 方法 | 测试场景 | 预期 |
|---|------|---------|------|
| 35 | `GW-L01` | 正常请求（5 次） | 全部放行 |
| 36 | `GW-L02` | 超限请求（第 6 次） | 429 |
| 37 | `GW-L03` | 窗口重置 | 等待 1s 后正常 |
| 38 | `GW-L04` | 多 IP 独立计数 | 各自最多 5 次 |

### 2.5 AuthServiceTest.java

纯单元测试（`@ExtendWith(MockitoExtension.class)`），测试 `AuthServiceImpl`：

| # | 方法 | 测试名称 |
|---|------|---------|
| 39 | `isWhiteList_match` | 白名单路径匹配 |
| 40 | `isWhiteList_noMatch` | 非白名单路径 |
| 41 | `validateToken_valid` | 有效 token 返回 loginId |
| 42 | `validateToken_null` | null token 抛 401 |
| 43 | `validateToken_expired` | 过期 token 抛 401 |
| 44 | `extractRole_user` | USER 前缀 |
| 45 | `extractRole_merchant` | MERCHANT 前缀 |
| 46 | `extractRole_unknown` | 未知前缀 |
| 47 | `hasPermission_userToUser` | 用户→用户路径 true |
| 48 | `hasPermission_merchantToUser` | 商家→用户路径 false |
| 49 | `hasPermission_shopOwnerManage` | 店长→管理路径 true |
| 50 | `hasPermission_nonOwnerManage` | 非店长→管理路径 false |
| 51 | `isPreFlightRequest_options` | OPTIONS → true |
| 52 | `isPreFlightRequest_get` | GET → false |

---

## 3. Auth 服务测试

### 3.1 文件清单

```
auth-service/src/test/
├── resources/
│   ├── application-test.yml
│   └── schema.sql
└── java/com/gzasc/aishopping/auth/
    ├── config/
    │   ├── TestDataInitializer.java
    │   └── TestSaTokenConfig.java
    ├── controller/
    │   ├── UserAuthControllerTest.java
    │   ├── MerchantAuthControllerTest.java
    │   └── InternalControllerTest.java
    ├── service/
    │   ├── UserAuthServiceTest.java
    │   └── MerchantAuthServiceTest.java
    └── util/
        └── BCryptUtilTest.java
```

### 3.2 测试基础配置

**application-test.yml：** H2 + 排除 Redis/Eureka + 初始化 `schema.sql` + `TestDataInitializer` 运行时插入有效 BCrypt 哈希的测试用户。

### 3.3 UserAuthControllerTest.java

```java
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
```

所有测试方法通过 **mock Service 层** 绕过 Sa-Token：

| # | 方法 | 测试名称 | Stub 行为 | 断言 |
|---|------|---------|----------|------|
| 1 | `AU-R01` | `register_normal` | `register()` → LoginResult(token, user, "USER") | 200 + token |
| 2 | `AU-R02` | `register_duplicateUsername` | `register()` → `AuthException("用户名已存在")` | 400 |
| 3 | `AU-R03` | `register_emptyUsername` | 请求体 `username:""` | 400 校验 |
| 4 | `AU-R04` | `register_invalidChars` | `username:"@invalid"` | 400 校验 |
| 5 | `AU-R05` | `register_weakPassword` | `password:"123"` | 400 校验 |
| 6 | `AU-R06` | `register_emptyPassword` | `password:""` | 400 校验 |
| 7 | `AU-R07` | `register_withNickname` | 带 `nickname` 字段，mock info service | 200 + nickname |
| 8 | `AU-R08` | `register_duplicatePhone` | `register()` → `AuthException("手机号已被注册")` | 400 |
| 9 | `AU-L01` | `login_normal` | `login()` → LoginResult | 200 + accountType |
| 10 | `AU-L02` | `login_wrongPassword` | `login()` → `AuthException("用户名或密码错误")` | 400 |
| 11 | `AU-L03` | `login_nonExistent` | 同上（防枚举） | 400 |
| 12 | `AU-L04` | `login_emptyUsername` | `username:""` | 400 |
| 13 | `AU-L05` | `login_disabled` | `login()` → `AuthException("账号已被禁用")` | 400 |
| 14 | `AU-O01` | `logout` | `logout()` 无异常 | 200 |
| 15 | `AU-O02` | `checkUsername_available` | `existsByUsername()` → false | 200 + true |
| 16 | `AU-O03` | `checkUsername_unavailable` | `existsByUsername()` → true | 200 + false |
| 17 | `AU-O04` | `checkPhone_available` | `existsByPhone()` → false | 200 + true |
| 18 | `AU-O05` | `checkPhone_registered` | `existsByPhone()` → true | 200 + false |

### 3.4 MerchantAuthControllerTest.java

与 UserAuthControllerTest 结构一致，18 个用例，区别：
- `accountType` = `"MERCHANT"`
- 响应字段为 `merchantInfo` 而非 `userInfo`
- 注册/登录消息文案含"商家"前缀
- Mock `MerchantAuthService` 和 `MerchantInfoService`

### 3.5 InternalControllerTest.java

| # | 方法 | 测试名称 | Stub | 断言 |
|---|------|---------|------|------|
| 37 | `AU-I01` | `registerEmployee_normal` | `registerEmployee()` → 30001L | 200 + merchantId |
| 38 | `AU-I02` | `registerEmployee_customPassword` | 同上 | 200 |
| 39 | `AU-I03` | `registerEmployee_duplicate` | `registerEmployee()` → `AuthException` | 400 |

### 3.6 BCryptUtilTest.java

纯单元测试（11 个用例）：

| # | 测试方法 |
|---|---------|
| 40 | `hashPassword_returnsValidHash` — 以 `$2a$12$` 开头 |
| 41 | `verifyPassword_correct_returnsTrue` |
| 42 | `verifyPassword_wrong_returnsFalse` |
| 43 | `verifyPassword_nullPassword_returnsFalse` |
| 44 | `verifyPassword_nullHash_returnsFalse` |
| 45 | `isValidPasswordFormat_valid` |
| 46 | `isValidPasswordFormat_tooShort` |
| 47 | `isValidPasswordFormat_tooLong` |
| 48 | `isValidUsernameFormat_valid` |
| 49 | `isValidUsernameFormat_specialChars` |

### 3.7 UserAuthServiceTest.java / MerchantAuthServiceTest.java

纯单元测试（`@ExtendWith(MockitoExtension.class)` + `mockStatic(StpUtil.class)`）：

| 服务 | 用例数 | 覆盖点 |
|------|--------|--------|
| `UserAuthServiceTest` | 12 | register 正常/重复/手机号重复/带昵称, login 正常/密码错/不存/禁用, existsByUsername/Phone |
| `MerchantAuthServiceTest` | 14 | 同上 + registerEmployee 正常/自定义密码/默认密码/重复 |

---

## 4. Product 服务测试

### 4.1 文件清单

```
product-service/src/test/
├── resources/
│   ├── application-test.yml
│   └── schema.sql
└── java/com/gzasc/aishopping/product/
    ├── controller/
    │   ├── ProductUserControllerTest.java
    │   ├── ProductSellerControllerTest.java
    │   └── internal/
    │       └── InternalProductControllerTest.java
    └── concurrency/
        └── StockConcurrencyTest.java
```

### 4.2 测试配置

**schema.sql（Product 服务特有表）：**
```sql
CREATE TABLE IF NOT EXISTS product_images (
    id INT PRIMARY KEY AUTO_INCREMENT,
    url VARCHAR(500) NOT NULL
);
CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL, price DECIMAL(10,2) NOT NULL,
    tags VARCHAR(500), description TEXT,
    stock INT NOT NULL DEFAULT 0,
    is_sale TINYINT NOT NULL DEFAULT 1,
    image_id INT, shop_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS product_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL, order_id VARCHAR(50) NOT NULL UNIQUE,
    quantity INT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    created_at DATETIME NOT NULL DEFAULT NOW(), expired_at DATETIME NOT NULL
);
CREATE TABLE IF NOT EXISTS salable_products (
    id BIGINT PRIMARY KEY
);
```

**`@MockBean`：** `ShopFeignClient`（`getShopInfo`, `batchGetShopInfo`）

### 4.3 ProductUserControllerTest.java（10 用例）

| # | 方法 | 测试名称 | @BeforeEach 准备 | 断言 |
|---|------|---------|----------------|------|
| 1 | `PU-Q01` | `getAllProducts_emptyList` | 无可售商品 | 200 + data.products = [] |
| 2 | `PU-Q02` | `getAllProducts_pagination` | 2 个可售商品 + salable | 200 + data 含 2 条 |
| 3 | `PU-Q03` | `getProductById_exists` | 商品存在 | 200 + data.name |
| 4 | `PU-Q04` | `getProductById_notExists` | 商品不存在 | 404 "商品不存在" |
| 5 | `PU-Q05` | `searchProducts_match` | 名称含"手机"的商品 | 200 + 匹配 |
| 6 | `PU-Q06` | `searchProducts_noMatch` | 无匹配关键词 | 200 + data.total = 0 |
| 7 | `PU-Q07` | `priceRange_normal` | 价格区间 [10,100] | 200 |
| 8 | `PU-Q08` | `priceRange_zeroRange` | 价格=50 的唯一匹配 | 200 |
| 9 | `PU-Q09` | `delistedProduct_invisible` | 上架商品A → 下架 → 查询 /all | A 不在结果中 |
| 10 | `PU-Q10` | `defaultImage_whenNoImage` | imageId=0 的商品 | 返回默认图片 URL |

### 4.4 ProductSellerControllerTest.java（13 用例）

| # | 方法 | 测试名称 | 操作 | 断言 |
|---|------|---------|------|------|
| 11 | `PS-C01` | `createProduct_withImage` | POST create + imageUrl | 200 + DB 有 products / product_images |
| 12 | `PS-C02` | `createProduct_emptyName` | name="" | 400 校验 |
| 13 | `PS-C03` | `createProduct_invalidPrice` | price=-1 | 400 校验 |
| 14 | `PS-C04` | `createProduct_emptyImage` | imageUrl="" | 400 校验 |
| 15 | `PS-C05` | `updateProduct_partial` | PUT 只传 stock | 200 + DB stock 更新 |
| 16 | `PS-C06` | `updateProduct_newImage` | PUT 含新 imageUrl | 200 + DB images 更新 |
| 17 | `PS-C07` | `updateProduct_notExist` | PUT id=999999 | 404 |
| 18 | `PS-C08` | `deleteProduct_delisted` | 先下架 → DELETE | 200 + DB 删除 |
| 19 | `PS-C09` | `deleteProduct_listed` | 直接 DELETE 在售商品 | 400 "请先下架" |
| 20 | `PS-C10` | `listProduct` | POST /{id}/list | 200 + is_sale=true |
| 21 | `PS-C11` | `unlistProduct` | POST /{id}/unlist | 200 + is_sale=false |
| 22 | `PS-C12` | `listProduct_duplicate` | 两次 list | 第二次幂等 |
| 23 | `PS-C13` | `batchQuery` | GET /batch?ids=1,2 | 200 + 列表 |

### 4.5 InternalProductControllerTest.java（9 用例）

| # | 方法 | 测试名称 | 准备 | 断言 |
|---|------|---------|------|------|
| 24 | `PI-D01` | `deductStock_sufficient` | stock=10, qty=3 | success=true, stock=7 |
| 25 | `PI-D02` | `deductStock_insufficient` | stock=10, qty=20 | success=false, stock=10 |
| 26 | `PI-D03` | `restoreStock` | stock=10, qty=5 | success=true, stock=15 |
| 27 | `PI-R01` | `reserveStock_sufficient` | stock=100, qty=3 | success=true, DB RESERVED |
| 28 | `PI-R02` | `reserveStock_insufficient` | stock=0 | success=false |
| 29 | `PI-R03` | `confirmReservation` | RESERVED → 确认 | success=true, CONFIRMED, stock 扣减 |
| 30 | `PI-R04` | `releaseReservation` | RESERVED → 释放 | success=true, RELEASED |
| 31 | `PI-R05` | `releaseReservation_duplicate` | 已 RELEASED → 再次释放 | 幂等 |
| 32 | `PI-R06` | `reservation_expiredCleanup` | 过期 RESERVED → 清理 | RELEASED + stock 不变 |

### 4.6 StockConcurrencyTest.java（2 用例）

```
PC-C01: 20 线程并发 deductStock(productId, 5)，stock=10
        → 最多 2 个成功，最终 stock >= 0

PC-C02: 5 线程并发 reserve(productId, 3)，stock=10
        → 最多 3 个成功，预留总量 <= stock
```

**模式：** `CyclicBarrier(n)` + `ExecutorService(n)` + `CountDownLatch` + `AtomicInteger`

---

## 5. Order 服务测试

### 5.1 文件清单

```
order-service/src/test/
├── resources/
│   ├── application-test.yml
│   └── schema.sql
└── java/com/gzasc/aishopping/order/
    ├── config/
    │   ├── OrderTestBase.java
    │   └── TestRedisConfig.java
    ├── controller/
    │   ├── OrderUserControllerTest.java
    │   └── OrderSellerControllerTest.java
    └── concurrency/
        └── OrderConcurrencyTest.java
```

### 5.2 测试基础

**schema.sql（Order 特有表）：**
```sql
CREATE TABLE IF NOT EXISTS t_order (
    order_id VARCHAR(20) PRIMARY KEY,
    user_id BIGINT NOT NULL, shop_id VARCHAR(32) NOT NULL,
    product_id VARCHAR(64) NOT NULL, quantity INT NOT NULL DEFAULT 1,
    total_price DECIMAL(10,2) NOT NULL,
    order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    contact_id INT
);
CREATE TABLE IF NOT EXISTS deleted_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(20) NOT NULL, user_id BIGINT, shop_id VARCHAR(32),
    product_id VARCHAR(64) NOT NULL, quantity INT NOT NULL DEFAULT 1,
    total_price DECIMAL(10,2) NOT NULL,
    order_status VARCHAR(20) NOT NULL, order_date TIMESTAMP NOT NULL,
    contact_id INT, deleted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**`@MockBean`：**
- `ProductFeignClient`（`getProductById`, `reserveStock`, `confirmReservation`, `releaseReservation`, `restoreStock`）
- `LogisticsFeignClient`
- `ContactFeignClient`
- `StringRedisTemplate`（OrderIdSelector 依赖）
- `FileFallbackDaemon`（抑制 `afterCommit` 事件）

**`OrderTestBase` 抽象基类：**
```java
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@Import(TestRedisConfig.class)
public abstract class OrderTestBase {
    @Autowired protected MockMvc mockMvc;
    @Autowired protected OrderMapper orderMapper;
    @Autowired protected DeletedOrderMapper deletedOrderMapper;
    @MockBean protected ProductFeignClient productFeignClient;
    @MockBean protected LogisticsFeignClient logisticsFeignClient;
    @MockBean protected ContactFeignClient contactFeignClient;
    @MockBean protected StringRedisTemplate stringRedisTemplate;
    @MockBean protected FileFallbackDaemon fileFallbackDaemon;

    protected String insertOrderInDb(String status, Long userId, String shopId,
                                      String productId, int quantity, double price, Integer contactId);
    protected void mockProductGetPrice(double price, int stock, String shopId);
}
```

### 5.3 OrderUserControllerTest.java（28 用例）

**Place Order（6 用例）：**

| # | 方法 | Mock 准备 | 预期 |
|---|------|----------|------|
| 1 | `OU-P01` | product 存在, stock 充足 | 200 + PENDING |
| 2 | `OU-P02` | product 不存在 | 400 "商品不存在" |
| 3 | `OU-P03` | stock=1, qty=5 | 400 "库存不足" |
| 4 | `OU-P04` | productId=null | 400 "商品信息为空" |
| 5 | `OU-P05` | contactId=null | 400 "收货人信息为空" |
| 6 | `OU-P06` | quantity=0 | 400 "购买数量必须大于0" |

**Payment（4 用例）：**

| # | 方法 | 订单状态 | Mock | 预期 |
|---|------|---------|------|------|
| 7 | `OU-PAY01` | PENDING | confirmReservation → success | 200 + PAID + 验证事件发送 |
| 8 | `OU-PAY02` | PAID | - | 400 "订单状态异常" |
| 9 | `OU-PAY03` | CANCELLED | - | 400 |
| 10 | `OU-PAY04` | PENDING（并发支付） | CAS 模拟 | 一个成功一个失败 |

**Cancel（4 用例）：**

| # | 方法 | 订单状态 | Mock | 预期 |
|---|------|---------|------|------|
| 11 | `OU-C01` | PENDING | releaseReservation → success | 200 + CANCELLED |
| 12 | `OU-C02` | PAID | restoreStock → success | 200 + CANCELLED |
| 13 | `OU-C03` | SHIPPED | - | 400 "状态已变更" |
| 14 | `OU-C04` | PENDING 并发取消 | - | 一个成功，一个 CAS 失败 |

**Deliver/Return/Delete（8 用例）：**

| # | 方法 | 订单状态 | 预期 |
|---|------|---------|------|
| 15 | `OU-D01` | SHIPPED → DELIVERED | 200 |
| 16 | `OU-D02` | PAID → DELIVERED | 400 |
| 17 | `OU-R01` | SHIPPED → RETURN_PENDING | 200 |
| 18 | `OU-R02` | DELIVERED → RETURN_PENDING | 200 |
| 19 | `OU-R03` | CANCELLED → RETURN_PENDING | 400 |
| 20 | `OU-RE01` | DELIVERED → DELETED | 200 + deleted_orders 有备份 |
| 21 | `OU-RE02` | PENDING → DELETED | 400 "不允许删除" |
| 22 | `OU-XX` | SHIPPED → DELETED | 400 "不允许删除" |

**Query（6 用例）：**

| # | 方法 | 准备 | 预期 |
|---|------|------|------|
| 23 | `OU-L01` | 2 条 order(userId=1) + 1 条(order userId=2) | 200 + data.size=2 |
| 24 | `OU-L02` | Mock contactFeign + logisticsFeign | 200 + 完整详情 |
| 25 | `OU-L03` | order userId=2，header X-User-Id=1 | 400 "订单不存在" |
| 26 | `OS-L01` | 2 条 order(shopId="1") + 1 条(shopId="2") | 200 + data.size=2 |
| 27 | `OS-L02` | Mock contactFeign + logisticsFeign | 200 + 完整详情 |
| 28 | `OS-XX` | contactFeignClient 抛异常 | 200 + contactName=null（优雅降级） |

### 5.4 OrderSellerControllerTest.java（7 用例）

| # | 方法 | DB 状态 | Mock | 预期 |
|---|------|---------|------|------|
| 29 | `OS-S01` | PAID | logisticsFeign.getLatestLogistics → null | 200 + SHIPPED + 事件 |
| 30 | `OS-S02` | PENDING | - | 400 |
| 31 | `OS-S03` | PAID(shopId=2) | - | 400 |
| 32 | `OS-A01` | RETURN_PENDING | - | 200 + RETURNING |
| 33 | `OS-A02` | DELIVERED | - | 400 |
| 34 | `OS-CR01` | RETURNING | restoreStock → success | 200 + RETURNED + 事件 |
| 35 | `OS-CR02` | RETURN_PENDING | - | 400 |

### 5.5 OrderConcurrencyTest.java（5 用例）

| # | 方法 | 场景 | 模式 | 验证 |
|---|------|------|------|------|
| 36 | `O-CONC01` | 并发支付+取消(PENDING) | CyclicBarrier(2) | 一个成功，最终 PAID 或 CANCELLED |
| 37 | `O-CONC02` | 并发取消+发货(PAID) | CyclicBarrier(2) | 一个成功 |
| 38 | `O-CONC03` | 并发删除(CANCELLED) | 5 线程 | 唯一一个删除 |
| 39 | `O-TO01` | 超时自动取消 | 直接调 `OrderTimeoutTask.cancelExpiredOrders()` | PENDING→CANCELLED |
| 40 | `O-EV01` | Redis 不可用文件兜底 | opsForStream().add() 抛异常 | verify(fileFallbackDaemon).writeFallback() |

---

## 6. Contact 服务测试

### 6.1 文件清单

```
contact-service/src/test/
├── resources/
│   ├── application-test.yml
│   └── schema.sql
└── java/com/gzasc/aishopping/contact/
    └── controller/
        ├── UserContactControllerTest.java
        ├── MerchantContactControllerTest.java
        └── internal/
            └── InternalContactControllerTest.java
```

### 6.2 测试配置

**schema.sql（Contact 特有表）：**
```sql
CREATE TABLE IF NOT EXISTS t_contact (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL, phone VARCHAR(20) NOT NULL,
    address VARCHAR(500) NOT NULL,
    is_default TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS user_contact (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL, contact_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS shop_address (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL, phone VARCHAR(20) NOT NULL,
    address VARCHAR(500) NOT NULL,
    address_type TINYINT NOT NULL DEFAULT 1,
    is_default TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS shop_address_rel (
    id INT AUTO_INCREMENT PRIMARY KEY,
    shop_id VARCHAR(33) NOT NULL, address_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**无 `@MockBean`：** Contact 服务不依赖 Feign 调用。

### 6.3 UserContactControllerTest.java（12 用例）

| # | 方法 | 测试名称 | Header | 断言 |
|---|------|---------|--------|------|
| 1 | `UC-C01` | `createContact_withAuth` | `X-User-Id:1` | 200 + DB 有 t_contact + user_contact |
| 2 | `UC-C02` | `createContact_withoutAuth` | 无 | 401 |
| 3 | `UC-C03` | `createContact_missingName` | `X-User-Id:1` | 400 校验 |
| 4 | `UC-C04` | `getContacts_withAuth` | `X-User-Id:1` | 200 + 列表 |
| 5 | `UC-C05` | `getContacts_withoutAuth` | 无 | 401 |
| 6 | `UC-C06` | `updateContact_withAuth` | `X-User-Id:1` | 200 |
| 7 | `UC-C07` | `updateContact_unauthorized` | `X-User-Id:2`（非所有者） | 400 "地址不存在" |
| 8 | `UC-C08` | `deleteContact_withAuth` | `X-User-Id:1` | 200 + 关联删除 |
| 9 | `UC-C09` | `deleteContact_unauthorized` | `X-User-Id:2` | 400 |
| 10 | `UC-C10` | `setDefaultContact` | `X-User-Id:1` | 200 + is_default=1 |
| 11 | `UC-C11` | `setDefaultContact_nonExistent` | `X-User-Id:1` | 400 "设置失败" |
| 12 | **`UC-C12`** | `setDefaultContact_clearsPrevious` | `X-User-Id:1` | P1 **回归测试**：创建 A(默认)、B、设置 B 为默认 → A.is_default=0, B.is_default=1 |

### 6.4 MerchantContactControllerTest.java（10 用例）

| # | 方法 | 测试名称 | Header | 断言 |
|---|------|---------|--------|------|
| 13 | `SA-C01` | `createAddress_withAuth` | `X-Shop-Id:1` | 200 |
| 14 | `SA-C02` | `createAddress_isDefault` | `X-Shop-Id:1`, `isDefault:1` | 200 + 同类型其他清除 |
| 15 | `SA-C03` | `createAddress_withoutAuth` | 无 | 401 |
| 16 | `SA-C04` | `updateAddress_ownShop` | `X-Shop-Id:1` | 200 |
| 17 | `SA-C05` | `updateAddress_otherShop` | `X-Shop-Id:2` | 400 |
| 18 | `SA-C06` | `deleteAddress` | `X-Shop-Id:1` | 200 |
| 19 | `SA-C07` | `getAddressList` | `X-Shop-Id:1` | 200 + 列表 |
| 20 | `SA-C08` | `getDefaultShipAddress` | `X-Shop-Id:1` | 200 + type=1 + default |
| 21 | `SA-C09` | `setDefaultAddress_ownShop` | `X-Shop-Id:1` | 200 + 同类型其他清除 |
| 22 | `SA-C10` | `setDefaultAddress_otherShop` | `X-Shop-Id:2` | 400 |

### 6.5 InternalContactControllerTest.java（2 用例）

| # | 方法 | 测试名称 | 断言 |
|---|------|---------|------|
| 23 | `SI-G01` | `getContactById_exists` | 200 + Contact 对象 |
| 24 | `SI-G02` | `getContactById_notExists` | 400 "联系人不存在" |

---

## 7. Logistics 服务测试

### 7.1 文件清单

```
logistics-service/src/test/
├── resources/
│   ├── application-test.yml
│   └── schema.sql
└── java/com/gzasc/aishopping/logistics/
    └── controller/
        ├── LogisticsControllerTest.java
        └── internal/
            └── InternalLogisticsControllerTest.java
```

### 7.2 测试配置

**schema.sql：**
```sql
CREATE TABLE IF NOT EXISTS logistics (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY',
    contact_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tracking_number VARCHAR(50) NOT NULL
);
```

**无 `@MockBean`：** Logistics 服务不依赖 Feign 调用。

### 7.3 LogisticsControllerTest.java（11 用例）

| # | 方法 | 测试名称 | @BeforeEach | 断言 |
|---|------|---------|------------|------|
| 1 | `LG-C01` | `createLogistics_success` | - | 200 + data.id 非空 |
| 2 | `LG-C02` | `createLogistics_missingOrderId` | - | 400 |
| 3 | `LG-C03` | `getAllLogistics` | 插入 2 条 | 200 + size=2 |
| 4 | `LG-C04` | `searchByTrackingNumber` | 插入 tracking="SF123" | 200 + 匹配 |
| 5 | `LG-C05` | `searchByTrackingNumber_notFound` | - | 200 + data=null |
| 6 | `LG-C06` | `deleteLogistics_exists` | 插入 1 条 | 200 |
| 7 | `LG-C07` | `deleteLogistics_notExists` | - | 400 |
| 8 | `LG-C08` | `getByOrder` | 插入 2 条(order=O1)+1 条(O2) | 200 + size=2 |
| 9 | `LG-C09` | `getLatestByType_delivery` | 插入 2 条 DELIVERY | 返回最新一条 |
| 10 | `LG-C10` | `getLatestByType_return` | 插入 RETURN 类型 | 200 |
| 11 | `LG-C11` | `getLatest_nonExistent` | 无数据 | 400 "物流信息不存在" |

### 7.4 InternalLogisticsControllerTest.java（3 用例）

| # | 方法 | 测试名称 | 断言 |
|---|------|---------|------|
| 12 | `LI-C01` | `createLogisticsInternal_success` | 200 + LogisticsResponse |
| 13 | `LI-C02` | `getLogisticsByOrderInternal` | 200 |
| 14 | `LI-C03` | `getLatestLogisticsInternal` | 200 |

---

## 8. Shop 服务测试

### 8.1 文件清单

```
shop-service/src/test/
├── resources/
│   ├── application-test.yml
│   └── schema.sql
└── java/com/gzasc/aishopping/shop/
    └── controller/
        ├── ShopUserControllerTest.java
        ├── ShopMerchantControllerTest.java
        └── internal/
            └── InternalShopControllerTest.java
```

### 8.2 测试配置

**schema.sql：**
```sql
CREATE TABLE IF NOT EXISTS shop_info (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500), logourl VARCHAR(256),
    address VARCHAR(200), phone VARCHAR(20)
);
CREATE TABLE IF NOT EXISTS shops (
    id BIGINT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    shop_info_id BIGINT,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS merchant_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    role TINYINT NOT NULL DEFAULT 2,
    assigned_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_merchant_shop (merchant_id, shop_id)
);
```

**`@MockBean`：** `AuthFeignClient`（employee register 调用），`SnowflakeIdGenerator`（可选 mock）

### 8.3 ShopUserControllerTest.java（4 用例）

| # | 方法 | 测试名称 | Header | 断言 |
|---|------|---------|--------|------|
| 1 | `SU-L01` | `getShopList_withAuth` | `X-User-Id:1` | 200 + 分页 |
| 2 | `SU-L02` | `getShopList_withoutAuth` | 无 | 400 "请先登录" |
| 3 | `SU-D01` | `getShopDetail_active` | `X-User-Id:1` | 200 + shop + shopInfo |
| 4 | `SU-D02` | `getShopDetail_closed` | `X-User-Id:1`（status=0） | 400 "店铺不存在或已关闭" |

### 8.4 ShopMerchantControllerTest.java（15 用例）

**店铺管理（9 用例）：**

| # | 方法 | 测试名称 | Mock | 断言 |
|---|------|---------|------|------|
| 5 | `SM-C01` | `createShop_success` | - | 200 + DB shop_info, shops, merchant_roles |
| 6 | `SM-C02` | `createShop_emptyName` | - | 400 校验 |
| 7 | `SM-C03` | `getShopsByMerchant` | - | 200 + shopIds |
| 8 | `SM-C04` | `getShopDetail_withAccess` | - | 200 |
| 9 | `SM-C05` | `updateShop_owner` | role=1 | 200 |
| 10 | `SM-C06` | `updateShop_employee` | role=2 | 400 "仅店长可操作" |
| 11 | `SM-C07` | `closeShop_owner` | role=1 | 200 + status=0 |
| 12 | `SM-C08` | `reopenShop_owner` | role=1 | 200 + status=1 |
| 13 | `SM-C09` | `operateOtherShop` | 无 role | 400 "无权限" |

**员工管理（6 用例）：**

| # | 方法 | 测试名称 | Mock | 断言 |
|---|------|---------|------|------|
| 14 | `SM-E01` | `addEmployee_owner` | AuthFeignClient → {merchantId:20001} | 200 + DB merchant_role |
| 15 | `SM-E02` | `addEmployee_notOwner` | role=2 | 400 |
| 16 | `SM-E03` | `getEmployees` | 插入 2 个员工 | 200 + 列表 |
| 17 | `SM-E04` | `removeEmployee_owner` | role=1 | 200 |
| 18 | `SM-E05` | `addEmployee_duplicate` | 约束冲突 | 抛异常 |
| 19 | `SM-E06` | `removeEmployee_nonExistent` | - | 200（幂等） |

### 8.5 InternalShopControllerTest.java（4 用例）

| # | 方法 | 测试名称 | 断言 |
|---|------|---------|------|
| 20 | `SI-R01` | `getMerchantRoles_existent` | 200 + roles 列表 |
| 21 | `SI-R02` | `getMerchantRoles_nonExistent` | 200 + 空列表 |
| 22 | `SI-I01` | `getShopInfo_exists` | 200 + ShopInfoDTO |
| 23 | `SI-I02` | `batchGetShopInfo` | 200 + Map<id, ShopInfoDTO> |

---

## 9. 文件创建汇总

| 模块 | 新建/修改 | 路径 |
|------|----------|------|
| **order-service** | 修改 | `pom.xml` |
| **order-service** | 新建 | `src/test/resources/application-test.yml` |
| **order-service** | 新建 | `src/test/resources/schema.sql` |
| **order-service** | 新建 | `src/test/resources/clean.sql` |
| **order-service** | 新建 | `src/test/java/.../config/OrderTestBase.java` |
| **order-service** | 新建 | `src/test/java/.../config/TestRedisConfig.java` |
| **order-service** | 新建 | `src/test/java/.../controller/OrderUserControllerTest.java` |
| **order-service** | 新建 | `src/test/java/.../controller/OrderSellerControllerTest.java` |
| **order-service** | 新建 | `src/test/java/.../concurrency/OrderConcurrencyTest.java` |
| **contact-service** | 修改 | `pom.xml` |
| **contact-service** | 新建 | `src/test/resources/application-test.yml` |
| **contact-service** | 新建 | `src/test/resources/schema.sql` |
| **contact-service** | 新建 | `src/test/java/.../controller/UserContactControllerTest.java` |
| **contact-service** | 新建 | `src/test/java/.../controller/MerchantContactControllerTest.java` |
| **contact-service** | 新建 | `src/test/java/.../controller/internal/InternalContactControllerTest.java` |
| **logistics-service** | 修改 | `pom.xml` |
| **logistics-service** | 新建 | `src/test/resources/application-test.yml` |
| **logistics-service** | 新建 | `src/test/resources/schema.sql` |
| **logistics-service** | 新建 | `src/test/java/.../controller/LogisticsControllerTest.java` |
| **logistics-service** | 新建 | `src/test/java/.../controller/internal/InternalLogisticsControllerTest.java` |
| **shop-service** | 修改 | `pom.xml` |
| **shop-service** | 新建 | `src/test/resources/application-test.yml` |
| **shop-service** | 新建 | `src/test/resources/schema.sql` |
| **shop-service** | 新建 | `src/test/java/.../controller/ShopUserControllerTest.java` |
| **shop-service** | 新建 | `src/test/java/.../controller/ShopMerchantControllerTest.java` |
| **shop-service** | 新建 | `src/test/java/.../controller/internal/InternalShopControllerTest.java` |
| **product-service** | 新建 | `src/test/resources/application-test.yml` |
| **product-service** | 新建 | `src/test/resources/schema.sql` |
| **product-service** | 新建 | `src/test/java/.../controller/ProductUserControllerTest.java` |
| **product-service** | 新建 | `src/test/java/.../controller/ProductSellerControllerTest.java` |
| **product-service** | 新建 | `src/test/java/.../controller/internal/InternalProductControllerTest.java` |
| **product-service** | 新建 | `src/test/java/.../concurrency/StockConcurrencyTest.java` |
| **gateway-service** | 新建 | `src/test/resources/application-test.yml` |
| **gateway-service** | 新建 | `src/test/java/.../config/TestRedisConfig.java` |
| **gateway-service** | 新建 | `src/test/java/.../filter/SaTokenAuthGlobalFilterTest.java` |
| **gateway-service** | 新建 | `src/test/java/.../filter/IpRateLimitFilterTest.java` |
| **gateway-service** | 新建 | `src/test/java/.../service/AuthServiceTest.java` |
| **auth-service** | 新建 | `src/test/resources/application-test.yml` |
| **auth-service** | 新建 | `src/test/resources/schema.sql` |
| **auth-service** | 新建 | `src/test/java/.../config/TestDataInitializer.java` |
| **auth-service** | 新建 | `src/test/java/.../config/TestSaTokenConfig.java` |
| **auth-service** | 新建 | `src/test/java/.../controller/UserAuthControllerTest.java` |
| **auth-service** | 新建 | `src/test/java/.../controller/MerchantAuthControllerTest.java` |
| **auth-service** | 新建 | `src/test/java/.../controller/InternalControllerTest.java` |
| **auth-service** | 新建 | `src/test/java/.../service/UserAuthServiceTest.java` |
| **auth-service** | 新建 | `src/test/java/.../service/MerchantAuthServiceTest.java` |
| **auth-service** | 新建 | `src/test/java/.../util/BCryptUtilTest.java` |

---

## 10. 实施顺序建议

### Phase 1 — 基础设施（可并行）

```
Task 1: 4 个模块添加 test 依赖（order/contact/logistics/shop pom.xml）
Task 2: 创建各服务的 application-test.yml 和 schema.sql
Task 3: 创建各服务的 TestRedisConfig（gateway, order）
```

### Phase 2 — 无外部依赖的服务（可并行）

```
Task 4: Contact 服务测试（UserContactControllerTest + MerchantContactControllerTest + InternalContactControllerTest）
Task 5: Logistics 服务测试（LogisticsControllerTest + InternalLogisticsControllerTest）
Task 6: Auth BCryptUtilTest（纯单元测试，无 Spring 上下文）
```

### Phase 3 — 有 Feign Mock 的服务（可并行）

```
Task 7: Auth 服务 Controller 测试（UserAuthControllerTest + MerchantAuthControllerTest + InternalControllerTest）
Task 8: Auth 服务 Service 测试（UserAuthServiceTest + MerchantAuthServiceTest）
Task 9: Product 服务测试（ProductUserControllerTest + ProductSellerControllerTest + InternalProductControllerTest）
Task 10: Shop 服务测试（ShopUserControllerTest + ShopMerchantControllerTest + InternalShopControllerTest）
```

### Phase 4 — 复杂依赖的服务（可并行）

```
Task 11: Order 服务测试（OrderUserControllerTest + OrderSellerControllerTest）
Task 12: Gateway 服务测试（SaTokenAuthGlobalFilterTest + IpRateLimitFilterTest + AuthServiceTest）
```

### Phase 5 — 并发测试与收尾（可并行）

```
Task 13: Product 并发测试（StockConcurrencyTest）
Task 14: Order 并发测试（OrderConcurrencyTest）
Task 15: 全局编译验证（mvn test）
```
