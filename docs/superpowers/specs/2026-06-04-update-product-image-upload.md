# updateProduct 图片上传改造设计

## 概述

将 `updateProduct` 流程对齐 `createProduct`，支持 MultipartFile 图片上传 + 本地存储，更新后异步删除旧图。

## 后端变更

### 1. Controller — `ProductSellerController.java`

- `@PutMapping("/{productId}")` 改为 `consumes = MediaType.MULTIPART_FORM_DATA_VALUE`
- 参数改为 `@RequestPart("product") @Valid UpdateProductRequest` + `@RequestPart(value = "image", required = false) MultipartFile`
- 若 image 非空，校验 content-type 仅允许 `image/jpeg` / `image/png`
- 空 image 走纯文本更新（保留原图）

### 2. DTO — `UpdateProductRequest.java`

- 移除 `imageUrl` 字段
- 仅保留：`name` / `description` / `price` / `stock`

### 3. Service 接口 — `ProductService.java`

- `updateProductWithImage(Product, String)` → `updateProductWithImage(Product, @Nullable MultipartFile)`

### 4. Service 实现 — `ProductServiceImpl.java`（核心）

`updateProductWithImage` 新流程：

```
1. selectProductById → 商品不存在则抛 404
2. if image != null && !image.isEmpty():
   a. imageStorageService.saveImage(productId, image) → 存新图，获得 relativePath
   b. fullUrl = imageBaseUrl + relativePath
   c. 获取旧图片 URL: existingProduct.getImageId() → getImageUrl(imageId)（复用已有方法）
   d. if 老 imageId 存在: productImageInfoMapper.updateUrl(new ProductImageInfo(老 imageId, fullUrl))
      else: productImageInfoMapper.insert(new ProductImageInfo(url=fullUrl)) → product.setImageId(新 id)
   e. DB 更新成功后 → 若 oldImageUrl != null 且非 DEFAULT_IMAGE_URL, 异步 imageStorageService.deleteImage(oldImageUrl)
3. else (image 为空):
   product.setImageId(existingProduct.getImageId())  # 保留原图
4. productMapper.updateProduct(product)
```

### 5. ImageStorageService 新增接口

```java
/**
 * 删除商品图片文件（异步）
 * @param imageUrl 完整的图片 URL（含 baseUrl 前缀）
 */
void deleteImage(String imageUrl);
```

### 6. ImageStorageServiceImpl 实现

- `@Async` 异步执行
- 从 imageUrl 中剥离 `imageBaseUrl` 前缀得到 relativePath
- 以 `storagePath` 为根解析文件路径 → `Files.deleteIfExists()`
- 日志记录删除结果

### 7. 配置变更

- 启动类或 `@Configuration` 类加 `@EnableAsync`

## 涉及文件清单

| 文件 | 变更类型 |
|------|----------|
| `product-service/.../controller/ProductSellerController.java` | 修改 |
| `product-service/.../dto/UpdateProductRequest.java` | 修改（移除 imageUrl） |
| `product-service/.../service/ProductService.java` | 修改方法签名 |
| `product-service/.../service/impl/ProductServiceImpl.java` | 修改实现 |
| `product-service/.../service/ImageStorageService.java` | 新增 deleteImage |
| `product-service/.../service/impl/ImageStorageServiceImpl.java` | 新增异步 deleteImage |
| `product-service/.../ProductServiceApplication.java`（或配置类） | 加 @EnableAsync |
| `product-service/.../controller/ProductSellerControllerTest.java` | 适配测试 |
| `product-service/.../service/ProductServiceImplTest.java` | 适配/追加测试 |

## 不变内容

- 前端（ShopProducts.vue / product.js / shop.js）本次不改动
- createProduct 流程不变
- 查询、删除、上下架等其余端点不变
- ProductMapper / ProductImageInfoMapper SQL 不变
