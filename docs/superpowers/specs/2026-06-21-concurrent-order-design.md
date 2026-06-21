# 下单流程并发升级设计

## 概述

将 order-service 下单流程从全串行升级为并发编程，应对日常大促 QPS 几百级场景。

- **技术栈**：纯 Redis，不引入消息队列
- **一致性模型**：最终一致 -- 先 Redis 原子扣库存，再落 DB，定时对账修正
- **Sentinel 限流配置调整**：不在本设计范围内，另行提供独立计划

## 当前问题

现有 `OrderServiceImpl.createOrder()`（`OrderServiceImpl.java:50-90`）存在以下问题：

1. `getProductById` 和 `getContactById` 两个互相独立的远程调用串行执行
2. `reserveStock` Feign 调用在 `@Transactional` 内部，DB 连接长时间被占用
3. 商品服务用 `SELECT FOR UPDATE` 悲观锁，同商品并发下单被串行化
4. 无 Redis 层库存防超卖，全靠 DB 行锁

## 现有可复用代码

自审查发现以下代码可直接复用，避免重复造轮子：

| 现有代码 | 位置 | 复用方式 |
|---------|------|---------|
| `StringRedisTemplate` | order-service 已注入（`RedisOrderIdGenerator` 等） | 直接用于 Lua 脚本执行 |
| `@EnableScheduling` | `ProductServiceApplication.java:15` 已有 | 对账任务无需新增配置 |
| `@EnableAsync` | `ProductServiceApplication.java:14` 已有 | 无需新增 |
| `ProductMapper.selectProductById` | `ProductMapper.java:23` | 预热时查商品库存，无需新增 `selectStockById` |
| `ProductReservationMapper.sumReservedQty` | `ProductReservationMapper.java:32` | 对账任务直接复用 |
| `ProductStockMapper.deductStock` | `ProductStockMapper.java:10` | 对账任务 DB 扣减复用（已有 CAS 乐观锁） |
| `ProductStockMapper.restoreStock` | `ProductStockMapper.java:13` | 对账任务 DB 回滚复用 |
| `RedisOrderIdGenerator` | order-service 已有 | 订单 ID 生成不变 |
| Spring Boot Redis 自动配置 | order-service 已有 | 无需新增 RedisConfig |

## 改造后下单流程

```
请求到达
  |
  v
(1) Sentinel 限流检查（已有，配置另行调整）
  |
  v
(2) CompletableFuture 并行执行：
    |- 线程1: productFeignClient.getProductById()
    |- 线程2: contactFeignClient.getContactById()
  |
  v (两个 Future 都 join)
(3) Redis Lua 原子扣减库存（stock:product:{productId}）
    | 失败 -> 抛异常，流程终止
    | 成功 -> 继续
  |
  v
(4) 生成订单ID（Redis INCR，已有）
  |
  v
(5) 写订单到 DB（orderMapper.insertOrder）
    | 失败 -> 补偿：Redis 加回库存，抛异常
    | 成功 -> 继续
  |
  v
(6) 库存 DB 同步由 product-service 定时对账任务维护（不在下单关键路径）
  |
  v
(7) 返回 orderId
```

### 关键变化

| 项目 | 改造前 | 改造后 |
|------|--------|--------|
| 查商品 + 查联系人 | 串行 Feign | CompletableFuture 并行 |
| 库存检查 + 扣减 | DB SELECT FOR UPDATE | Redis Lua 原子扣减 |
| 库存 DB 同步 | 同步 Feign 调用（在事务内） | 定时对账任务异步落库 |
| 事务边界 | 跨 Feign 调用的长事务 | 去掉 @Transactional（单条 SQL 自动提交） |
| 防超卖 | DB 行锁 | Redis Lua CAS |

### 事务边界调整

- **去掉 `createOrder` 上的 `@Transactional`**：改造后只有一条 `insertOrder` SQL，MyBatis 自动提交，无需显式事务
- Redis 库存扣减在事务外，失败时用补偿机制回滚
- 库存 DB 同步移到 product-service 的定时对账任务中，不阻塞下单流程

## Redis Lua 原子扣库存

### Redis 数据结构

```
Key:   stock:product:{productId}
Value: 剩余库存数（整数）
TTL:   无（由库存同步机制维护）
```

### Lua 脚本

文件路径：`order-service/src/main/resources/lua/deduct_stock.lua`

```
-- KEYS[1] = stock:product:{productId}
-- ARGV[1] = quantity (要扣减的数量)

local stock = redis.call('GET', KEYS[1])
if not stock then
    return -1  -- 库存数据未预热
end

stock = tonumber(stock)
local qty = tonumber(ARGV[1])

if stock < qty then
    return 0  -- 库存不足
end

redis.call('DECRBY', KEYS[1], qty)
return 1  -- 扣减成功
```

**返回值约定**：
- `1` = 扣减成功
- `0` = 库存不足
- `-1` = Redis 中无此商品库存缓存（需预热）

### 补偿回滚

DB 写入失败时用 `INCRBY` 加回库存，无需 Lua 脚本，直接调用 `StringRedisTemplate.opsForValue().increment()`。

## 库存预热与对账（product-service）

合并为一个 `StockSyncTask` 类（@Component），包含启动预热和定时对账两个职责。无需新增接口和 Feign 调用。

### product-service 新增 Redis 依赖

`product-service/pom.xml` 新增：
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

`product-service/application.yml` 新增（复用 order-service 的配置格式）：
```
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

### StockSyncTask 实现

```
@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncTask implements ApplicationRunner {

    private final ProductMapper productMapper;
    private final ProductReservationMapper productReservationMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String STOCK_KEY_PREFIX = "stock:product:";

    @Override
    public void run(ApplicationArguments args) {
        warmupAll();
    }

    @Scheduled(fixedDelay = 30000)
    public void reconcileStock() {
        List<Product> products = productMapper.selectAllIdAndStock();
        for (Product p : products) {
            String redisKey = STOCK_KEY_PREFIX + p.getId();
            String redisStockStr = redisTemplate.opsForValue().get(redisKey);

            if (redisStockStr == null) {
                redisTemplate.opsForValue().set(redisKey, String.valueOf(p.getStock()));
                continue;
            }

            int redisStock = Integer.parseInt(redisStockStr);
            int reservedQty = productReservationMapper.sumReservedQty(p.getId());

            int expectedRedisStock = p.getStock() - reservedQty;
            if (redisStock != expectedRedisStock) {
                redisTemplate.opsForValue().set(redisKey, String.valueOf(expectedRedisStock));
            }
        }
    }

    private void warmupAll() {
        List<Product> products = productMapper.selectAllIdAndStock();
        for (Product p : products) {
            redisTemplate.opsForValue().set(STOCK_KEY_PREFIX + p.getId(), String.valueOf(p.getStock()));
        }
        log.info("库存预热完成，共 {} 个商品", products.size());
    }
}
```

**一致性公式**：`Redis库存 = DB商品表stock - 预占记录总数量`

### ProductMapper 新增方法

`ProductMapper.java` 新增一个轻量查询（只查 id 和 stock）：

```
@Select("SELECT id, stock FROM products")
List<Product> selectAllIdAndStock();
```

### 商品创建/修改时即时预热

`ProductCommandServiceImpl` 注入 `StringRedisTemplate`，在 `createProductWithImage` 和 `updateProductWithImage` 末尾加一行：

```
// createProductWithImage 末尾
redisTemplate.opsForValue().set("stock:product:" + productId, String.valueOf(product.getStock()));

// updateProductWithImage 末尾（仅当 stock 字段被修改时）
if (product.getStock() != null) {
    redisTemplate.opsForValue().set("stock:product:" + product.getId(), String.valueOf(product.getStock()));
}
```

### order-service 不再调用 reserveStock Feign

改造后 order-service 通过 Redis Lua 直接扣减库存，不再通过 Feign 调用 `reserveStock`。product-service 的 `reserveStock` 接口保留但不再由 order-service 调用，改由对账任务间接维护预占记录。

## CompletableFuture 并行调用与线程池

### 线程池配置

新增文件：`order-service/.../config/OrderThreadPoolConfig.java`

```
@Configuration
public class OrderThreadPoolConfig {
    @Bean("orderExecutor")
    public ThreadPoolTaskExecutor orderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("order-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

参数说明：
- 核心线程 8，最大 16：日常大促 QPS 几百级足够
- 队列 100：防止无界队列导致 OOM
- `CallerRunsPolicy`：队列满时由调用线程执行，起到背压作用

### 并行调用代码结构

```
CompletableFuture<ProductDTO> productFuture = CompletableFuture.supplyAsync(() -> {
    ApiResponse<ProductDTO> resp = productFeignClient.getProductById(request.getProductId());
    if (resp == null || resp.getData() == null) {
        throw new CompletionException(new OrderException("商品不存在（错误代码：O-003）"));
    }
    return resp.getData();
}, orderExecutor);

CompletableFuture<ContactDTO> contactFuture = CompletableFuture.supplyAsync(() -> {
    ApiResponse<ContactDTO> resp = contactFeignClient.getContactById(request.getContactId());
    if (resp == null || resp.getData() == null) {
        throw new CompletionException(new OrderException("联系人不存在，请重新选择联系人（错误代码：O-006）"));
    }
    return resp.getData();
}, orderExecutor);

try {
    CompletableFuture.allOf(productFuture, contactFuture).join();
} catch (CompletionException e) {
    throw (e.getCause() instanceof OrderException)
        ? (OrderException) e.getCause()
        : new OrderException("系统异常，请稍后重试");
}

ProductDTO product = productFuture.join();
ContactDTO contact = contactFuture.join();
```

### Feign 超时配置

`order-service/application.yml` 新增：

```
spring:
  cloud:
    openfeign:
      client:
        config:
          product-service:
            connect-timeout: 1000
            read-timeout: 3000
          contact-service:
            connect-timeout: 1000
            read-timeout: 3000
```

原来两个 Feign 调用串行最坏要 6 秒（各 3 秒），并行后最坏只要 3 秒。

### 异常处理策略

| 场景 | 处理 |
|------|------|
| 商品查询失败 | 直接抛异常，不等待联系人查询 |
| 联系人查询失败 | 直接抛异常，不等待商品查询 |
| 两个都失败 | 先抛先完成的那个异常 |
| Feign 熔断（429） | 包装为 CompletionException，解包后抛出 |

## 改造后 OrderServiceImpl.createOrder 核心结构

```
@Override
public String createOrder(PlaceOrderRequest request, Long userId) {
    // 1. 并行查商品 + 联系人（CompletableFuture）
    ProductDTO product = ...;
    ContactDTO contact = ...;

    // 2. Redis Lua 原子扣库存
    long result = stockRedisService.deductStock(productId, quantity);
    if (result == -1) {
        throw new OrderException("库存数据未预热，请稍后重试（错误代码：O-007）");
    }
    if (result == 0) {
        throw new OrderException("商品库存不足（错误代码：O-005）");
    }

    // 3. 生成订单ID + 构建订单（已有逻辑）
    String orderId = orderIdSelector.generate();
    Order order = Order.buildInitOrder(...);

    // 4. 写订单 DB（单条 SQL，无需 @Transactional）
    try {
        if (orderMapper.insertOrder(order) <= 0) {
            stockRedisService.restoreStock(productId, quantity);
            throw new OrderException("创建订单失败");
        }
    } catch (Exception e) {
        stockRedisService.restoreStock(productId, quantity);
        throw new OrderException("创建订单失败");
    }

    return orderId;
}
```

### 补偿机制

Redis 扣减成功后写 DB 失败时，把已扣减的 Redis 库存加回去：

```
stockRedisService.restoreStock(productId, quantity);  // 内部执行 INCRBY
```

## StockRedisService

@Component，无接口，封装 Lua 脚本加载和执行：

```
@Slf4j
@Component
@RequiredArgsConstructor
public class StockRedisService {

    private final StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> deductScript;

    @PostConstruct
    public void init() {
        deductScript = new DefaultRedisScript<>();
        deductScript.setLocation(new ClassPathResource("lua/deduct_stock.lua"));
        deductScript.setResultType(Long.class);
    }

    public long deductStock(Long productId, int quantity) {
        String key = "stock:product:" + productId;
        Long result = redisTemplate.execute(deductScript, List.of(key), String.valueOf(quantity));
        return result != null ? result : -1;
    }

    public void restoreStock(Long productId, int quantity) {
        String key = "stock:product:" + productId;
        redisTemplate.opsForValue().increment(key, quantity);
    }
}
```

## 一致性保障总结

| 场景 | 保障机制 |
|------|---------|
| Redis 扣减成功，DB 写入失败 | 补偿 INCRBY 回滚 |
| Redis 扣减成功，DB 写入成功 | 定时对账修正差异 |
| Redis 数据丢失 | 对账任务从 DB 重建 |
| 对账期间有新下单 | Lua 原子操作不受对账影响（对账只 SET，Lua 用 GET+DECRBY，有短暂窗口但可接受） |

## 改造涉及的文件清单

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `order-service/.../config/OrderThreadPoolConfig.java` | 新增 | 下单线程池配置 |
| `order-service/.../component/StockRedisService.java` | 新增 | Redis Lua 扣库存 + 补偿回滚（@Component，无接口） |
| `order-service/src/main/resources/lua/deduct_stock.lua` | 新增 | 扣减库存 Lua 脚本 |
| `order-service/.../service/impl/OrderServiceImpl.java` | 修改 | 并行调用 + Redis Lua 扣库存 + 去掉 @Transactional + 补偿 |
| `order-service/src/main/resources/application.yml` | 修改 | 新增 Feign 超时配置 |
| `product-service/pom.xml` | 修改 | 新增 spring-boot-starter-data-redis 依赖 |
| `product-service/src/main/resources/application.yml` | 修改 | 新增 Redis 配置 |
| `product-service/.../task/StockSyncTask.java` | 新增 | 启动预热 + 定时对账（合并为一个类，复用现有 Mapper） |
| `product-service/.../mapper/ProductMapper.java` | 修改 | 新增 selectAllIdAndStock() 方法 |
| `product-service/.../service/impl/ProductCommandServiceImpl.java` | 修改 | 商品创建/修改时即时预热 Redis |

**共 10 个文件**（4 新增 + 6 修改），相比初版设计 15 个文件减少 5 个。

### 删除的过度设计（初版有，自审查后移除）

| 删除项 | 理由 |
|--------|------|
| OrderTransactionalService 接口 + Impl | 单条 SQL 不需要 @Transactional，MyBatis 自动提交 |
| StockCacheService 接口 + Impl | 过度设计，预热和对账合并到 StockSyncTask |
| ScheduleConfig.java | product-service 已有 @EnableScheduling |
| OrderInitRunner.java | 预热由 product-service 自己负责，不需要 order-service 通过 Feign 触发 |
| warmupAllStockCache Feign 接口 | 同上，不需要跨服务调用 |
| restore_stock.lua | 回滚只需 INCRBY，直接用 StringRedisTemplate API |
| InternalProductController 预热端点 | 不需要暴露 HTTP 接口 |
| ProductMapper.selectStockById | 用 selectAllIdAndStock 替代 |

## 测试策略

| 层级 | 测试内容 | 工具 |
|------|---------|------|
| Lua 脚本单测 | 库存充足/不足/未预热三种场景 | 嵌入式 Redis (embedded-redis) |
| 并发单测 | 100 线程同时下单同一商品，验证不超卖 | CountDownLatch + JUnit |
| 补偿单测 | DB 写入失败时 Redis 库存正确回滚 | Mockito mock OrderMapper 抛异常 |
| 并行调用单测 | 商品/联系人查询并行执行，异常正确传播 | Mockito mock Feign + CompletableFuture |
| 对账任务单测 | Redis-DB 不一致时被修正 | 嵌入式 Redis + H2 内存库 |
| 集成测试 | 完整下单流程端到端 | Spring Boot Test |

### 并发防超卖验证

```
@Test
void concurrentOrder_sameProduct_shouldNotOversell() throws InterruptedException {
    int threadCount = 100;
    int stock = 10;
    redisTemplate.opsForValue().set("stock:product:1", String.valueOf(stock));

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);

    for (int i = 0; i < threadCount; i++) {
        new Thread(() -> {
            try {
                startLatch.await();
                long result = stockRedisService.deductStock(1L, 1);
                if (result == 1) successCount.incrementAndGet();
                else failCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        }).start();
    }

    startLatch.countDown();
    endLatch.await();

    assertEquals(stock, successCount.get());
    assertEquals(threadCount - stock, failCount.get());
}
```

## 范围外事项

- **Sentinel 限流配置调整**：单独提供独立计划，不纳入本设计范围

## 改造后 OrderServiceImpl.createOrder 核心结构

```
@Override
public String createOrder(PlaceOrderRequest request, Long userId) {
    // 1. 并行查商品 + 联系人（CompletableFuture）
    ProductDTO product = ...;
    ContactDTO contact = ...;

    // 2. Redis Lua 原子扣库存
    long result = stockRedisService.deductStock(productId, quantity);
    if (result == -1) {
        throw new OrderException("库存数据未预热，请稍后重试（错误代码：O-007）");
    }
    if (result == 0) {
        throw new OrderException("商品库存不足（错误代码：O-005）");
    }

    // 3. 生成订单ID + 构建订单（已有逻辑）
    String orderId = orderIdSelector.generate();
    Order order = Order.buildInitOrder(...);

    // 4. 写订单 DB（单条 SQL，无需 @Transactional）
    try {
        if (orderMapper.insertOrder(order) <= 0) {
            stockRedisService.restoreStock(productId, quantity);
            throw new OrderException("创建订单失败");
        }
    } catch (Exception e) {
        stockRedisService.restoreStock(productId, quantity);
        throw new OrderException("创建订单失败");
    }

    return orderId;
}
```

### 补偿机制

Redis 扣减成功后写 DB 失败时，把已扣减的 Redis 库存加回去：

```
stockRedisService.restoreStock(productId, quantity);  // 内部执行 INCRBY
```

## StockRedisService

@Component，无接口，封装 Lua 脚本加载和执行：

```
@Slf4j
@Component
@RequiredArgsConstructor
public class StockRedisService {

    private final StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> deductScript;

    @PostConstruct
    public void init() {
        deductScript = new DefaultRedisScript<>();
        deductScript.setLocation(new ClassPathResource("lua/deduct_stock.lua"));
        deductScript.setResultType(Long.class);
    }

    public long deductStock(Long productId, int quantity) {
        String key = "stock:product:" + productId;
        Long result = redisTemplate.execute(deductScript, List.of(key), String.valueOf(quantity));
        return result != null ? result : -1;
    }

    public void restoreStock(Long productId, int quantity) {
        String key = "stock:product:" + productId;
        redisTemplate.opsForValue().increment(key, quantity);
    }
}
```

## 一致性保障总结

| 场景 | 保障机制 |
|------|---------|
| Redis 扣减成功，DB 写入失败 | 补偿 INCRBY 回滚 |
| Redis 扣减成功，DB 写入成功 | 定时对账修正差异 |
| Redis 数据丢失 | 对账任务从 DB 重建 |
| 对账期间有新下单 | Lua 原子操作不受对账影响（对账只 SET，Lua 用 GET+DECRBY，有短暂窗口但可接受） |

## 改造涉及的文件清单

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `order-service/.../config/OrderThreadPoolConfig.java` | 新增 | 下单线程池配置 |
| `order-service/.../component/StockRedisService.java` | 新增 | Redis Lua 扣库存 + 补偿回滚（@Component，无接口） |
| `order-service/src/main/resources/lua/deduct_stock.lua` | 新增 | 扣减库存 Lua 脚本 |
| `order-service/.../service/impl/OrderServiceImpl.java` | 修改 | 并行调用 + Redis Lua 扣库存 + 去掉 @Transactional + 补偿 |
| `order-service/src/main/resources/application.yml` | 修改 | 新增 Feign 超时配置 |
| `product-service/pom.xml` | 修改 | 新增 spring-boot-starter-data-redis 依赖 |
| `product-service/src/main/resources/application.yml` | 修改 | 新增 Redis 配置 |
| `product-service/.../task/StockSyncTask.java` | 新增 | 启动预热 + 定时对账（合并为一个类，复用现有 Mapper） |
| `product-service/.../mapper/ProductMapper.java` | 修改 | 新增 selectAllIdAndStock() 方法 |
| `product-service/.../service/impl/ProductCommandServiceImpl.java` | 修改 | 商品创建/修改时即时预热 Redis |

**共 10 个文件**（4 新增 + 6 修改），相比初版设计 15 个文件减少 5 个。

### 删除的过度设计（初版有，自审查后移除）

| 删除项 | 理由 |
|--------|------|
| OrderTransactionalService 接口 + Impl | 单条 SQL 不需要 @Transactional，MyBatis 自动提交 |
| StockCacheService 接口 + Impl | 过度设计，预热和对账合并到 StockSyncTask |
| ScheduleConfig.java | product-service 已有 @EnableScheduling |
| OrderInitRunner.java | 预热由 product-service 自己负责，不需要 order-service 通过 Feign 触发 |
| warmupAllStockCache Feign 接口 | 同上，不需要跨服务调用 |
| restore_stock.lua | 回滚只需 INCRBY，直接用 StringRedisTemplate API |
| InternalProductController 预热端点 | 不需要暴露 HTTP 接口 |
| ProductMapper.selectStockById | 用 selectAllIdAndStock 替代 |

## 测试策略

| 层级 | 测试内容 | 工具 |
|------|---------|------|
| Lua 脚本单测 | 库存充足/不足/未预热三种场景 | 嵌入式 Redis (embedded-redis) |
| 并发单测 | 100 线程同时下单同一商品，验证不超卖 | CountDownLatch + JUnit |
| 补偿单测 | DB 写入失败时 Redis 库存正确回滚 | Mockito mock OrderMapper 抛异常 |
| 并行调用单测 | 商品/联系人查询并行执行，异常正确传播 | Mockito mock Feign + CompletableFuture |
| 对账任务单测 | Redis-DB 不一致时被修正 | 嵌入式 Redis + H2 内存库 |
| 集成测试 | 完整下单流程端到端 | Spring Boot Test |

### 并发防超卖验证

```
@Test
void concurrentOrder_sameProduct_shouldNotOversell() throws InterruptedException {
    int threadCount = 100;
    int stock = 10;
    redisTemplate.opsForValue().set("stock:product:1", String.valueOf(stock));

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);

    for (int i = 0; i < threadCount; i++) {
        new Thread(() -> {
            try {
                startLatch.await();
                long result = stockRedisService.deductStock(1L, 1);
                if (result == 1) successCount.incrementAndGet();
                else failCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        }).start();
    }

    startLatch.countDown();
    endLatch.await();

    assertEquals(stock, successCount.get());
    assertEquals(threadCount - stock, failCount.get());
}
```

## 范围外事项

- **Sentinel 限流配置调整**：单独提供独立计划，不纳入本设计范围
