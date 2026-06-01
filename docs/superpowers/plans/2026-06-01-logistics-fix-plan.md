# Logistics 服务源码问题修复方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 Logistics 服务 API 集成测试中发现的 4 个源码问题

**Architecture:** logistics-service 模块的 DTO 校验增强、Controller 一致性对齐、Service 层事务处理优化、构建依赖清理。涉及 4 个文件修改，无新增文件，无 API 行为变更。

**Tech Stack:** Spring Boot 3, MyBatis, Jakarta Validation, Maven

---

### Task 1: 清理未使用的 H2 测试依赖

**Files:**
- Modify: `AI-Shopping-backend_Eureka/logistics-service/pom.xml:54-58`

**问题：** `pom.xml` 中声明了 `h2` 的 test scope 依赖，但测试配置 `src/test/resources/application.yml` 使用 MySQL 而非 H2，`LogisticsMapperTest` 也通过 `@AutoConfigureTestDatabase(replace = Replace.NONE)` 强制使用 MySQL。H2 依赖完全未被使用。

- [ ] **Step 1: 删除 pom.xml 中的 H2 依赖块**

删除 `pom.xml` 中第 54-58 行：

```xml
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
```

删除后 `pom.xml` 的 dependencies 区域结束于 `spring-boot-starter-test`。

- [ ] **Step 2: 验证构建通过**

```bash
cd AI-Shopping-backend_Eureka
mvn clean compile -pl logistics-service -am -DskipTests
```

Expected: BUILD SUCCESS（无 H2 依赖不应影响编译）

- [ ] **Step 3: 运行全量测试验证**

```bash
mvn clean test -pl logistics-service -am
```

Expected: 61/61 测试通过

- [ ] **Step 4: 提交**

```bash
git add AI-Shopping-backend_Eureka/logistics-service/pom.xml
git commit -m "chore: 移除未使用的 H2 测试依赖\n\nMapperTest 使用 @AutoConfigureTestDatabase(replace = NONE) 强制使用 MySQL，\nH2 依赖从未被加载，删除以减少构建混淆。"
```

---

### Task 2: 修复 type 字段空字符串绕过校验的问题

**Files:**
- Modify: `AI-Shopping-backend_Eureka/logistics-service/src/main/java/com/gzasc/aishopping/logistics/converter/LogisticsConverter.java:15,24`
- Modify: `AI-Shopping-backend_Eureka/logistics-service/src/main/java/com/gzasc/aishopping/logistics/dto/CreateLogisticsRequest.java:14`
- Test: `AI-Shopping-backend_Eureka/logistics-service/src/test/java/com/gzasc/aishopping/logistics/controller/LogisticsControllerTest.java`
- Test: `AI-Shopping-backend_Eureka/logistics-service/src/test/java/com/gzasc/aishopping/logistics/service/impl/LogisticsServiceImplTest.java`

**问题：** `CreateLogisticsRequest.type` 字段无校验注解（第 14 行 `private String type`），空字符串 `""` 可绕过 `@Valid` 校验。`LogisticsConverter.toModel()` 中只处理了 null 情况（`type != null ? type : "DELIVERY"`），空字符串按原值存入数据库。

**修复策略：** 不改动 DTO 校验（type 应保持可选，null→DELIVERY），而是在 `LogisticsConverter` 中将空字符串也兜底为 `"DELIVERY"`。同时补充测试覆盖空字符串场景。

#### Step 1: 修改 LogisticsConverter 处理空字符串

修改 `LogisticsConverter.java:15`（CreateLogisticsRequest 转模型）：

```java
// before
logistics.setType(request.getType() != null ? request.getType() : "DELIVERY");
// after
logistics.setType(org.springframework.util.StringUtils.hasText(request.getType()) ? request.getType() : "DELIVERY");
```

修改 `LogisticsConverter.java:24`（LogisticsRequest 转模型）：

```java
// before
logistics.setType(request.getType() != null ? request.getType() : "DELIVERY");
// after
logistics.setType(org.springframework.util.StringUtils.hasText(request.getType()) ? request.getType() : "DELIVERY");
```

需在 `LogisticsConverter.java` 添加 import：

```java
import org.springframework.util.StringUtils;
```

- [ ] **Step 1: 修改 LogisticsConverter.java 第 15 行和第 24 行**

文件：`AI-Shopping-backend_Eureka/logistics-service/src/main/java/com/gzasc/aishopping/logistics/converter/LogisticsConverter.java`

- [ ] **Step 2: 编译验证**

```bash
cd AI-Shopping-backend_Eureka
mvn clean compile -pl logistics-service -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 在 LogisticsControllerTest 中补充空字符串测试**

在 `LogisticsControllerTest.java` 中添加测试方法，放在 `createLogistics_invalidType_passesToService` 测试附近（约第 335-349 行）：

```java
@Test
@DisplayName("LG-005a type为空字符串 - 应兜底为DELIVERY")
void createLogistics_emptyType_defaultsToDelivery() throws Exception {
    LogisticsResponse response = LogisticsResponse.builder()
            .id(6).orderId("ORD-EMPTY-TYPE").type("DELIVERY").trackingNumber("SF-EMPTY").build();

    when(logisticsService.createLogistics(any(CreateLogisticsRequest.class))).thenReturn(response);

    mockMvc.perform(post("/logistics/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"orderId":"ORD-EMPTY-TYPE","type":"","contactId":1,"trackingNumber":"SF-EMPTY"}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.type").value("DELIVERY"));
}
```

- [ ] **Step 4: 在 LogisticsServiceImplTest 中补充空字符串服务层测试**

在 `LogisticsServiceImplTest.java` 中添加测试方法，放在 `createLogistics_withInvalidType_success` 测试附近（约第 343-367 行）：

```java
@Test
@DisplayName("LG-005a type为空字符串 - 转换器兜底为DELIVERY")
void createLogistics_withEmptyType_defaultsToDelivery() {
    CreateLogisticsRequest request = new CreateLogisticsRequest();
    request.setOrderId("ORD-EMPTY-TYPE");
    request.setType("");
    request.setContactId(1);
    request.setTrackingNumber("SF-EMPTY");

    Logistics logistics = new Logistics();
    logistics.setOrderId("ORD-EMPTY-TYPE");
    logistics.setType("DELIVERY");
    logistics.setContactId(1);
    logistics.setTrackingNumber("SF-EMPTY");

    LogisticsResponse response = LogisticsResponse.builder()
            .id(6).orderId("ORD-EMPTY-TYPE").type("DELIVERY")
            .trackingNumber("SF-EMPTY").build();

    when(logisticsConverter.toModel(request)).thenReturn(logistics);
    when(logisticsMapper.insertLogistics(logistics)).thenReturn(1);
    when(logisticsConverter.toResponse(logistics)).thenReturn(response);

    LogisticsResponse result = logisticsService.createLogistics(request);

    assertEquals("DELIVERY", result.getType());
}
```

- [ ] **Step 5: 运行全量测试验证**

```bash
mvn clean test -pl logistics-service -am
```

Expected: 63/63 测试通过（原 61 + 新增 2）

- [ ] **Step 6: 提交**

```bash
git add AI-Shopping-backend_Eureka/logistics-service/src/main/java/com/gzasc/aishopping/logistics/converter/LogisticsConverter.java
git add AI-Shopping-backend_Eureka/logistics-service/src/test/java/com/gzasc/aishopping/logistics/controller/LogisticsControllerTest.java
git add AI-Shopping-backend_Eureka/logistics-service/src/test/java/com/gzasc/aishopping/logistics/service/impl/LogisticsServiceImplTest.java
git commit -m "fix: type空字符串绕过校验，转换器兜底为DELIVERY\n\nLogisticsConverter 对空字符串也做 DELIVERY 默认值处理，\n避免空字符串直接入库。补充 Controller 和 Service 层测试覆盖。"
```

---

### Task 3: 修复 @Transactional 自调用代理失效问题

**Files:**
- Modify: `AI-Shopping-backend_Eureka/logistics-service/src/main/java/com/gzasc/aishopping/logistics/service/impl/LogisticsServiceImpl.java:24-39`

**问题：** `createLogistics(CreateLogisticsRequest)`（第 24-29 行）自调用 `this.createLogistics(Logistics)`（第 33-39 行）。Spring AOP 代理无法拦截同类内的方法自调用，导致内层方法的 `@Transactional` 不生效。虽然外层已有 `@Transactional` 保护，当从 `InternalLogisticsController` 直接调用 `createLogistics(Logistics)` 时事务仍正常，但自调用场景下内层的 `@Transactional` 形同虚设，是潜在 bug。

**修复策略：** 将核心插入逻辑抽取为 `private` 方法，两个 `public` 方法各自持有 `@Transactional` 并调用该私有方法，避免自调用。

- [ ] **Step 1: 重构 LogisticsServiceImpl**

将 `createLogistics(Logistics)` 的方法体抽取为私有方法，两个 `public` 方法分别调用它：

```java
@Override
@Transactional
public LogisticsResponse createLogistics(CreateLogisticsRequest request) {
    Logistics logistics = logisticsConverter.toModel(request);
    return doCreateLogistics(logistics);
}

@Override
@Transactional
public LogisticsResponse createLogistics(Logistics logistics) {
    return doCreateLogistics(logistics);
}

private LogisticsResponse doCreateLogistics(Logistics logistics) {
    int result = logisticsMapper.insertLogistics(logistics);
    if (result <= 0) {
        throw new LogisticsException("创建物流信息失败");
    }
    return logisticsConverter.toResponse(logistics);
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile -pl logistics-service -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 运行全量测试验证**

```bash
mvn clean test -pl logistics-service -am
```

Expected: 63/63 测试通过（行为未变）

- [ ] **Step 4: 提交**

```bash
git add AI-Shopping-backend_Eureka/logistics-service/src/main/java/com/gzasc/aishopping/logistics/service/impl/LogisticsServiceImpl.java
git commit -m "fix: 修复 @Transactional 自调用代理失效\n\n抽取 doCreateLogistics 私有方法，两个 public 方法各自持有\n@Transactional 注解，避免 AOP 自调用导致事务不生效的问题。"
```

---

### Task 4: InternalLogisticsController 增加 @Valid 校验

**Files:**
- Modify: `AI-Shopping-backend_Eureka/logistics-service/src/main/java/com/gzasc/aishopping/logistics/controller/InternalLogisticsController.java:23`
- Test: `AI-Shopping-backend_Eureka/logistics-service/src/test/java/com/gzasc/aishopping/logistics/controller/InternalLogisticsControllerTest.java`

**问题：** `InternalLogisticsController.createLogistics()` 的 `@RequestBody` 参数缺少 `@Valid` 注解，与外部 `LogisticsController` 的 `@RequestBody @Valid` 模式不一致。虽然 `LogisticsRequest` DTO 当前没有校验注解（它是一个跨服务共享的 POJO），但缺失 `@Valid` 意味着未来即使新增校验注解也不会生效。

**修复策略：** 在 `@RequestBody` 后添加 `@Valid`。当前 `LogisticsRequest` 无校验注解，所以添加 `@Valid` 后行为不变，但保持了与外部 Controller 一致的校验模式。

- [ ] **Step 1: 修改 InternalLogisticsController.java:23**

修改 `InternalLogisticsController.java`：

```java
// before (第 23 行)
public ApiResponse<LogisticsResponse> createLogistics(@RequestBody LogisticsRequest request) {
// after
public ApiResponse<LogisticsResponse> createLogistics(@RequestBody @Valid LogisticsRequest request) {
```

新增 import：

```java
import jakarta.validation.Valid;
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile -pl logistics-service -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 运行全量测试验证**

```bash
mvn clean test -pl logistics-service -am
```

Expected: 63/63 测试通过

- [ ] **Step 4: 提交**

```bash
git add AI-Shopping-backend_Eureka/logistics-service/src/main/java/com/gzasc/aishopping/logistics/controller/InternalLogisticsController.java
git commit -m "refactor: InternalLogisticsController 补充 @Valid 注解\n\n与外部 LogisticsController 的校验模式保持一致，\n确保未来 LogisticsRequest 增加校验注解时能生效。"
```

---

### 验证 Checklist

| 检查项 | 预期 |
|--------|------|
| `pom.xml` 无 H2 依赖 | `mvn dependency:tree -pl logistics-service` 无 h2 |
| 空字符串 type 入库为 DELIVERY | HTTP POST `{"type":""}` 返回 `data.type="DELIVERY"` |
| `@Transactional` 自调用消除 | 源码无 `this.createLogistics(logistics)` 自调用 |
| `@Valid` 一致性 | InternalLogisticsController 方法签名含 `@Valid` |
| 全量测试 63/63 通过 | `mvn clean test -pl logistics-service -am` |
| API 集成测试不变 | 全量 27 个场景 HTTP 测试行为一致 |
