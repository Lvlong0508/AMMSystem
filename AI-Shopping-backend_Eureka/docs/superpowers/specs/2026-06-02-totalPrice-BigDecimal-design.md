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

### Layer 5 - 测试层（4 文件 4 处 literal）

| 文件 | 行 | 当前写法 | 改为 |
|------|:--:|---------|------|
| `OrderServiceImplTest.java` | 79 | `setTotalPrice(100.0)` | `setTotalPrice(BigDecimal.valueOf(100.0))` |
| `OrderMapperTest.java` | 37 | `setTotalPrice(99.99)` | `setTotalPrice(BigDecimal.valueOf(99.99))` |
| `DeletedOrderMapperTest.java` | 38 | `setTotalPrice(99.99)` | `setTotalPrice(BigDecimal.valueOf(99.99))` |
| `OrderEventConsumerTest.java` | 62 | `setTotalPrice(100.0)` | `setTotalPrice(BigDecimal.valueOf(100.0))` |

测试中 `ProductDTO` 构造的 `50.0` 参数（common-api ProductDTO.price 已是 BigDecimal）需同步改为 `BigDecimal.valueOf(50.0)`，共 5 处（`OrderServiceImplTest` 的 L104、L148、L166、L187、L206）。

需要在各测试文件新增 import `java.math.BigDecimal`。

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
