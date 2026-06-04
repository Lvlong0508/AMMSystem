# updateProduct 图片上传改造 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 `updateProduct` 端点改为 multipart 图片上传，对齐 `createProduct`，更新后异步删除旧图。

**Architecture:** 复用 `ImageStorageService.saveImage` 存新图，新增 `deleteImage` 异步删旧图。Service 层用 `getImageUrl(existingProduct.getImageId())` 获取旧 URL。Controller 改为 `consumes = MULTIPART_FORM_DATA_VALUE`，image 可选。

**Tech Stack:** Spring Boot 3, MyBatis, Mockito, JUnit 5

---

### Task 1: ImageStorageService 新增 deleteImage + 异步实现 + @EnableAsync

**Files:**
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/service/ImageStorageService.java`
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ImageStorageServiceImpl.java`
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/ProductServiceApplication.java`

- [ ] **Step 1: ImageStorageService 接口新增 deleteImage**

```java
// ImageStorageService.java — 在 saveImage 后新增
void deleteImage(String imageUrl);
```

- [ ] **Step 2: ImageStorageServiceImpl 实现异步 deleteImage**

`ImageStorageServiceImpl.java` — 新增 `imageBaseUrl` 注入和 `deleteImage`：

```java
// 在 storagePath 字段下新增：
@Value("${app.image.base-url}")
private String imageBaseUrl;

// 新增方法：
@Async
@Override
public void deleteImage(String imageUrl) {
    if (imageUrl == null || imageUrl.isBlank()) {
        return;
    }
    String relativePath = imageUrl.replace(imageBaseUrl, "");
    if (relativePath.startsWith("/")) {
        relativePath = relativePath.substring(1);
    }
    Path filePath = storagePath.resolve(relativePath).normalize();
    try {
        boolean deleted = Files.deleteIfExists(filePath);
        if (deleted) {
            log.info("删除旧图片成功: {}", filePath);
        } else {
            log.warn("旧图片文件不存在: {}", filePath);
        }
    } catch (IOException e) {
        log.error("删除旧图片失败: {}", filePath, e);
    }
}
```

- [ ] **Step 3: ProductServiceApplication 加 @EnableAsync**

```java
// ProductServiceApplication.java — 在 @EnableScheduling 下新增
import org.springframework.scheduling.annotation.EnableAsync;
// 在类上添加：
@EnableAsync
```

---

### Task 2: UpdateProductRequest 移除 imageUrl 字段

**Files:**
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/dto/UpdateProductRequest.java`

- [ ] **Step 1: 从 UpdateProductRequest 中删除 imageUrl**

```java
// UpdateProductRequest.java — before
public class UpdateProductRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;  // ← 删除此行
}

// after
public class UpdateProductRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
}
```

---

### Task 3: ProductService 接口改方法签名

**Files:**
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/service/ProductService.java`

- [ ] **Step 1: 修改 updateProductWithImage 签名**

```java
// ProductService.java — 修改前：
int updateProductWithImage(Product product, String imageUrl);

// 修改后：
int updateProductWithImage(Product product, MultipartFile image);
```

同时删除 `imageUrl` 参数的 Javadoc，更新为：

```java
/**
 * 更新商品并处理图片（同一事务）
 * @param product 商品基本信息（需包含ID）
 * @param image 新图片文件，为空则不更新图片
 * @return 影响的行数
 */
int updateProductWithImage(Product product, MultipartFile image);
```

---

### Task 4: ProductServiceImpl 改造 updateProductWithImage

**Files:**
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java`

- [ ] **Step 1: 重写 updateProductWithImage 方法**

```java
@Override
@Transactional
public int updateProductWithImage(Product product, MultipartFile image) {
    Product existingProduct = productMapper.selectProductById(product.getId());
    if (existingProduct == null) {
        throw new ProductException(404, "商品不存在: " + product.getId());
    }

    if (image != null && !image.isEmpty()) {
        String relativePath = imageStorageService.saveImage(product.getId(), image);
        String fullUrl = imageBaseUrl + relativePath;

        Integer oldImageId = existingProduct.getImageId();
        String oldImageUrl = (oldImageId != null && oldImageId > 0) ? getImageUrl(oldImageId) : null;

        if (oldImageId != null && oldImageId > 0) {
            ProductImageInfo imageInfo = new ProductImageInfo();
            imageInfo.setId(oldImageId);
            imageInfo.setUrl(fullUrl);
            productImageInfoMapper.updateUrl(imageInfo);
        } else {
            ProductImageInfo newImage = new ProductImageInfo();
            newImage.setUrl(fullUrl);
            productImageInfoMapper.insert(newImage);
            product.setImageId(newImage.getId());
        }

        if (oldImageUrl != null && !DEFAULT_IMAGE_URL.equals(oldImageUrl)) {
            imageStorageService.deleteImage(oldImageUrl);
        }
    } else {
        product.setImageId(existingProduct.getImageId());
    }

    return productMapper.updateProduct(product);
}
```

- [ ] **Step 2: 确保 getImageUrl 是可访问的私有方法**

确认 `getImageUrl(Integer imageId)` 方法已存在 (`ProductServiceImpl.java:96`)，无需修改。

---

### Task 5: ProductSellerController 改造 updateProduct 端点

**Files:**
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/controller/ProductSellerController.java`

- [ ] **Step 1: 修改 updateProduct 方法**

```java
// ProductSellerController.java — 修改前：
@PutMapping("/{productId}")
public ApiResponse<Void> updateProduct(
        @PathVariable("productId") Long productId,
        @RequestBody @Valid UpdateProductRequest request) {
    log.info("更新商品, productId={}", productId);
    Product product = new Product();
    product.setId(productId);
    if (request.getName() != null) product.setName(request.getName());
    if (request.getDescription() != null) product.setDescription(request.getDescription());
    if (request.getPrice() != null) product.setPrice(request.getPrice());
    if (request.getStock() != null) product.setStock(request.getStock());

    int result = productService.updateProductWithImage(product, request.getImageUrl());
    if (result > 0) {
        return ApiResponse.success("更新商品成功", null);
    }
    throw new ProductException(404, "商品不存在");
}

// 修改后：
@PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ApiResponse<Void> updateProduct(
        @PathVariable("productId") Long productId,
        @RequestPart("product") @Valid UpdateProductRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image) {
    log.info("更新商品, productId={}", productId);

    if (image != null && !image.isEmpty()) {
        String contentType = image.getContentType();
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            throw new ProductException(400, "仅支持 JPG 和 PNG 格式");
        }
    }

    Product product = new Product();
    product.setId(productId);
    if (request.getName() != null) product.setName(request.getName());
    if (request.getDescription() != null) product.setDescription(request.getDescription());
    if (request.getPrice() != null) product.setPrice(request.getPrice());
    if (request.getStock() != null) product.setStock(request.getStock());

    int result = productService.updateProductWithImage(product, image);
    if (result > 0) {
        return ApiResponse.success("更新商品成功", null);
    }
    throw new ProductException(404, "商品不存在");
}
```

---

### Task 6: ProductServiceImplTest 适配测试

**Files:**
- Modify: `product-service/src/test/java/com/gzasc/aishopping/product/service/ProductServiceImplTest.java`

- [ ] **Step 1: 修改 testUpdateProductWithImageSuccess — 改为 MultipartFile 参数**

```java
@Test
@DisplayName("PR-020 - 更新商品名称和价格（含新图片）")
void testUpdateProductWithImageSuccess() {
    Product existing = new Product();
    existing.setId(2001L);
    existing.setName("旧名称");
    existing.setImageId(1);
    when(productMapper.selectProductById(2001L)).thenReturn(existing);
    when(imageStorageService.saveImage(anyLong(), any(MultipartFile.class)))
            .thenReturn("/image/goods/main/2001/2001_abc.jpg");
    when(productImageInfoMapper.selectURLById(1)).thenReturn(new ProductImageInfo(1, "http://localhost:8081/image/goods/main/2001/old.jpg"));
    when(productImageInfoMapper.updateUrl(any(ProductImageInfo.class))).thenReturn(1);
    when(productMapper.updateProduct(any(Product.class))).thenReturn(1);

    Product update = new Product();
    update.setId(2001L);
    update.setName("新名称");
    update.setPrice(BigDecimal.valueOf(199));
    MockMultipartFile imageFile = new MockMultipartFile("image", "new.jpg", "image/jpeg", "new-image-content".getBytes());

    int result = productService.updateProductWithImage(update, imageFile);

    assertEquals(1, result);
    verify(imageStorageService).saveImage(eq(2001L), any(MultipartFile.class));
    verify(productImageInfoMapper).updateUrl(any(ProductImageInfo.class));
    verify(productMapper).updateProduct(any(Product.class));
    verify(imageStorageService).deleteImage("http://localhost:8081/image/goods/main/2001/old.jpg");
}
```

- [ ] **Step 2: 修改 testUpdateProductNotFound — 改为 MultipartFile 参数**

```java
@Test
@DisplayName("PR-023 - 更新不存在的商品")
void testUpdateProductNotFound() {
    when(productMapper.selectProductById(99999L)).thenReturn(null);

    Product update = new Product();
    update.setId(99999L);
    update.setName("新名称");
    ProductException exception = assertThrows(ProductException.class,
            () -> productService.updateProductWithImage(update, null));
    assertTrue(exception.getMessage().contains("不存在"));
}
```

- [ ] **Step 3: 修改 testUpdateProductWithNewImage_ExistingProductWithoutImage — 改为 MultipartFile**  (原 B1 测试)

```java
@Test
@DisplayName("B1 - updateProductWithImage 传入新 image 且原商品无图片时，imageId 应正确关联")
void testUpdateProductWithNewImage_ExistingProductWithoutImage() {
    Product existingProduct = new Product();
    existingProduct.setId(1L);
    existingProduct.setShopId(100L);
    existingProduct.setName("测试商品");
    existingProduct.setPrice(BigDecimal.valueOf(99.00));
    existingProduct.setStock(10);
    // imageId = null — 无旧图片

    when(productMapper.selectProductById(1L)).thenReturn(existingProduct);
    when(imageStorageService.saveImage(anyLong(), any(MultipartFile.class)))
            .thenReturn("/image/goods/main/1/1_new.jpg");

    doAnswer(invocation -> {
        ProductImageInfo arg = invocation.getArgument(0);
        arg.setId(999);
        return 1;
    }).when(productImageInfoMapper).insert(any());

    when(productMapper.updateProduct(any())).thenReturn(1);

    Product updateData = new Product();
    updateData.setId(1L);
    updateData.setName("更新后");
    MockMultipartFile imageFile = new MockMultipartFile("image", "new.jpg", "image/jpeg", "content".getBytes());

    int result = productService.updateProductWithImage(updateData, imageFile);

    ArgumentCaptor<ProductImageInfo> imageCaptor = ArgumentCaptor.forClass(ProductImageInfo.class);
    verify(productImageInfoMapper).insert(imageCaptor.capture());
    assertEquals("http://localhost:8081/image/goods/main/1/1_new.jpg", imageCaptor.getValue().getUrl());

    ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
    verify(productMapper).updateProduct(productCaptor.capture());
    assertNotNull(productCaptor.getValue().getImageId());
    assertEquals(Integer.valueOf(999), productCaptor.getValue().getImageId());

    // 无旧图片，不应调用 deleteImage
    verify(imageStorageService, never()).deleteImage(anyString());

    assertEquals(1, result);
}
```

- [ ] **Step 4: 新增测试 — updateProduct 纯文本更新（不传图片）**

```java
@Test
@DisplayName("updateProduct - 纯文本更新不传图片，保留原图")
void testUpdateProductWithoutImage() {
    Product existing = new Product();
    existing.setId(3001L);
    existing.setName("旧名称");
    existing.setImageId(5);
    when(productMapper.selectProductById(3001L)).thenReturn(existing);
    when(productMapper.updateProduct(any(Product.class))).thenReturn(1);

    Product update = new Product();
    update.setId(3001L);
    update.setName("新名称");
    update.setPrice(BigDecimal.valueOf(299));

    int result = productService.updateProductWithImage(update, null);

    assertEquals(1, result);
    verify(productMapper).updateProduct(productCaptor.capture());
    assertEquals(Integer.valueOf(5), productCaptor.getValue().getImageId());
    verify(imageStorageService, never()).saveImage(anyLong(), any(MultipartFile.class));
    verify(imageStorageService, never()).deleteImage(anyString());
}
```

- [ ] **Step 5: 运行 ProductServiceImplTest 验证通过**

Run: `mvn test -pl product-service -Dtest=ProductServiceImplTest -am`
Expected: All tests PASS

---

### Task 7: ProductSellerControllerTest 适配测试

**Files:**
- Modify: `product-service/src/test/java/com/gzasc/aishopping/product/controller/ProductSellerControllerTest.java`

- [ ] **Step 1: 修改 testUpdateProductSuccess — 改为 multipart 请求**

```java
@Test
@DisplayName("PR-020 - PUT /api/seller/product/{productId} - 更新商品（含图片）")
void testUpdateProductSuccess() throws Exception {
    when(productService.updateProductWithImage(any(Product.class), any(MultipartFile.class))).thenReturn(1);

    UpdateProductRequest request = new UpdateProductRequest();
    request.setName("新名称");
    request.setPrice(BigDecimal.valueOf(199));

    MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "fake-image-content".getBytes(StandardCharsets.UTF_8));
    MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/seller/product/2001")
                    .file(productPart)
                    .file(imageFile)
                    .with(requestPut -> {
                        requestPut.setMethod("PUT");
                        return requestPut;
                    }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}
```

- [ ] **Step 2: 修改 testUpdateProductNotFound — 改为 multipart 请求**

```java
@Test
@DisplayName("PR-023 - PUT /api/seller/product/{productId} - 商品不存在")
void testUpdateProductNotFound() throws Exception {
    when(productService.updateProductWithImage(any(Product.class), any(MultipartFile.class))).thenReturn(0);

    UpdateProductRequest request = new UpdateProductRequest();
    request.setName("新名称");

    MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/seller/product/99999")
                    .file(productPart)
                    .with(requestPut -> {
                        requestPut.setMethod("PUT");
                        return requestPut;
                    }))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404));
}
```

- [ ] **Step 3: 新增测试 — 更新商品不传图片（纯文本）**

```java
@Test
@DisplayName("updateProduct - 更新商品不传图片")
void testUpdateProductWithoutImage() throws Exception {
    when(productService.updateProductWithImage(any(Product.class), isNull())).thenReturn(1);

    UpdateProductRequest request = new UpdateProductRequest();
    request.setName("纯文本更新");

    MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/seller/product/2001")
                    .file(productPart)
                    .with(requestPut -> {
                        requestPut.setMethod("PUT");
                        return requestPut;
                    }))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}
```

- [ ] **Step 4: 新增测试 — 更新商品传非法格式图片**

```java
@Test
@DisplayName("updateProduct - 图片格式不支持")
void testUpdateProductWithInvalidImageFormat() throws Exception {
    UpdateProductRequest request = new UpdateProductRequest();
    request.setName("新名称");
    request.setPrice(BigDecimal.valueOf(199));

    MockMultipartFile imageFile = new MockMultipartFile("image", "test.gif", "image/gif", "fake-gif-content".getBytes(StandardCharsets.UTF_8));
    MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/seller/product/2001")
                    .file(productPart)
                    .file(imageFile)
                    .with(requestPut -> {
                        requestPut.setMethod("PUT");
                        return requestPut;
                    }))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
}
```

- [ ] **Step 5: 运行 ProductSellerControllerTest 验证通过**

Run: `mvn test -pl product-service -Dtest=ProductSellerControllerTest -am`
Expected: All tests PASS

---

### Task 8: 全量运行验证

**Files:** 无需修改

- [ ] **Step 1: 运行 product-service 所有测试**

Run: `mvn test -pl product-service -am`
Expected: All tests pass, including ProductServiceImplTest and ProductSellerControllerTest

---

## Self-Review Checklist

1. **Spec coverage:** 
   - Controller multipart 改造 → Task 5 ✓
   - DTO 移除 imageUrl → Task 2 ✓
   - Service 接口签名变更 → Task 3 ✓
   - Service 实现：存储新图 → 更新DB → 异步删旧图 → Task 4 ✓
   - deleteImage 异步实现 → Task 1 ✓
   - @EnableAsync → Task 1 ✓
   - 图片格式校验 → Task 5 ✓

2. **Placeholder scan:** 无 TBD/TODO ✓

3. **Type consistency:**
   - `updateProductWithImage(Product, MultipartFile)` — 接口与实现一致 ✓
   - `deleteImage(String imageUrl)` — 声明一致 ✓
   - `getImageUrl(Integer imageId)` → `String` — 返回值类型匹配 ✓
