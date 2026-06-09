# 商品服务低优先级重构 Spec

> **文件名**: `2026-06-09-product-refactor-p3-low.md`
> **优先级**: P3 - 低优先级
> **对应报告问题**: #11, #12, #13, #14, #15
> **状态**: 设计稿（已简化，去掉了过度工程部分）
> **前置依赖**: 建议在 P1/P2 完成后执行

---

## 1. 背景与目标

### 1.1 现状问题

| 问题 | 文件 | 影响 |
|------|------|------|
| #11 未使用/历史遗留方法 | `ProductService:43/69/82/131`, `ProductMapper:77` | 增加接口面积和测试维护成本 |
| #12 Controller 异常处理重复 | `ProductUserController:31/45/64/76/91` | 样板代码多，错误响应策略不集中 |
| #13 批量 ids 字符串解析重复 | `ProductSellerController:47`, `InternalProductController:55` | 空值、非法数字处理不一致 |
| #14 Converter 列表转换模式重复 | `ProductConverter:69/92/125/152` | 转换逻辑膨胀 |
| #15 测试定时任务干扰 | `ProductReservationServiceImpl:100` | 测试日志噪声大 |

### 1.2 重构目标

- 清理未使用的接口方法和 Mapper 方法
- Controller 异常处理统一交由 `GlobalExceptionHandler`
- 批量 ID 解析使用 Spring 标准方式
- Converter 抽取公共 resolve 方法
- 测试时关闭定时调度任务

---

## 2. 历史遗留方法清理

### 2.1 检查清单

以下方法需要确认是否存在调用方（Feign/前端/其他服务）：

#### `ProductService` 接口

| 方法 | 行动 |
|------|------|
| `getAllProductsByShopId` | 搜索调用方，无调用则删除 |
| `getSalableProductsAbstract` | 已标记 `@Deprecated`，确认无调用后删除 |
| `getAbstractProductsForMerchant` | 搜索调用方，无调用则删除 |
| `getProductsByPriceRange` | 搜索调用方，无调用则删除 |

#### `ProductMapper`

| 方法 | 行动 |
|------|------|
| `updateProductImageId` | 搜索调用方（含 XML 映射文件），无调用则删除 |

### 2.2 处理策略

1. 搜索全项目有无调用方
2. 有调用方 → 标注 `@Deprecated(forRemoval=true, since="next-release")`
3. 无调用方 → 直接删除

---

## 3. Controller 异常处理统一

### 3.1 当前问题

`ProductUserController` 中多个方法重复 `try/catch`。

### 3.2 改造方案

增强 `GlobalExceptionHandler`，Controller 去掉 `try/catch`：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductException.class)
    public ApiResponse<Void> handleProductException(ProductException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error(500, "系统内部错误");
    }
}
```

```java
// 改造后 —— Controller 只保留业务编排
@GetMapping("/{productId}")
public ApiResponse<ProductWithImageDetailDTO> getProductDetail(@PathVariable Long productId) {
    return ApiResponse.success(buyerProductService.getBuyerVisibleProductDetail(productId));
}
```

---

## 4. 批量 ID 解析统一

将 `ids.split(",")` 改为 Spring 标准方式：

```java
// 改造前
@GetMapping("/batch")
public ApiResponse<List<ProductDTO>> getBatchProducts(@RequestParam("ids") String ids) {
    String[] idArr = ids.split(",");  // 手动解析
}

// 改造后 —— Spring 自动解析逗号分隔
@GetMapping("/batch")
public ApiResponse<List<ProductDTO>> getBatchProducts(@RequestParam("ids") List<Long> ids) {
    // 直接使用 List<Long>
}
```

---

## 5. Converter 公共方法抽取

### 5.1 当前问题

`ProductConverter` 中每种 DTO 转换都重复获取 imageUrl 和 shopInfo。

### 5.2 改造方案

抽取两个私有工具方法：

```java
@Component
public class ProductConverter {

    private String resolveImageUrl(Integer imageId, Map<Integer, String> imageUrlMap) {
        return imageUrlMap != null && imageId != null
            ? imageUrlMap.getOrDefault(imageId, DEFAULT_IMAGE_URL)
            : DEFAULT_IMAGE_URL;
    }

    private ShopInfoDTO resolveShopInfo(Long shopId, Map<Long, ShopInfoDTO> shopInfoMap) {
        return shopInfoMap != null && shopId != null
            ? shopInfoMap.get(shopId)
            : null;
    }

    // 各 convert 方法调用上述公共方法
}
```

不引入新的类或上下文对象，保持简单。

---

## 6. 测试定时任务隔离

### 6.1 调度任务独立开关

```java
@Component
@ConditionalOnProperty(name = "app.scheduler.reservation.enabled", havingValue = "true", matchIfMissing = true)
public class ReservationScheduler {
    // ...
}
```

### 6.2 测试 Profile 配置

```yaml
# src/test/resources/application-test.yml
app:
  scheduler:
    reservation:
      enabled: false
```

```java
@ActiveProfiles("test")
@SpringBootTest
class ProductReservationServiceTest {
    // ...
}
```

---

## 7. 测试策略

- 删除方法后运行 `mvn test`，确认无编译错误
- 全量运行 147 个测试，确认零失败
- 验证 API 响应结构和状态码一致

---

## 8. 迁移计划

### Phase 1: 异常处理统一 + 方法清理（合并执行，避免交叉修改冲突）
1. 增强 `GlobalExceptionHandler`
2. 清理所有 Controller 的 `try/catch`
3. 搜索无调用方的方法并删除（或标注 `@Deprecated`）
4. 全量回归

### Phase 2: Converter 优化
1. 抽取 `resolveImageUrl`、`resolveShopInfo` 公共方法
2. 替换重复的转换逻辑

### Phase 3: 批量 ID 统一 + 测试隔离
1. 将 `ids.split(",")` 改为 `@RequestParam List<Long>`
2. 添加调度任务开关
3. 确认测试日志无调度任务噪声
