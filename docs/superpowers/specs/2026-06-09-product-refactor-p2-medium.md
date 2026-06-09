# 商品服务中优先级重构 Spec

> **文件名**: `2026-06-09-product-refactor-p2-medium.md`
> **优先级**: P2 - 中优先级
> **对应报告问题**: #6, #7, #8, #9, #10
> **状态**: 设计稿（已简化，去掉了过度工程部分）

---

## 1. 背景与目标

### 1.1 现状问题

| 问题 | 文件 | 影响 |
|------|------|------|
| #6 库存逻辑分散 | `ProductServiceImpl:266`, `ProductReservationServiceImpl:29`, `ProductReservationMapper:28` | 库存规则散落，Mapper 越界更新 products.stock |
| #7 `@Deprecated` DTO 被主流程使用 | `ProductWithImageAbstractDTO:10`, `ProductWithImageDetailDTO:12`, `ProductConverter:20`, `ProductServiceImpl:125` | 废弃代码积压，误导开发者继续使用 |
| #8 图片上传校验重复 | `ProductSellerController:61/92` | 校验策略散在入口层 |
| #9 图片类型/空文件校验重复 | `ProductSellerController` 多处 | 新增类型限制时需改多处 |
| #10 图片 URL 拼接耦合配置细节 | `ProductServiceImpl:392`, `ImageStorageServiceImpl:63/72` | URL 规则变化影响多类 |

### 1.2 重构目标

- 库存操作收敛到 `InventoryService`，Mapper 不越界更新库存
- 废弃 DTO 和转换方法清理
- 图片校验统一到 `ImageStorageService`
- 图片 URL 统一由 `ImageStorageService` 管理

---

## 2. 库存服务统一

### 2.1 当前问题

库存操作分散：
- `ProductServiceImpl.restoreStock` → 直接调用 `ProductMapper.restoreStock`
- `ProductReservationServiceImpl` → 通过 `ProductReservationMapper` 更新 `products.stock`

`ProductReservationMapper` 跨越了自己的边界，直接操作 `products` 表。

### 2.2 改造方案

#### 新建 `ProductStockMapper`

```java
@Mapper
public interface ProductStockMapper {
    @Update("UPDATE products SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Update("UPDATE products SET stock = stock + #{quantity} WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
```

#### 新建 `InventoryService`

```java
public interface InventoryService {
    boolean restoreStock(Long productId, int quantity);
}
```

库存不足时直接抛出 `ProductException(409, "库存不足")`，由全局异常处理器处理。

#### 调用链调整

| 当前 | 变更为 |
|------|--------|
| `ProductReservationMapper` 直接更新 `products.stock` | 使用 `ProductStockMapper` |
| `ProductServiceImpl.restoreStock` | 委托给 `InventoryService` |

---

## 3. 废弃 DTO 和 Converter 清理

### 3.1 处理方案

| DTO | 行动 |
|-----|------|
| `ProductWithImageAbstractDTO` | 去掉 `@Deprecated` 或确认是否已被 `ProductCardDTO` 完全替代，确认后删除 |
| `ProductWithImageDetailDTO` | 去掉 `@Deprecated`（仍是详情查询的核心 DTO） |

### 3.2 具体步骤

1. 确认 `ProductWithImageDetailDTO` 仍有独立价值 → 去掉 `@Deprecated`
2. 如果 `ProductWithImageAbstractDTO` 已被 `ProductCardDTO` 完全替代，切换到新 DTO 后删除
3. `ProductConverter` 中对应的方法同步清理
4. 全量测试验证

---

## 4. 图片校验统一

### 4.1 当前问题

`ProductSellerController` 的创建和更新方法中重复校验图片状态。

### 4.2 改造方案

在 `ImageStorageService` 中增加前置校验，Controller 不再重复判断：

```java
public interface ImageStorageService {
    String saveImage(Long productId, MultipartFile imageFile);

    /** 校验图片文件，失败时抛出 ProductException */
    void validateImage(MultipartFile imageFile);
}
```

校验规则（简单实用）：

| 规则 | 实现 |
|------|------|
| 空文件检查 | `image == null \|\| image.isEmpty()` |
| 类型白名单 | `image.getContentType()` 属于 `image/jpeg`、`image/png` |
| 大小限制 | 超过 5MB 拒绝（`application.yml` 可配） |

Controlller 做简单前置校验后调用 `ImageStorageService.saveImage`，不再各自写一套。

---

## 5. 图片 URL 统一管理

### 5.1 当前问题

```
ProductServiceImpl:    imageBaseUrl + relativePath   // 拼 URL
ImageStorageServiceImpl:  删除时用 imageBaseUrl 反解析  // 拆 URL
```

URL 的拼接和理解分散在两个服务中。

### 5.2 改造方案

`ImageStorageService` 统一返回**完整 URL**：

```java
public interface ImageStorageService {
    /** 返回完整的可访问 URL（含 baseUrl） */
    String saveImage(Long productId, MultipartFile imageFile);

    void deleteImage(String fullImageUrl);
}
```

变更点：

| 文件 | 变更 |
|------|------|
| `ImageStorageService.saveImage` | 返回 `imageBaseUrl + relativePath` 完整 URL |
| `ProductServiceImpl.createProductWithImage` | 移除 `imageBaseUrl + relativePath` 拼接 |
| `ProductServiceImpl.updateProductWithImage` | 移除 `imageBaseUrl + relativePath` 拼接 |

> 注意：如果数据库中已有相对路径格式的历史数据，`getImageUrl` 中做一次兼容判断（已含相对路径前缀的跳过拼接），不改存量数据。

---

## 6. 测试策略

### 6.1 库存

- 测试 `ProductStockMapper.deductStock` 库存不足时返回 0 行
- 测试 `InventoryService` 委托后的行为与旧实现一致

### 6.2 图片

- 测试 `ImageStorageService.validateImage` 各种校验场景
- 测试图片 URL 格式变更后的一致性

### 6.3 回归

- 全量运行 147 个测试，确认零失败

---

## 7. 迁移计划

### Phase 1: 库存统一
1. 新建 `ProductStockMapper`
2. 新建 `InventoryService` 接口+实现
3. 改造 `ProductReservationServiceImpl` 使用新 Mapper
4. 改造 `ProductServiceImpl.restoreStock` 委托

### Phase 2: 图片校验 + URL 统一
1. 在 `ImageStorageService` 中增加 `validateImage`
2. 将 URL 拼接逻辑移入 `ImageStorageServiceImpl`
3. 清理 Controller 重复校验

### Phase 3: DTO 清理
1. 确认 DTO 状态，去掉 `@Deprecated` 或删除
2. 同步清理 `ProductConverter`
