# Product Service Bug 修复设计文档

## 概述

基于 API 接口集成测试报告发现的 17 个未修复 Bug，按严重性分三阶段修复。涵盖 SQL 错误、API 规范、数据安全、边界条件、代码整洁度。

## 影响范围

| 模块 | 文件 |
|------|------|
| Mapper | `ProductImageInfoMapper.java`, `ProductMapper.java`, `SalableProductMapper.java`, `ProductReservationMapper.java` |
| Service | `ProductServiceImpl.java`, `ProductReservationServiceImpl.java` |
| Controller | `ProductSellerController.java`, `ProductUserController.java`, `InternalProductController.java` |
| 异常处理 | `GlobalExceptionHandler.java`, `ProductException.java` |
| Model | `Product.java` |

## Phase 1 (P0) — CRITICAL + HIGH

### C1: selectByIds 缺少 id 字段

**现状**: `ProductImageInfoMapper.java:21-27` — SQL 为 `SELECT url`，未选 `id` 字段。
`buildImageUrlMap` 调用 `.collect(Collectors.toMap(ProductImageInfo::getId, ...))` 时 `getId()` 返回默认 0，多条记录 key 冲突抛 `DuplicateKeyException`。

**修复**: SQL 改为 `SELECT id, url`。

### N8/N9: 用户端搜索/价格区间未过滤 is_sale

**现状**: `ProductServiceImpl.java:120-131` (`getProductsByName`) 和 `ProductServiceImpl.java:305-317` (`getProductsByPriceRange`) 直接从 `products` 表查询，未关联 `salable_products` 表或过滤 `is_sale=true`。

**修复**: 在 Service 层查询后增加 `stream().filter(Product::isSale)` 过滤。注意：已有单元测试需同步更新。

### N7: getProductsByShopId page=0 负偏移

**现状**: `ProductServiceImpl.java:228-229` — `(page - 1) * size`，当 `page=0` 时产生负 `OFFSET`，SQL 异常。

**修复**: 改为 `(page > 0 ? (page - 1) * size : 0)`。同时补充单元测试覆盖 `page=0` 边界。

### N2: 内部 createProduct 缺 @Valid

**现状**: `InternalProductController.java:100-107` — 直接接收 `Product` 实体而非 DTO，调用方可设置任意字段（如 `id`、`isSale`、`shopId`），无 `@Valid` 校验。

**修复**: 新增 `InternalCreateProductRequest` DTO，只暴露必要字段（name, price, tags, description, stock），加 `@Valid` 注解。

### T7: 更新 API 500

**现状**: `ProductSellerController.java:66-83` — `UpdateProductRequest` 中未提供的字段在 `Product` 中为 null，`updateProduct` 的 SQL 全字段覆盖写入 null → 数据库非空约束导致 500。

**修复**: Mapper 层 `updateProduct` 改为动态 SQL (`<set>`)，只更新非 null 字段。同时修复 N5（见 Phase 2）。

## Phase 2 (P1) — MEDIUM

| ID | 文件 | 修复 |
|----|------|------|
| C4 | `GlobalExceptionHandler.java:18-23` | 移除 `@ResponseStatus` 硬编码，改为根据 `ProductException.getCode()` 动态判断 HTTP 状态码 |
| N1 | `InternalProductController.java:49-55` | `/internal/product/batch` 返回值改为 `ApiResponse<List<ProductWithImageAbstractDTO>>` |
| N3 | `InternalProductController.java` | `deduct-stock`、`restore-stock`、`reserve-stock` 增加 `@Valid` + DTO 校验 |
| N6 | `ProductDTO.java:16`, `InternalProductController.java:38` | `ProductDTO.price` 类型 `Double`→`BigDecimal`，消除跨服务精度损失；`InternalProductController` 移除 `.doubleValue()` 强制转换 |
| N5 | `ProductServiceImpl.java:381-387` | 无现有图片时走 insert 新图片分支 |
| N10 | `InternalProductController.java:89-97,119-128` | `confirm-reservation`/`release-reservation` 改为 `@RequestBody` |

## Phase 3 (P2) — LOW

| ID | 文件 | 修复 |
|----|------|------|
| N4 | `ProductReservationMapper.java:28-36` | `productId` 类型统一为 `Long` |
| N11 | `ProductServiceImpl.java:179-189` | `deleteProduct` 补充删除关联图片 |
| N12 | `InternalProductController.java:30-46` | `getProductById` 补充 `imageUrl` 和 `shop` 信息 |
| N13 | `SalableProductMapper.java:19-20` | 分页大小参数化 |
| N14 | `ProductReservationServiceImpl.java` | 预占不足时 code=409 |
| N15 | `Product.java:20` | `boolean isSale` → `Boolean isSale` 或 getter 改为 `isSale()`→`getIsSale()` |

## 执行顺序

```
Phase 1 (P0) → 单元测试验证 → Phase 2 (P1) → 单元测试验证 → Phase 3 (P2) → 全量测试
```

每个 Phase 内部可以并行修复无依赖的 Bug。
