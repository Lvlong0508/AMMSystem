# RedisOrderIdGenerator.generate() 防御 null 修复方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `RedisOrderIdGenerator.generate()` 在 Redis `INCR` 返回 `null` 时改为快速失败（抛 `IllegalStateException`），不再把字符串 `"null"` 拼进订单号。

**Architecture:** 在 `increment` 调用后立即判空，null 时抛 `IllegalStateException` 并附上 key 与上下文信息，便于排障。这是 fail-fast 原则：序列化失败 ID 进入数据库/Redis Stream 后再回滚成本远大于在源头拒绝。

**Tech Stack:** Spring Boot 3.x + JUnit 5 + Mockito

---

## 问题诊断

`RedisOrderIdGenerator.java:22-26`：
```java
Long sequence = redisTemplate.opsForValue().increment(key);   // 可能返回 null
if (sequence != null && sequence == 1) {
    redisTemplate.expire(key, 24, TimeUnit.HOURS);
}
String seqStr = String.format("%05d", sequence);              // null → 字符串 "null"
```

`String.format("%05d", null)` 的实际行为是返回字符串 `"null"`（5 字符后是 "null"），而不是 `String.format` 模板本身抛 `NullPointerException`。因此 OID-01 测试观察到 `orderId.contains("null") == true`，订单号会被污染（如 `20260602000nullABCDE`）并写入 `order` 表与 Redis Stream，导致下游解析异常且难以排查。

### 触发条件

`StringRedisTemplate.opsForValue().increment(key)` 在以下情况会返回 `null`：
1. Redis 服务不可达（连接被拒绝、超时）
2. Pipeline/Transaction 在某些边角场景下未正确返回结果
3. Spring Data Redis 的反序列化层对 `Long` 失败时静默返回 null

`increment` 实际抛异常的场景（连接错误）由 `RedisConnectionFailureException` 抛出，不在本缺陷范围。

---

## 修复策略

**选择 fail-fast**：

```java
if (sequence == null) {
    throw new IllegalStateException(
        "Redis INCR 返回 null，无法生成订单号。请检查 Redis 连接状态。key=" + key);
}
```

为什么不选其它：
- **返回 null**：调用方 `OrderIdSelector.generate()` 不知道怎么办，会把 null 向上传递，最终 NPE 在更深层抛出，定位更困难。
- **兜底 0**：与合法 `INCR 0` 冲突，破坏单调性。
- **重试 N 次**：增加复杂度，且若 Redis 真挂了，重试也救不回来。
- **使用 `Objects.requireNonNullElse(sequence, 1L)`**：静默吃错，可能把 key 重置为 1，破坏唯一性。

`IllegalStateException` 比 `NullPointerException` 更有意义——明确告诉调用者这是"环境状态"问题，不是代码 bug。

---

## 改动清单

| 文件 | 改动 |
|------|------|
| `order-service/.../id/RedisOrderIdGenerator.java` | L22-26 加 null 校验并抛 `IllegalStateException` |
| `order-service/.../id/RedisOrderIdGeneratorTest.java` | OID-01 由"记录现状"改为"验证抛 IllegalStateException" |

---

## 任务分解

### Task 1: 修复 `RedisOrderIdGenerator.java`

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/id/RedisOrderIdGenerator.java:22-26`

- [ ] **Step 1: 添加 null 校验**

把第 22 行（`Long sequence = ...`）到第 23 行（`if (sequence != null && sequence == 1)`）之间的逻辑改为：

```java
Long sequence = redisTemplate.opsForValue().increment(key);
if (sequence == null) {
    throw new IllegalStateException(
            "Redis INCR 返回 null，无法生成订单号。请检查 Redis 连接状态。key=" + key);
}
if (sequence == 1) {
    redisTemplate.expire(key, 24, TimeUnit.HOURS);
}
```

注意：`increment == null` 已经被排除，所以 `if (sequence == 1)` 不再需要再判空，可以简化。原 line 23 的 `sequence != null &&` 守卫随之移除。

- [ ] **Step 2: 验证编译**

```bash
mvn clean compile -pl order-service -am -q
```

预期：`BUILD SUCCESS`。

---

### Task 2: 更新 OID-01 测试

**Files:**
- Modify: `order-service/src/test/java/com/gzasc/aishopping/order/id/RedisOrderIdGeneratorTest.java`

- [ ] **Step 1: 替换 OID-01 测试方法**

把现有方法 `generate_sequenceNull` 整体替换为：

```java
@Test
@DisplayName("OID-01 increment 返回 null 时抛 IllegalStateException 含 key 信息")
void generate_sequenceNull() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.increment(anyString())).thenReturn(null);

    RedisOrderIdGenerator gen = new RedisOrderIdGenerator(redisTemplate);
    IllegalStateException ex = assertThrows(IllegalStateException.class, gen::generate);

    String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String expectedKey = "order:seq:" + currentDate;
    assertTrue(ex.getMessage().contains(expectedKey),
            "异常信息应包含 Redis key，便于排障: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("INCR"),
            "异常信息应说明是 INCR 操作: " + ex.getMessage());
}
```

> 旧测试断言 `orderId.contains("null")` 是"记录 bug 现状"用的，修复后应被替换为"验证正确抛异常"。

- [ ] **Step 2: 运行 RedisOrderIdGeneratorTest**

```bash
mvn test -pl order-service -Dtest='RedisOrderIdGeneratorTest' 2>&1 | Select-String -Pattern "Tests run|BUILD|FAIL"
```

预期：8/8 通过，OID-01 改抛 `IllegalStateException` 而非返回污染字符串。

---

### Task 3: 端到端回归

- [ ] **Step 1: 跑全部 order-service 单元测试**

```bash
mvn test -pl order-service -Dtest='!*MapperTest' 2>&1 | Select-String -Pattern "Tests run:|BUILD" | Select-Object -First 5
```

预期：非 mapper 测试 175+ 通过（基线 175 = 201 - 26 mapper 集成）。

- [ ] **Step 2: 跑 mapper 集成测试**

```bash
mvn test -pl order-service -Dtest='OrderMapperTest,DeletedOrderMapperTest' 2>&1 | Select-String -Pattern "Tests run:|BUILD" | Select-Object -First 5
```

预期：26/26 通过。

---

## 注意事项

- **不影响正常路径**：仅 `increment == null` 时改变行为。正常 Redis 响应非 null 时行为完全一致（format、expire、随机后缀都不变）。
- **下游 `OrderIdSelector.generate()` 仍会冒泡这个异常**——`OrderServiceImpl.createOrder()` 也没有 catch 它，事务回滚，符合 fail-fast 预期。
- **监控建议**（不在本修复范围）：该异常出现意味着 Redis 不可用，应触发告警。可后续在 `OrderIdSelector` 或调用方加 `@Retryable` / 告警埋点。

---

## 不变的部分

- `OrderIdGenerator` 接口签名不变
- `OrderIdSelector` 不变
- `RedisOrderIdGenerator.generate_format`、`generate_increment`、`generate_newKeyFirstSequence`、`generate_subsequentNoExpire` 4 个原有测试不变
- 业务调用方（`OrderServiceImpl.createOrder`）不变
