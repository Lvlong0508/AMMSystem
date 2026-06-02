# totalPrice 字段类型统一改造：double → BigDecimal

## 背景

`OrderServiceImpl.java:54` 存在编译错误：`product.getPrice()` 返回 `BigDecimal`，但代码将其赋值给 `double` 变量。进一步排查发现整个 `totalPrice` 字段在模型层使用 `double`，而 DTO 层使用 `BigDecimal`，类型体系不统一。

## 目标

将 `totalPrice` 全线统一为 `BigDecimal`，消除编译错误，消除 `double → BigDecimal` 转换中的精度丢失风险。

## 架构影响图

```
DB (DECIMAL)  ←MyBatis→  Model (BigDecimal)  ←Converter→  DTO (BigDecimal)
                              ↕                          ↕
                        DeletedModel (BigDecimal)     OrderItem (BigDecimal)
```

MyBatis 的 `DECIMAL` ↔ `BigDecimal` 是 JDBC 天然映射，mapper 无需改动。

## 改动清单

### Layer 1 - 模型层（2 文件）

| 文件 | 改动 |
|------|------|
| `Order.java` | `double totalPrice` → `BigDecimal`；`buildInitOrder(..., double totalPrice)` → `BigDecimal` |
| `DeletedOrder.java` | `double totalPrice` → `BigDecimal`；`fromOrder()` 中的 `setTotalPrice()` 改为接收 `BigDecimal` |

**Order.java** 核心变更：
```diff
- private double totalPrice;
+ private BigDecimal totalPrice;

  public static Order buildInitOrder(String orderId, Long userId, String shopId,
-                                    String productId, int quantity, double totalPrice)
+                                    String productId, int quantity, BigDecimal totalPrice)
```

### Layer 2 - DTO 层（2 文件）

| 文件 | 当前类型 | 目标类型 |
|------|---------|---------|
| `OrderDTO.java` (common-api) | `double totalPrice` | `BigDecimal totalPrice` |
| `OrderItem.java` (chat-service) | `Double totalPrice` | `BigDecimal totalPrice` |
| `OrderDetailDTO.java` | `BigDecimal` ✅ | 不变 |
| `OrderAbstractUserDTO.java` | `BigDecimal` ✅ | 不变 |

**OrderDTO.java** (common-api) 核心变更：
```diff
- private double totalPrice;
+ private BigDecimal totalPrice;
```
需要新增 import `java.math.BigDecimal`。

**OrderItem.java** (chat-service) 核心变更：
```diff
- Double totalPrice,
+ BigDecimal totalPrice,
```
需要新增 import `java.math.BigDecimal`。

### Layer 3 - Service 层（1 文件）

**OrderServiceImpl.java**：将 `createOrder` 中的价格计算从 `double` 算术改为 `BigDecimal` 算术。

```diff
- double price = product.getPrice() != null ? product.getPrice().doubleValue() : 0.0;
+ BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;

- Order order = Order.buildInitOrder(orderId, userId, shopId, request.getProductId(),
-        request.getQuantity(), price * request.getQuantity());
+ Order order = Order.buildInitOrder(orderId, userId, shopId, request.getProductId(),
+        request.getQuantity(), price.multiply(BigDecimal.valueOf(request.getQuantity())));
```

需要新增 import `java.math.BigDecimal`。

### Layer 4 - Converter 层（1 文件）

**OrderConverter.java**：利用模型已是 `BigDecimal`，去掉多余的 `BigDecimal.valueOf()` 转换。

```diff
- dto.setTotalPrice(BigDecimal.valueOf(order.getTotalPrice()));
+ dto.setTotalPrice(order.getTotalPrice());
```

两处（`toUserAbstractDTO` 和 `toDetailDTO`）同步修改。

### Layer 5 - 测试层（6 文件）

**order-service 测试文件：**

| 文件 | 行 | 当前写法 | 改为 |
|------|:--:|---------|------|
| `OrderServiceImplTest.java` | 79 | `setTotalPrice(100.0)` | `setTotalPrice(BigDecimal.valueOf(100))` |
| `OrderMapperTest.java` | 37 | `setTotalPrice(99.99)` | `setTotalPrice(new BigDecimal("99.99"))` |
| `DeletedOrderMapperTest.java` | 38 | `setTotalPrice(99.99)` | `setTotalPrice(new BigDecimal("99.99"))` |
| `OrderEventConsumerTest.java` | 62 | `setTotalPrice(100.0)` | `setTotalPrice(BigDecimal.valueOf(100))` |

测试中 `ProductDTO` 构造的 `50.0` 参数（common-api ProductDTO.price 已是 BigDecimal）需同步改为 `BigDecimal.valueOf(50)`，共 5 处（`OrderServiceImplTest` 的 L104、L148、L166、L187、L206）。

**chat-service 测试文件（遗漏补充）：**

| 文件 | 行 | 当前写法 | 改为 |
|------|:--:|---------|------|
| `ChatControllerTest.java` | 103 | `new OrderItem(..., 5998.0, ...)` | `new OrderItem(..., BigDecimal.valueOf(5998), ...)` |
| `ChatControllerTest.java` | 104 | `new OrderItem(..., 199.0, ...)` | `new OrderItem(..., BigDecimal.valueOf(199), ...)` |
| `ChatControllerTest.java` | 118 | `.value(5998.0)` | `.value(5998)` |
| `DtoSerializationTest.java` | 66 | `new OrderItem(..., 5998.0, ...)` | `new OrderItem(..., BigDecimal.valueOf(5998), ...)` |
| `DtoSerializationTest.java` | 124 | `new OrderItem(..., 5998.0, ...)` | `new OrderItem(..., BigDecimal.valueOf(5998), ...)` |
| `DtoSerializationTest.java` | 137 | `assertEquals(5998.0, ...)` | `assertEquals(BigDecimal.valueOf(5998), ...)` |
| `DtoSerializationTest.java` | 190 | `new OrderItem(..., 5998.0, ...)` | `new OrderItem(..., BigDecimal.valueOf(5998), ...)` |

需要在所有测试文件新增 import `java.math.BigDecimal`。

## 不变的部分

| 组件 | 原因 |
|------|------|
| DB Schema `DECIMAL(10,2)` | 与 BigDecimal 天然兼容 |
| `OrderMapper.java` / `DeletedOrderMapper.java` | MyBatis 自动映射 DECIMAL ↔ BigDecimal |
| `OrderDetailDTO.java` | 已是 BigDecimal ✅ |
| `OrderAbstractUserDTO.java` | 已是 BigDecimal ✅ |
| `OrderUserControllerTest.java` | 第 289 行已使用 `BigDecimal.valueOf(100)` ✅ |
| `OrderToolsTest.java` (chat-service) | 使用 `Map<String, Object>`，无类型约束 ✅ |

## 执行顺序

1. 模型层（Order.java + DeletedOrder.java）
2. DTO 层（OrderDTO.java + OrderItem.java）
3. Service 层（OrderServiceImpl.java）
4. Converter 层（OrderConverter.java）
5. 测试层（4 个测试文件）
6. 编译验证 `mvn compile -pl order-service -am`
7. 运行测试 `mvn test -pl order-service`

## Codegraph 影响分析

| 分析维度 | 结果 |
|---------|------|
| `Order.totalPrice` 影响范围 | **58 个节点**，涵盖模型、Service、Converter、DTO、测试 |
| `Order.buildInitOrder` 调用者 | 仅 `OrderServiceImpl.createOrder` (第45行) |
| `DeletedOrder.fromOrder` 影响 | **31 个节点**，包含 deleteOrder、测试文件 |
| `OrderConverter.toUserAbstractDTO` 影响 | **20 个节点**，包含 getOrdersByUserId、getOrdersByShopId |

## 注意事项与修复方案

### 1. DTO 层跨模块依赖

**问题**：`OrderDTO.java` 位于 `common-api` 模块，被多个服务引用。

**修复方案**：
- 修改 `common-api` 后需重新打包：`mvn clean install -pl common-api`
- 确保所有依赖服务（order-service、chat-service）更新依赖版本

### 2. chat-service OrderItem record 类型

**问题**：`OrderItem.java` 使用 Java record，构造时需注意类型匹配。

**修复方案**：
```diff
- OrderItem item = new OrderItem(orderId, productId, quantity, 100.0, ...);
+ OrderItem item = new OrderItem(orderId, productId, quantity, BigDecimal.valueOf(100), ...);
```

### 3. 测试文件 import 新增

**问题**：所有测试文件需新增 `import java.math.BigDecimal`。

**修复方案**：在每个测试文件头部添加：
```java
import java.math.BigDecimal;
```

### 4. ProductDTO 构造参数类型

**问题**：`ProductDTO` 的 `price` 字段已是 `BigDecimal`，但测试中使用 `double` 字面量构造。

**修复方案**：
```diff
- ProductDTO mockProduct = new ProductDTO(1L, "Test", 50.0, null, null, 10, 100L, null, null);
+ ProductDTO mockProduct = new ProductDTO(1L, "Test", BigDecimal.valueOf(50), null, null, 10, 100L, null, null);
```

### 5. 编译顺序

**问题**：模型层和 DTO 层修改后，Service 层和 Converter 层才能编译通过。

**修复方案**：严格按照执行顺序操作，每层修改后验证编译：
```bash
# 模型层修改后
mvn compile -pl order-service -am

# DTO 层修改后（需先打包 common-api）
mvn clean install -pl common-api
mvn compile -pl order-service,chat-service -am
```

### 6. MyBatis 映射验证

**问题**：虽然 MyBatis 天然支持 DECIMAL ↔ BigDecimal，但需确认 XML 映射文件无自定义类型转换。

**修复方案**：检查 `OrderMapper.xml` 和 `DeletedOrderMapper.xml`，确认无 `<result>` 标签中的 `typeHandler` 配置。如有，需移除或更新为 `BigDecimalTypeHandler`。

### 7. 序列化兼容性

**问题**：`OrderDTO` 实现了 `Serializable`，类型变更可能影响 Redis 缓存或消息队列中的旧数据。

**修复方案**：
- 如果有 Redis 缓存订单数据，需在部署后清理缓存
- 如果有消息队列传输 OrderDTO，需确保消费者和生产者同时部署
- 建议添加 `serialVersionUID` 并在类型变更后更新其值
