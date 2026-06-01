# Logistics 服务两处 Bug 修复实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 logistics-service 的两个源码问题：createdAt 响应缺失 + order_id 缺少长度校验

**Architecture:** 两个修复互不依赖，可并行进行。Fix 1 修改 Mapper 层（追加 @SelectKey），Fix 2 修改 DTO 层（追加 @Size）

**Tech Stack:** Spring Boot 3.x, MyBatis, JUnit 5, MockMvc

---

### Task 1: Fix createdAt - 增加 @SelectKey 回读 created_at

**Files:**
- Modify: `logistics-service/src/main/java/.../mapper/LogisticsMapper.java:11-14`
- Test: `logistics-service/src/test/java/.../mapper/LogisticsMapperTest.java:31-39`

- [ ] **Step 1: 修改 insertLogistics 方法，追加 @SelectKey 注解**

在 `insertLogistics` 方法的 `@Options` 后追加 `@SelectKey`：

```java
@Insert("INSERT INTO logistics (order_id, type, contact_id, tracking_number) " +
        "VALUES (#{orderId}, #{type}, #{contactId}, #{trackingNumber})")
@Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
@SelectKey(statement = "SELECT created_at FROM logistics WHERE id = #{id}",
           keyProperty = "createdAt",
           before = false,
           resultType = Timestamp.class)
int insertLogistics(Logistics logistics);
```

同时需要在 imports 中确认已导入 `org.apache.ibatis.annotations.SelectKey`。

- [ ] **Step 2: 更新 Mapper 测试，验证 createdAt 被回填**

修改 `LogisticsMapperTest.java:37` 的 `insertLogistics_shouldReturnGeneratedId` 测试，追加 createdAt 断言：

```java
@Test
@DisplayName("插入物流记录并返回自增ID和创建时间")
void insertLogistics_shouldReturnGeneratedId() {
    Logistics logistics = buildLogistics(generateOrderId(), "DELIVERY", generateTrackingNumber(), 1);
    int affected = logisticsMapper.insertLogistics(logistics);

    assertThat(affected).isEqualTo(1);
    assertThat(logistics.getId()).isNotNull();
    assertThat(logistics.getCreatedAt()).isNotNull();
}
```

- [ ] **Step 3: 运行 Mapper 测试验证**

Run: `mvn test -pl logistics-service -Dtest=LogisticsMapperTest -DfailIfNoTests=false`
Expected: 所有测试通过，createdAt 不再为 null

- [ ] **Step 4: Commit**

```bash
git add logistics-service/src/main/java/.../mapper/LogisticsMapper.java
git add logistics-service/src/test/java/.../mapper/LogisticsMapperTest.java
git commit -m "fix: insertLogistics 新增 @SelectKey 回读 created_at"
```

### Task 2: Fix order_id 长度校验

**Files:**
- Modify: `logistics-service/src/main/java/.../dto/CreateLogisticsRequest.java:9-10`
- Test: `logistics-service/src/test/java/.../controller/LogisticsControllerTest.java`

- [ ] **Step 1: 在 CreateLogisticsRequest.orderId 上追加 @Size 注解**

```java
@NotBlank(message = "订单号不能为空")
@Size(max = 20, message = "订单号长度不能超过20个字符")
private String orderId;
```

同时确认导入 `jakarta.validation.constraints.Size`。

- [ ] **Step 2: 在 ControllerTest 中添加 orderId 超长校验测试**

在 `LogisticsControllerTest.java` 中 `createLogistics_orderIdNull_validationError` 测试之后追加：

```java
@Test
@DisplayName("LG-003b 创建物流记录 - orderId超过20字符")
void createLogistics_orderIdTooLong_validationError() throws Exception {
    mockMvc.perform(post("/logistics/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"orderId":"ABCDEFGHIJKLMNOPQRSTUVWXYZ","type":"DELIVERY","contactId":1,"trackingNumber":"SF123"}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
}
```

- [ ] **Step 3: 运行 Controller 测试验证**

Run: `mvn test -pl logistics-service -Dtest=LogisticsControllerTest -DfailIfNoTests=false`
Expected: 所有测试通过，超长 orderId 返回 400

- [ ] **Step 4: Commit**

```bash
git add logistics-service/src/main/java/.../dto/CreateLogisticsRequest.java
git add logistics-service/src/test/java/.../controller/LogisticsControllerTest.java
git commit -m "fix: CreateLogisticsRequest 新增 @Size(max=20) 校验"
```
