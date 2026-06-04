# Bug 修复方案

## 修复项概览

| ID | 严重性 | 位置 | 描述 | 类型 |
|:--:|:------:|------|------|:----:|
| B1 | CRITICAL | `ProductServiceImpl.java:422` | `updateProductWithImage` 中 `setImageId` 被无条件覆盖 | 逻辑 Bug |
| B2 | MEDIUM | `InternalProductController.java:76` | 库存不足返回 body.code=500 而非 400 | API 设计 |
| E1 | LOW | 集成测试期望 | #2/#5/#14 期望 HTTP 400 应与代码行为 HTTP 404 对齐 | 测试对齐 |

---

## 1. B1 — `setImageId` 无条件覆盖（CRITICAL）

### 问题

`ProductServiceImpl.java:400-424` 的 `updateProductWithImage` 方法：

```java
if (imageUrl != null && !imageUrl.isBlank()) {
    if (existingProduct.getImageId() != null && existingProduct.getImageId() > 0) {
        // 更新已有图片 URL
        ProductImageInfo imageInfo = new ProductImageInfo();
        imageInfo.setId(existingProduct.getImageId());
        imageInfo.setUrl(imageUrl);
        productImageInfoMapper.updateUrl(imageInfo);
    } else {
        // 插入新图片
        ProductImageInfo newImage = new ProductImageInfo();
        newImage.setUrl(imageUrl);
        productImageInfoMapper.insert(newImage);
        product.setImageId(newImage.getId());   // ✅ 第 418 行正确设置新 ID
    }
}

product.setImageId(existingProduct.getImageId());  // ❌ 第 422 行无条件覆盖！
```

当 `imageUrl` 不为空且原商品无图片时：
- 第 417-418 行正确插入新图片并设 `product.setImageId(newImage.getId())`
- 但第 422 行立即用 `existingProduct.getImageId()`（值为 null/0）覆盖
- **结果**：新插入的图片 ID 永远无法关联到商品，`products.image_id` 始终为 null/0

### 修复方案

将第 422 行移到 `else` 分支中，确保仅在无新图片时才保留原图片 ID：

```java
if (imageUrl != null && !imageUrl.isBlank()) {
    if (existingProduct.getImageId() != null && existingProduct.getImageId() > 0) {
        ProductImageInfo imageInfo = new ProductImageInfo();
        imageInfo.setId(existingProduct.getImageId());
        imageInfo.setUrl(imageUrl);
        productImageInfoMapper.updateUrl(imageInfo);
    } else {
        ProductImageInfo newImage = new ProductImageInfo();
        newImage.setUrl(imageUrl);
        productImageInfoMapper.insert(newImage);
        product.setImageId(newImage.getId());
    }
} else {
    product.setImageId(existingProduct.getImageId());  // 移到 else 中
}
```

### 验证方法

- 单元测试：`ProductServiceImplTest` 新增 `testUpdateProductWithNewImage_ExistingProductWithoutImage`，验证 updateProductWithImage 传入新 imageUrl 且原商品无图片时：
  - `productImageInfoMapper.insert()` 被调用
  - `productMapper.updateProduct()` 的参数 `imageId` 等于新插入行的 ID（非 null/0）
- 集成测试：直连 API `PUT /api/seller/product/{id}` → 验证返回后 `GET /api/seller/product/{id}` 的 `imageUrl` 非空

---

## 2. B2 — 库存不足返回 code=500（MEDIUM）[已解决]

### 状态

`POST /internal/product/deduct-stock` 端点已被整体删除（无 Feign 调用方），该问题随端点删除自动解决。

---

## 3. E1 — 集成测试期望对齐（LOW）

### 问题

`GlobalExceptionHandler.java:25` 将 `ProductException(404, ...)` 映射为 **HTTP 404**：

```java
int httpStatus = e.getCode() >= 500 ? 500 : (e.getCode() >= 400 ? e.getCode() : 400);
```

但集成测试 #2/#5/#14 期望 **HTTP 400**，与代码行为不一致。

### 修复方案

更新集成测试期望 / 文档记录：
- #2 按ID查询商品详情(id=1) → 期望 HTTP 404 "商品不存在"
- #5 查询不存在商品(id=99999) → 期望 HTTP 404 "商品不存在"
- #14 删除不存在商品(id=99999) → 期望 HTTP 404 "商品不存在: 99999"

（仅修正测试文档期望值，不改代码。`GlobalExceptionHandler` 行为正确。）

---

## 4. 实施顺序

| 优先级 | 任务 | 依赖 | 预计工时 |
|:------:|------|:----:|:--------:|
| P0 | B1: `setImageId` 无条件覆盖修复 | 无 | 5 min |
| P0 | B1 单元测试验证 | B1 修复后 | 10 min |
| P1 | B2: 库存不足 code=400 | 无 | 1 min |
| P1 | 运行全部单元测试确认无回归 | B1+B2 修复后 | 2 min |
| P2 | E1: 更新 API接口集成测试报告.md 期望值 | 无 | 5 min |
