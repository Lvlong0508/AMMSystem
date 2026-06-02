# totalPrice 字段 double→BigDecimal 改造实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `Order.totalPrice`/`DeletedOrder.totalPrice`/`OrderDTO.totalPrice`/`chat-service OrderItem.totalPrice` 全部从 `double`/`Double` 改造为 `BigDecimal`，消除编译错误与精度风险。

**Architecture:** 模型层（order-service）与 DTO 层（common-api/chat-service）共同采用 `BigDecimal`；Service 层用 `BigDecimal.multiply` 替代 `double` 算术；Converter 层去掉冗余的 `BigDecimal.valueOf(double)` 包装；MyBatis 的 `DECIMAL ↔ BigDecimal` 是天然 JDBC 映射，mapper XML 无需修改。

**Tech Stack:** Spring Boot 3.x + MyBatis + Lombok + JUnit 5 + Mockito + Maven

**项目根目录:** `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka`

---

## 预分析结果（与 spec 差异点）

1. **spec 未提及的额外问题**：`OrderServiceImplTest.java` 中 5 处 `new ProductDTO(1L, "Test", 50.0, null, null, 10, 100L, null, null)` 仅传 9 个参数，但 common-api `ProductDTO` 现有 10 个字段（含 `imageUrl`），**当前就编译失败**。本计划在按 spec 把 `50.0` 改为 `BigDecimal.valueOf(50)` 的同时，**补一个尾随 `null` 参数**（即第 10 个位置 `imageUrl = null`），让调用变成 10 参。

2. **当前工作区已存在的临时补丁**：`OrderServiceImpl.java:54` 已经被改写为 `product.getPrice() != null ? product.getPrice().doubleValue() : 0.0`，这是一个临时绕过编译错误的写法。Task 3 会彻底替换为 `BigDecimal` 算术。

3. **规范与文件实际行号已对齐**：spec 标注的 L79/L104/L148/L166/L187/L206/L37/L38/L62/L103/L104/L118/L66/L124/L137/L190 与实际文件内容完全一致。

---

## 文件结构与任务分解

| 层 | 文件 | 操作 |
|----|------|------|
| 1 模型 | `order-service/.../model/Order.java` | 修改 |
| 1 模型 | `order-service/.../model/DeletedOrder.java` | 修改 |
| 2 DTO | `common-api/.../dto/order/OrderDTO.java` | 修改 |
| 2 DTO | `chat-service/.../dto/OrderItem.java` | 修改 |
| 3 Service | `order-service/.../service/impl/OrderServiceImpl.java` | 修改 |
| 4 Converter | `order-service/.../converter/OrderConverter.java` | 修改 |
| 5 测试 | `order-service/.../test/.../OrderServiceImplTest.java` | 修改 |
| 5 测试 | `order-service/.../test/.../mapper/OrderMapperTest.java` | 修改 |
| 5 测试 | `order-service/.../test/.../mapper/DeletedOrderMapperTest.java` | 修改 |
| 5 测试 | `order-service/.../test/.../stream/OrderEventConsumerTest.java` | 修改 |
| 5 测试 | `chat-service/.../test/.../controller/ChatControllerTest.java` | 修改 |
| 5 测试 | `chat-service/.../test/.../dto/DtoSerializationTest.java` | 修改 |

---

### Task 1: 改造模型层 `Order.java`

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/model/Order.java`

- [ ] **Step 1: 添加 import 并改字段类型**

将文件第 1-7 行（import 区）后追加 `import java.math.BigDecimal;`，并把第 46 行 `private double totalPrice;` 改为 `private BigDecimal totalPrice;`。

- [ ] **Step 2: 修改 `buildInitOrder` 静态工厂方法签名**

把第 64-65 行的 `buildInitOrder(..., double totalPrice)` 改为 `buildInitOrder(..., BigDecimal totalPrice)`。第 72 行 `order.totalPrice = totalPrice;` 不变（Lombok @Data 自动生成 setter 接受 `BigDecimal`）。

- [ ] **Step 3: 验证模型层编译**

```bash
mvn clean compile -pl order-service -am -q
```

预期：`BUILD SUCCESS`。

---

### Task 2: 改造模型层 `DeletedOrder.java`

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/model/DeletedOrder.java`

- [ ] **Step 1: 添加 import 并改字段类型**

在 import 区追加 `import java.math.BigDecimal;`，把第 18 行 `private double totalPrice;` 改为 `private BigDecimal totalPrice;`。

- [ ] **Step 2: 验证模型层编译**

```bash
mvn clean compile -pl order-service -am -q
```

预期：`BUILD SUCCESS`。`fromOrder` 方法体内的 `setTotalPrice(order.getTotalPrice())` 仍然合法（Order 已是 BigDecimal）。

---

### Task 3: 改造 DTO 层 `OrderDTO.java` (common-api)

**Files:**
- Modify: `common-api/src/main/java/com/gzasc/aishopping/common/dto/order/OrderDTO.java`

- [ ] **Step 1: 添加 import 并改字段类型**

在 import 区追加 `import java.math.BigDecimal;`，把第 24 行 `private double totalPrice;` 改为 `private BigDecimal totalPrice;`。

- [ ] **Step 2: 重新打包 common-api 并验证编译**

```bash
mvn clean install -pl common-api -DskipTests -q
mvn clean compile -pl order-service,chat-service -am -q
```

预期：`BUILD SUCCESS`。

---

### Task 4: 改造 DTO 层 `OrderItem.java` (chat-service)

**Files:**
- Modify: `chat-service/src/main/java/com/gzasc/aishopping/chat/dto/OrderItem.java`

- [ ] **Step 1: 改 record 字段类型**

在文件第 1 行后追加 `import java.math.BigDecimal;`，把第 7 行 `Double totalPrice,` 改为 `BigDecimal totalPrice,`。

- [ ] **Step 2: 验证 chat-service 主代码编译**

```bash
mvn clean compile -pl chat-service -am -q
```

预期：`BUILD SUCCESS`。测试代码会失败，留到 Task 9 修复。

---

### Task 5: 改造 Service 层 `OrderServiceImpl.java`

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java`

- [ ] **Step 1: 添加 import `BigDecimal`**

在 import 区追加 `import java.math.BigDecimal;`（import 顺序按字母排在 `import java.util.*;` 之前）。

- [ ] **Step 2: 替换 `createOrder` 中的 double 算术**

把第 54 行和第 68-69 行从：

```java
double price = product.getPrice() != null ? product.getPrice().doubleValue() : 0.0;
...
Order order = Order.buildInitOrder(orderId, userId, shopId, request.getProductId(),
        request.getQuantity(), price * request.getQuantity());
```

改为：

```java
BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
...
Order order = Order.buildInitOrder(orderId, userId, shopId, request.getProductId(),
        request.getQuantity(), price.multiply(BigDecimal.valueOf(request.getQuantity())));
```

- [ ] **Step 3: 验证主代码编译**

```bash
mvn clean compile -pl order-service -am -q
```

预期：`BUILD SUCCESS`。

---

### Task 6: 改造 Converter 层 `OrderConverter.java`

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/converter/OrderConverter.java`

- [ ] **Step 1: 去掉冗余的 `BigDecimal.valueOf` 包装**

第 23 行 `dto.setTotalPrice(BigDecimal.valueOf(order.getTotalPrice()));` → `dto.setTotalPrice(order.getTotalPrice());`

第 54 行 `dto.setTotalPrice(BigDecimal.valueOf(order.getTotalPrice()));` → `dto.setTotalPrice(order.getTotalPrice());`

- [ ] **Step 2: 验证 Converter 编译**

```bash
mvn clean compile -pl order-service -am -q
```

预期：`BUILD SUCCESS`。

---

### Task 7: 修复 order-service 测试 `OrderServiceImplTest.java`

**Files:**
- Modify: `order-service/src/test/java/com/gzasc/aishopping/order/service/OrderServiceImplTest.java`

- [ ] **Step 1: 添加 import `BigDecimal`**

在 import 区追加 `import java.math.BigDecimal;`。

- [ ] **Step 2: 修改 `createOrder` 工厂方法 totalPrice 赋值**

第 79 行 `o.setTotalPrice(100.0);` → `o.setTotalPrice(BigDecimal.valueOf(100));`

- [ ] **Step 3: 修改 5 处 `ProductDTO` 构造调用**

第 104、148、166、187 行：
```java
new ProductDTO(1L, "Test", 50.0, null, null, 10, 100L, null, null)
```
→ 改为 10 参形式（补尾随 `null`），并把 `50.0` 改为 `BigDecimal.valueOf(50)`：
```java
new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null, null)
```

第 206 行：
```java
new ProductDTO(1L, "Test", 50.0, null, null, 10, null, null, null)
```
→ 改为：
```java
new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, null, null, null, null)
```

- [ ] **Step 4: 验证 order-service 测试编译**

```bash
mvn clean test-compile -pl order-service -am -q
```

预期：`BUILD SUCCESS`，0 errors。

---

### Task 8: 修复其他 order-service 测试

**Files:**
- Modify: `order-service/src/test/java/com/gzasc/aishopping/order/mapper/OrderMapperTest.java`
- Modify: `order-service/src/test/java/com/gzasc/aishopping/order/mapper/DeletedOrderMapperTest.java`
- Modify: `order-service/src/test/java/com/gzasc/aishopping/order/stream/OrderEventConsumerTest.java`

- [ ] **Step 1: 修复 `OrderMapperTest.java`**

在 import 区追加 `import java.math.BigDecimal;`，第 37 行 `order.setTotalPrice(99.99);` → `order.setTotalPrice(new BigDecimal("99.99"));`

- [ ] **Step 2: 修复 `DeletedOrderMapperTest.java`**

在 import 区追加 `import java.math.BigDecimal;`，第 38 行 `d.setTotalPrice(99.99);` → `d.setTotalPrice(new BigDecimal("99.99"));`

- [ ] **Step 3: 修复 `OrderEventConsumerTest.java`**

在 import 区追加 `import java.math.BigDecimal;`，第 62 行 `o.setTotalPrice(100.0);` → `o.setTotalPrice(BigDecimal.valueOf(100));`

- [ ] **Step 4: 验证测试编译**

```bash
mvn clean test-compile -pl order-service -am -q
```

预期：`BUILD SUCCESS`。

---

### Task 9: 修复 chat-service 测试

**Files:**
- Modify: `chat-service/src/test/java/com/gzasc/aishopping/chat/controller/ChatControllerTest.java`
- Modify: `chat-service/src/test/java/com/gzasc/aishopping/chat/dto/DtoSerializationTest.java`

- [ ] **Step 1: 修复 `ChatControllerTest.java`**

在 import 区追加 `import java.math.BigDecimal;`。
- 第 103 行：`new OrderItem("ORD001", "P001", 2, 5998.0, "PAID", ...)` → `new OrderItem("ORD001", "P001", 2, BigDecimal.valueOf(5998), "PAID", ...)`
- 第 104 行：`new OrderItem("ORD002", "P002", 1, 199.0, "SHIPPED", ...)` → `new OrderItem("ORD002", "P002", 1, BigDecimal.valueOf(199), "SHIPPED", ...)`
- 第 118 行：`.andExpect(jsonPath("$.data.data.orders[0].totalPrice").value(5998.0))` → `.andExpect(jsonPath("$.data.data.orders[0].totalPrice").value(5998))`

- [ ] **Step 2: 修复 `DtoSerializationTest.java`**

在 import 区追加 `import java.math.BigDecimal;`。
- 第 66 行：`new OrderItem("O001", "P001", 2, 5998.0, "PAID", ...)` → `new OrderItem("O001", "P001", 2, BigDecimal.valueOf(5998), "PAID", ...)`
- 第 124 行：`new OrderItem("O001", "P001", 2, 5998.0, "PAID", ...)` → 同上
- 第 137 行：`assertEquals(5998.0, deserialized.totalPrice());` → `assertEquals(BigDecimal.valueOf(5998), deserialized.totalPrice());`
- 第 190 行：`new OrderItem("O001", "P001", 2, 5998.0, "PAID", ...)` → 同上

- [ ] **Step 3: 验证 chat-service 测试编译**

```bash
mvn clean test-compile -pl chat-service -am -q
```

预期：`BUILD SUCCESS`。

---

### Task 10: 端到端验证

- [ ] **Step 1: 全模块编译**

```bash
mvn clean compile -pl order-service,chat-service -am -q
```

预期：`BUILD SUCCESS`。

- [ ] **Step 2: 跑单元测试（不含需要 MySQL/Redis 的集成测试）**

```bash
mvn test -pl order-service -Dtest='OrderServiceImplTest,OrderEventConsumerTest' -q
mvn test -pl chat-service -Dtest='ChatControllerTest,DtoSerializationTest' -q
```

预期：所有指定单元测试通过。

> 说明：mapper 测试（`OrderMapperTest`、`DeletedOrderMapperTest`）需要 MySQL 数据库，默认环境可能未启动，本任务不强制跑这些。

- [ ] **Step 3: （如需）运行 MyBatis XML 映射一致性自检**

`grep -n "typeHandler" order-service/src/main/resources/**/*.xml` 应无输出（即无自定义 BigDecimal typeHandler）。

---

## 注意事项

- **DB Schema `DECIMAL(10,2)` 不变**，MyBatis 自动 DECIMAL↔BigDecimal，mapper XML 不动。
- **Redis 缓存兼容性**：若生产环境有 `OrderDTO` 缓存，本次类型变更后需清缓存。
- **序列号兼容**：`OrderDTO implements Serializable` 但未声明 `serialVersionUID`，本次不引入兼容性 bug，但建议后续补充显式 UID。
- **不需修改的文件**：`OrderDetailDTO`、`OrderAbstractUserDTO`、`OrderMapper.java/xml`、`DeletedOrderMapper.java/xml`、`OrderUserControllerTest.java`、`OrderToolsTest.java`。
