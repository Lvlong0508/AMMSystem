# Bug 修复实施方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 `product-service` 中审计发现的 3 个问题（B1 CRITICAL、B2 MEDIUM、E1 LOW）

**Architecture:** product-service 单模块修改，不涉及跨服务。B1 修复 `ProductServiceImpl.updateProductWithImage` 中 `setImageId` 被无条件覆盖的逻辑 Bug；B2 修复 `InternalProductController.deductStock` 返回错误码；E1 更新集成测试报告的期望值。

**Tech Stack:** Java 17, Spring Boot 3, MyBatis, JUnit 5

---

### Task 1: B1 — 修复 `setImageId` 无条件覆盖

**Files:**
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java:400-424`
- Test: `product-service/src/test/java/com/gzasc/aishopping/product/service/ProductServiceImplTest.java`

- [ ] **Step 1: 写失败的测试（TDD：先看到红色）**

在 `ProductServiceImplTest.java` 末尾新增：

```java
@Test
void testUpdateProductWithNewImage_ExistingProductWithoutImage() {
    Product existingProduct = new Product();
    existingProduct.setId(1L);
    existingProduct.setShopId(100L);
    existingProduct.setName("测试商品");
    existingProduct.setPrice(BigDecimal.valueOf(99.00));
    existingProduct.setStock(10);

    when(productMapper.selectProductById(1L)).thenReturn(existingProduct);

    doAnswer(invocation -> {
        ProductImageInfo arg = invocation.getArgument(0);
        arg.setId(999L);
        return 1;
    }).when(productImageInfoMapper).insert(any());

    when(productMapper.updateProduct(any())).thenReturn(1);

    Product updateData = new Product();
    updateData.setId(1L);
    updateData.setName("更新后");

    int result = productService.updateProductWithImage(updateData, "http://new-image.jpg");

    ArgumentCaptor<ProductImageInfo> imageCaptor = ArgumentCaptor.forClass(ProductImageInfo.class);
    verify(productImageInfoMapper).insert(imageCaptor.capture());
    assertEquals("http://new-image.jpg", imageCaptor.getValue().getUrl());

    ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
    verify(productMapper).updateProduct(productCaptor.capture());
    assertNotNull(productCaptor.getValue().getImageId());
    assertEquals(Long.valueOf(999L), productCaptor.getValue().getImageId());

    assertEquals(1, result);
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
cd AI-Shopping-backend_Eureka
mvn test -pl product-service -Dtest="ProductServiceImplTest#testUpdateProductWithNewImage_ExistingProductWithoutImage" -DfailIfNoTests=false
```

预期：测试失败。因为当前 buggy 代码第 422 行用 `existingProduct.getImageId()`（null）覆盖了 `newImage.getId()`（999L），`productCaptor.getValue().getImageId()` 断言 `assertNotNull` 失败。

- [ ] **Step 3: 实现修复**

修改 `ProductServiceImpl.java:408-423`：

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
    product.setImageId(existingProduct.getImageId());
}
```

即将第 422 行 `product.setImageId(existingProduct.getImageId())` 移到 `if (imageUrl != null && !imageUrl.isBlank())` 的 `else` 分支中。

- [ ] **Step 4: 运行测试，确认通过**

```bash
mvn test -pl product-service -Dtest="ProductServiceImplTest" -DfailIfNoTests=false
```

预期：26/26 通过（原 25 + 新增 1）。

- [ ] **Step 5: Commit**

```bash
git add product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java
git add product-service/src/test/java/com/gzasc/aishopping/product/service/ProductServiceImplTest.java
git commit -m "fix: updateProductWithImage中setImageId被无条件覆盖(B1 CRITICAL)"
```

---

### Task 2: B2 — 库存不足返回 code=400（已解决：端点已删除）

> `POST /internal/product/deduct-stock` 端点已被整体删除（无 Feign 调用方），无需单独修复。

---

### Task 3: E1 — 更新集成测试报告期望值

**Files:**
- Modify: `product-service/API接口集成测试报告.md`

- [ ] **Step 1: 更新 #2/#5/#14 的预期结果**

在 `API接口集成测试报告.md` 第 66、69、84 行（表格 #2、#5、#14），将"预期结果"列中的 `400 "商品不存在"` 改为 `404 "商品不存在"`：

- #2: 预期结果 `404 "商品不存在"`
- #5: 预期结果 `404 "商品不存在"`
- #14: 预期结果 `404 "商品不存在: 99999"`

- [ ] **Step 2: 更新 #19 的预期结果标注 HTTP 状态**

在表格 #19 的"预期结果"列标注 HTTP 200：

- #19: 预期结果 `HTTP 200 + body.code=500 "恢复失败"`

> ~~#18/#20~~ 已删除：`deductStock` 端点已移除。

- [ ] **Step 3: Commit**

```bash
git add product-service/API接口集成测试报告.md
git commit -m "docs: 对齐集成测试报告期望值与实际代码行为(E1)"
```

---

## 自检清单

| Spec 需求 | 对应 Task | 覆盖情况 |
|-----------|:---------:|:--------:|
| B1: `setImageId` 无条件覆盖修复 | Task 1 | ✅ |
| B1 单元测试验证 | Task 1 Step 1-4 | ✅ |
| B2: 库存不足返回 code=400 | Task 2（端点已删除） | ✅ |
| E1: 集成测试报告期望对齐 | Task 3 | ✅ |
