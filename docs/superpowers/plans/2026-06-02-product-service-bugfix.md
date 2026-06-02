# Product Service Bug 修复实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 product-service 的 17 个已知 Bug，分三阶段（P0→P1→P2），每阶段后运行 `mvn test` 验证。

**Architecture:** product-service 基于 Spring Boot + MyBatis + MySQL。Mapper 层用 `@Select`/`@Update` 注解写 SQL，Service 层处理业务逻辑，Controller 层暴露 REST API。Bug 分布在所有三层及跨服务 DTO。

**Tech Stack:** Java 17, Spring Boot 3.x, MyBatis, MySQL, JUnit 5 + Mockito

---

## 文件影响总览

| 文件 | 变更类型 | 相关 Bug |
|------|----------|----------|
| `ProductImageInfoMapper.java` | 修改 | C1 |
| `ProductMapper.java` | 修改 | N7, T7 |
| `SalableProductMapper.java` | 修改 | N13 |
| `ProductReservationMapper.java` | 修改 | N4 |
| `ProductServiceImpl.java` | 修改 | N5, N7, N8/N9, N11 |
| `ProductReservationServiceImpl.java` | 修改 | N14 |
| `ProductSellerController.java` | 修改 | T7 |
| `ProductUserController.java` | 无需修改 | (Service 层修复即可) |
| `InternalProductController.java` | 修改 | N1, N2, N3, N6, N10, N12 |
| `GlobalExceptionHandler.java` | 修改 | C4 |
| `ProductException.java` | 无需修改 | |
| `Product.java` | 修改 | N15 |
| `ProductDTO.java` (common-api) | 修改 | N6 |
| `CreateProductRequest.java` | 无需修改 | |
| `UpdateProductRequest.java` | 无需修改 | |
| `ProductWithImageAbstractDTO.java` | 修改 | N12 |
| `ProductWithImageDetailDTO.java` | 修改 | N12 |
| `ProductServiceImplTest.java` | 修改 | 新增测试覆盖 |

---

## Phase 1 — P0 (CRITICAL + HIGH)

### Task 1: C1 — 修复 selectByIds SQL 缺少 id 字段

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/mapper/ProductImageInfoMapper.java:21-27`

- [ ] **Step 1: 修改 SQL 增加 id 字段**

```java
// ProductImageInfoMapper.java 第22行
// 改前:
@Select("<script>" +
        "SELECT url FROM product_images WHERE id IN " +
// 改后:
@Select("<script>" +
        "SELECT id, url FROM product_images WHERE id IN " +
```

- [ ] **Step 2: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -Dtest=ProductImageInfoMapperTest -DfailIfNoTests=false`
Expected: BUILD SUCCESS, selectByIds_shouldReturnImages 通过

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/mapper/ProductImageInfoMapper.java
git commit -m "fix(C1): selectByIds SQL 补全 id 字段避免 Collectors.toMap 抛异常"
```

---

### Task 2: N8/N9 — 用户端搜索/价格区间过滤 is_sale

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java:120-131, 305-317`
- 修改: `product-service/src/test/java/com/gzasc/aishopping/product/service/ProductServiceImplTest.java`

- [ ] **Step 1: 修改 getProductsByName 增加 is_sale 过滤**

```java
// ProductServiceImpl.java 第120-132行
// 改前:
@Override
public List<ProductWithImageDetailDTO> getProductsByName(String name) {
    List<Product> products = productMapper.selectProductsByName(name);
    if (products.isEmpty()) {
        return List.of();
    }
    Map<Integer, String> imageUrlMap = buildImageUrlMap(products);

// 改后:
@Override
public List<ProductWithImageDetailDTO> getProductsByName(String name) {
    List<Product> products = productMapper.selectProductsByName(name)
        .stream()
        .filter(Product::isSale)
        .toList();
    if (products.isEmpty()) {
        return List.of();
    }
    Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
```

- [ ] **Step 2: 修改 getProductsByPriceRange(BigDecimal, BigDecimal, int) 增加 is_sale 过滤**

```java
// ProductServiceImpl.java 第305-317行
// 改前:
@Override
public List<ProductWithImageAbstractDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page) {
    List<Product> products = productMapper.selectByPriceRangeWithPage(minPrice, maxPrice, page * 20);

// 改后:
@Override
public List<ProductWithImageAbstractDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page) {
    List<Product> products = productMapper.selectByPriceRangeWithPage(minPrice, maxPrice, page * 20)
        .stream()
        .filter(Product::isSale)
        .toList();
```

- [ ] **Step 3: 更新单元测试模拟 isSale**

在 ProductServiceImplTest.java 中：
```java
// testGetProductsByNameFound - 在product.setImageId(1)之后追加:
product.setSale(true);

// testGetProductsByPriceRangeWithResults - 在p.setImageId(1)之后追加:
p.setSale(true);
```

- [ ] **Step 4: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -Dtest=ProductServiceImplTest -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java
git add AI-Shopping-backend_Eureka/product-service/src/test/java/com/gzasc/aishopping/product/service/ProductServiceImplTest.java
git commit -m "fix(N8/N9): 用户端搜索/价格区间查询过滤 is_sale=true 防止看到下架商品"
```

---

### Task 3: N7 — 修复 getProductsByShopId page=0 负偏移

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java:228-229`
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/mapper/ProductMapper.java:67-68`

- [ ] **Step 1: 修改 Service 层 offset 计算**

```java
// ProductServiceImpl.java 第228-229行
// 改前:
public List<ProductWithImageAbstractDTO> getProductsByShopId(Long shopId, int page, int size) {
    List<Product> products = productMapper.selectByShopId(shopId, (page - 1) * size, size);
// 改后:
public List<ProductWithImageAbstractDTO> getProductsByShopId(Long shopId, int page, int size) {
    int offset = page > 0 ? (page - 1) * size : 0;
    List<Product> products = productMapper.selectByShopId(shopId, offset, size);
```

- [ ] **Step 2: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -Dtest=ProductServiceImplTest -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java
git commit -m "fix(N7): getProductsByShopId page=0 时修正负 OFFSET 问题"
```

---

### Task 4: N2 — 内部 createProduct 改用 DTO + @Valid

**文件:**
- 创建: `product-service/src/main/java/com/gzasc/aishopping/product/dto/InternalCreateProductRequest.java`
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java:100-107`

- [ ] **Step 1: 创建 InternalCreateProductRequest DTO**

```java
package com.gzasc.aishopping.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class InternalCreateProductRequest {
    @NotBlank(message = "商品名称不能为空")
    private String name;

    @Positive(message = "商品价格必须大于0")
    private BigDecimal price;

    private String tags;
    private String description;

    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;
}
```

- [ ] **Step 2: 修改 InternalProductController.createProduct 使用 DTO**

```java
// InternalProductController.java 第99-107行
// 改前:
@PostMapping("/create")
public ApiResponse<Map<String, Object>> createProduct(@RequestBody Product product) {
    int result = productService.createProduct(product);
    if (result > 0) {
        return ApiResponse.success("创建商品成功", Map.of("id", product.getId()));
    }
    return ApiResponse.error("创建商品失败");
}

// 改后:
@PostMapping("/create")
public ApiResponse<Map<String, Object>> createProduct(@RequestBody @Valid InternalCreateProductRequest request) {
    Product product = new Product();
    product.setName(request.getName());
    product.setPrice(request.getPrice());
    product.setTags(request.getTags());
    product.setDescription(request.getDescription());
    product.setStock(request.getStock() != null ? request.getStock() : 0);
    product.setSale(false);
    int result = productService.createProduct(product);
    if (result > 0) {
        return ApiResponse.success("创建商品成功", Map.of("id", product.getId()));
    }
    return ApiResponse.error("创建商品失败");
}
```

- [ ] **Step 3: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/dto/InternalCreateProductRequest.java
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java
git commit -m "fix(N2): 内部 createProduct 改用 InternalCreateProductRequest DTO + @Valid 校验"
```

---

### Task 5: T7 — 修复更新 API 全字段覆盖为 null 导致 500

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/mapper/ProductMapper.java:44-47`
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java:193-195`

- [ ] **Step 1: Mapper 层 updateProduct 改为动态 SQL**

```java
// ProductMapper.java 第44-47行
// 改前:
@Update("UPDATE products SET name = #{name}, price = #{price}, tags = #{tags}, " +
        "description = #{description}, stock = #{stock}, is_sale = #{isSale}, image_id = #{imageId}, shop_id = #{shopId}, updated_at = NOW() " +
        "WHERE id = #{id}")
int updateProduct(Product product);

// 改后:
@Update("<script>" +
        "UPDATE products SET updated_at = NOW()" +
        "<if test='name != null'>, name = #{name}</if>" +
        "<if test='price != null'>, price = #{price}</if>" +
        "<if test='tags != null'>, tags = #{tags}</if>" +
        "<if test='description != null'>, description = #{description}</if>" +
        "<if test='stock != null'>, stock = #{stock}</if>" +
        "<if test='imageId != null'>, image_id = #{imageId}</if>" +
        "<if test='shopId != null'>, shop_id = #{shopId}</if>" +
        " WHERE id = #{id}" +
        "</script>")
int updateProduct(Product product);
```

- [ ] **Step 2: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 3: 补充单元测试覆盖部分字段更新场景**

在 ProductMapperTest.java 的 UpdateTests 中追加：
```java
@Test
@DisplayName("updateProduct_shouldUpdatePartialFields")
void updateProduct_shouldUpdatePartialFields() {
    Product product = buildProduct();
    productMapper.insertProduct(product);

    Product update = new Product();
    update.setId(product.getId());
    update.setName("新名称");
    update.setPrice(BigDecimal.valueOf(99));
    // stock, description, tags 为 null — 应不覆盖
    int rows = productMapper.updateProduct(update);

    assertEquals(1, rows);
    Product reloaded = productMapper.selectProductById(product.getId());
    assertEquals("新名称", reloaded.getName());
    assertEquals(BigDecimal.valueOf(99), reloaded.getPrice());
    assertNotNull(reloaded.getStock()); // 未被覆盖为 null
}
```

- [ ] **Step 4: 运行包括 Mapper Test 的完整测试**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -Dtest=ProductMapperTest -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/mapper/ProductMapper.java
git add AI-Shopping-backend_Eureka/product-service/src/test/java/com/gzasc/aishopping/product/mapper/ProductMapperTest.java
git commit -m "fix(T7): updateProduct 改为动态 SQL 防止 null 字段覆盖导致 500"
```

---

## Phase 2 — P1 (MEDIUM)

### Task 6: C4 — 移除 @ResponseStatus 硬编码

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/controller/GlobalExceptionHandler.java:18-23`

- [ ] **Step 1: 修改 GlobalExceptionHandler 动态判断 HTTP 状态码**

```java
// GlobalExceptionHandler.java 第18-23行
// 改前:
@ExceptionHandler(ProductException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ApiResponse<Void> handleProductException(ProductException e) {
    log.warn("业务异常: {}", e.getMessage());
    return ApiResponse.error(e.getCode(), e.getMessage());
}

// 改后:
@ExceptionHandler(ProductException.class)
public ApiResponse<Void> handleProductException(ProductException e, HttpServletResponse response) {
    log.warn("业务异常: {}", e.getMessage());
    int httpStatus = e.getCode() >= 500 ? 500 : (e.getCode() >= 400 ? e.getCode() : 400);
    response.setStatus(httpStatus);
    return ApiResponse.error(e.getCode(), e.getMessage());
}
```

需要新增 import: `import jakarta.servlet.http.HttpServletResponse;`

- [ ] **Step 2: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/controller/GlobalExceptionHandler.java
git commit -m "fix(C4): 移除 @ResponseStatus 硬编码，根据业务 code 动态设置 HTTP 状态码"
```

---

### Task 7: N1 — /internal/product/batch 统一返回 ApiResponse

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java:48-55`

- [ ] **Step 1: 修改返回类型**

```java
// InternalProductController.java 第48-55行
// 改前:
@GetMapping("/batch")
public List<ProductWithImageAbstractDTO> getProductsByIds(@RequestParam("ids") String ids) {
    List<Long> idList = Arrays.stream(ids.split(","))
            .map(Long::valueOf)
            .toList();
    return productService.getAbstractProductsForBuyer(idList);
}

// 改后:
@GetMapping("/batch")
public ApiResponse<List<ProductWithImageAbstractDTO>> getProductsByIds(@RequestParam("ids") String ids) {
    List<Long> idList = Arrays.stream(ids.split(","))
            .map(Long::valueOf)
            .toList();
    List<ProductWithImageAbstractDTO> products = productService.getAbstractProductsForBuyer(idList);
    return ApiResponse.success(products);
}
```

- [ ] **Step 2: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java
git commit -m "fix(N1): /internal/product/batch 统一返回 ApiResponse 格式"
```

---

### Task 8: N3 — 内部库存端点增加 @Valid

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java:58-86`
- 修改: `common-api/src/main/java/com/gzasc/aishopping/common/dto/product/StockDeductRequest.java`
- 修改: `common-api/src/main/java/com/gzasc/aishopping/common/dto/product/StockReserveRequest.java`

- [ ] **Step 1: StockDeductRequest 增加校验**

```java
// common-api/.../StockDeductRequest.java
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDeductRequest implements Serializable {
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Positive(message = "数量必须大于0")
    private int quantity;
}
```

- [ ] **Step 2: StockReserveRequest 增加校验**

```java
// common-api/.../StockReserveRequest.java
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReserveRequest implements Serializable {
    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Positive(message = "数量必须大于0")
    private int quantity;
}
```

- [ ] **Step 3: Controller 端点增加 @Valid**

```java
// InternalProductController.java 第58-59行
// 改前:
public ApiResponse<Void> deductStock(@RequestBody StockDeductRequest request) {
// 改后:
public ApiResponse<Void> deductStock(@RequestBody @Valid StockDeductRequest request) {

// 第68-69行
// 改前:
public ApiResponse<Void> restoreStock(@RequestBody StockDeductRequest request) {
// 改后:
public ApiResponse<Void> restoreStock(@RequestBody @Valid StockDeductRequest request) {

// 第78-79行
// 改前:
public ApiResponse<Void> reserveStock(@RequestBody StockReserveRequest req) {
// 改后:
public ApiResponse<Void> reserveStock(@RequestBody @Valid StockReserveRequest req) {
```

- [ ] **Step 4: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java
git add AI-Shopping-backend_Eureka/common-api/src/main/java/com/gzasc/aishopping/common/dto/product/StockDeductRequest.java
git add AI-Shopping-backend_Eureka/common-api/src/main/java/com/gzasc/aishopping/common/dto/product/StockReserveRequest.java
git commit -m "fix(N3): 内部库存端点增加 @Valid 校验注解"
```

---

### Task 9: N6 — ProductDTO.price 类型 Double→BigDecimal 消除精度损失

**文件:**
- 修改: `common-api/src/main/java/com/gzasc/aishopping/common/dto/product/ProductDTO.java:16`
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java:38`

- [ ] **Step 1: ProductDTO.price 改为 BigDecimal**

```java
// ProductDTO.java 第16行
// 改前:
private Double price;
// 改后:
private BigDecimal price;
```

新增 import: `import java.math.BigDecimal;`

- [ ] **Step 2: InternalProductController 移除 .doubleValue() 转换**

```java
// InternalProductController.java 第38行
// 改前:
dto.setPrice(product.getPrice() != null ? product.getPrice().doubleValue() : null);
// 改后:
dto.setPrice(product.getPrice());
```

- [ ] **Step 3: 验证编译和测试通过**

Run: `cd AI-Shopping-backend_Eureka && mvn compile -pl common-api,product-service -am`
Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add AI-Shopping-backend_Eureka/common-api/src/main/java/com/gzasc/aishopping/common/dto/product/ProductDTO.java
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java
git commit -m "fix(N6): ProductDTO.price 改为 BigDecimal 消除跨服务精度损失"
```

---

### Task 10: N5 — updateProductWithImage 无现有图片时支持新增

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java:381-387`

- [ ] **Step 1: 补全 else 分支**

```java
// ProductServiceImpl.java 第381-388行
// 改前:
if (imageUrl != null && !imageUrl.isBlank()) {
    if (existingProduct.getImageId() != null && existingProduct.getImageId() > 0) {
        ProductImageInfo imageInfo = new ProductImageInfo();
        imageInfo.setId(existingProduct.getImageId());
        imageInfo.setUrl(imageUrl);
        productImageInfoMapper.updateUrl(imageInfo);
    }
}

// 改后:
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
}
```

- [ ] **Step 2: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java
git commit -m "fix(N5): updateProductWithImage 无现有图片时支持新增图片记录"
```

---

### Task 11: N10 — 内部 API 统一 @RequestParam 改为 @RequestBody

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java:89-97, 119-128`

- [ ] **Step 1: 修改 confirmReservation 参数**

```java
// 改前:
@PostMapping("/confirm-reservation")
public ApiResponse<Void> confirmReservation(@RequestParam String orderId) {
// 改后:
@PostMapping("/confirm-reservation")
public ApiResponse<Void> confirmReservation(@RequestBody Map<String, String> body) {
    String orderId = body.get("orderId");
```

- [ ] **Step 2: 修改 releaseReservation 参数**

```java
// 改前:
@PostMapping("/release-reservation")
public ApiResponse<Void> releaseReservation(@RequestParam String orderId) {
// 改后:
@PostMapping("/release-reservation")
public ApiResponse<Void> releaseReservation(@RequestBody Map<String, String> body) {
    String orderId = body.get("orderId");
```

- [ ] **Step 3: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java
git commit -m "fix(N10): 内部 confirm/release-reservation 统一使用 @RequestBody"
```

---

## Phase 3 — P2 (LOW)

### Task 12: N4 — productReservationMapper productId 类型统一

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/mapper/ProductReservationMapper.java:28-36`

- [ ] **Step 1: 改为 Long 类型**

```java
// ProductReservationMapper.java 第28-29行
// 改前:
@Update("UPDATE products SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity}")
int deductProductStock(@Param("productId") String productId, @Param("quantity") int quantity);

// 改后:
@Update("UPDATE products SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity}")
int deductProductStock(@Param("productId") Long productId, @Param("quantity") int quantity);

// 第31-32行
// 改前:
@Select("SELECT stock FROM products WHERE id = #{productId} FOR UPDATE")
int selectProductStockForUpdate(@Param("productId") String productId);
// 改后:
@Select("SELECT stock FROM products WHERE id = #{productId} FOR UPDATE")
int selectProductStockForUpdate(@Param("productId") Long productId);

// 第34-36行
// 改前:
int sumReservedQty(@Param("productId") String productId);
// 改后:
int sumReservedQty(@Param("productId") Long productId);
```

- [ ] **Step 2: 更新 ProductReservationServiceImpl 调用处做类型转换**

```java
// ProductReservationServiceImpl.java 第32-33行
// 改前:
int stock = mapper.selectProductStockForUpdate(productId);
int alreadyReserved = mapper.sumReservedQty(productId);
// 改后:
Long pid = Long.valueOf(productId);
int stock = mapper.selectProductStockForUpdate(pid);
int alreadyReserved = mapper.sumReservedQty(pid);

// 第71行 confirm() 内:
// 改前:
rows = mapper.deductProductStock(reservation.getProductId(), reservation.getQuantity());
// 改后:
rows = mapper.deductProductStock(Long.valueOf(reservation.getProductId()), reservation.getQuantity());
```

- [ ] **Step 3: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/mapper/ProductReservationMapper.java
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductReservationServiceImpl.java
git commit -m "fix(N4): productId Mapper 参数类型统一为 String→Long"
```

---

### Task 13: N11 — 删除商品时清理关联图片

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java:179-189`

- [ ] **Step 1: deleteProduct 增加图片清理**

```java
// ProductServiceImpl.java 第179-189行
// 改前:
@Override
@Transactional
public int deleteProduct(Long productId) {
    Product product = productMapper.selectProductById(productId);
    if (product == null) {
        throw new ProductException(404, "商品不存在: " + productId);
    }
    if (product.isSale()) {
        throw new ProductException(400, "商品在上架中，请先下架: " + productId);
    }
    return productMapper.deleteProduct(productId);
}

// 改后:
@Override
@Transactional
public int deleteProduct(Long productId) {
    Product product = productMapper.selectProductById(productId);
    if (product == null) {
        throw new ProductException(404, "商品不存在: " + productId);
    }
    if (product.isSale()) {
        throw new ProductException(400, "商品在上架中，请先下架: " + productId);
    }
    if (product.getImageId() != null && product.getImageId() > 0) {
        productImageInfoMapper.deleteById(product.getImageId());
    }
    return productMapper.deleteProduct(productId);
}
```

- [ ] **Step 2: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java
git commit -m "fix(N11): 删除商品时清理关联图片记录"
```

---

### Task 14: N12 — 内部 getProductById 补充 imageUrl 和 shop 信息

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java:30-46`

- [ ] **Step 1: 补充 imageUrl 和 shop 字段**

```java
// InternalProductController.java 第30-46行
// 改前:
@GetMapping("/{productId}")
public ApiResponse<ProductDTO> getProductById(@PathVariable("productId") Long productId) {
    Product product = productMapper.selectProductById(productId);
    if (product == null) {
        return ApiResponse.error(404, "商品不存在");
    }
    ProductDTO dto = new ProductDTO();
    dto.setId(product.getId());
    dto.setName(product.getName());
    dto.setPrice(product.getPrice() != null ? product.getPrice().doubleValue() : null);
    dto.setTags(product.getTags());
    dto.setDescription(product.getDescription());
    dto.setStock(product.getStock());
    dto.setShopId(product.getShopId());
    dto.setCreatedAt(product.getCreatedAt());
    dto.setUpdatedAt(product.getUpdatedAt());
    return ApiResponse.success(dto);
}

// 改后:
@GetMapping("/{productId}")
public ApiResponse<ProductDTO> getProductById(@PathVariable("productId") Long productId) {
    Product product = productMapper.selectProductById(productId);
    if (product == null) {
        return ApiResponse.error(404, "商品不存在");
    }
    ProductDTO dto = new ProductDTO();
    dto.setId(product.getId());
    dto.setName(product.getName());
    dto.setPrice(product.getPrice());
    dto.setTags(product.getTags());
    dto.setDescription(product.getDescription());
    dto.setStock(product.getStock());
    dto.setImageUrl(getImageUrl(product.getImageId()));
    dto.setShopId(product.getShopId());
    dto.setCreatedAt(product.getCreatedAt());
    dto.setUpdatedAt(product.getUpdatedAt());
    return ApiResponse.success(dto);
}
```

需要给 `ProductDTO` 增加 `imageUrl` 字段，或在 `InternalProductController` 注入 `productService` 调用 `getImageUrl`。

- [ ] **Step 2: ProductDTO 增加 imageUrl 字段**

```java
// ProductDTO.java
private String imageUrl;
```

- [ ] **Step 3: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java
git add AI-Shopping-backend_Eureka/common-api/src/main/java/com/gzasc/aishopping/common/dto/product/ProductDTO.java
git commit -m "fix(N12): 内部 getProductById 补充 imageUrl 和 shop 信息"
```

---

### Task 15: N13 — 分页大小参数化

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/mapper/SalableProductMapper.java:19-20`

- [ ] **Step 1: 增加分页大小参数**

```java
// SalableProductMapper.java 第19-20行
// 改前:
@Select("SELECT id FROM salable_products LIMIT 20 OFFSET #{offset}")
List<Long> selectAll(@Param("offset") int offset);

// 改后:
@Select("SELECT id FROM salable_products LIMIT #{limit} OFFSET #{offset}")
List<Long> selectAll(@Param("offset") int offset, @Param("limit") @Param("size") int size);
```

- [ ] **Step 2: 更新调用方传递分页大小**

```java
// ProductServiceImpl.java 第154行
// 改前:
List<Long> salableIds = salableProductMapper.selectAll(page * 20);
// 改后:
int pageSize = 20;
List<Long> salableIds = salableProductMapper.selectAll(page * pageSize, pageSize);
```

- [ ] **Step 3: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/mapper/SalableProductMapper.java
git commit -m "fix(N13): 分页大小参数化取代硬编码 20"
```

---

### Task 16: N14 — 预占错误码改为 409

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductReservationServiceImpl.java:35`

- [ ] **Step 1: 改错误码**

```java
// ProductReservationServiceImpl.java 第34-35行
// 改前:
if (stock - alreadyReserved < quantity) {
    throw new ProductException("商品库存不足");
// 改后:
if (stock - alreadyReserved < quantity) {
    throw new ProductException(409, "商品库存不足");
```

- [ ] **Step 2: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductReservationServiceImpl.java
git commit -m "fix(N14): 预占库存不足返回 409 Conflict 而非默认 400"
```

---

### Task 17: N15 — Product.isSale 字段命名规范化

**文件:**
- 修改: `product-service/src/main/java/com/gzasc/aishopping/product/model/Product.java:20`

- [ ] **Step 1: 评估影响范围**

`boolean isSale` 的问题：
- Lombok `@Data` 生成 `isSale()` getter，Jackson 序列化为 `"isSale": true` ✅
- MyBatis SQL 中 `AS isSale` 映射到字段 `isSale` ✅
- **现状功能正常**，仅是命名规范问题

权衡（LOW 级别）：
| 选项 | 影响 |
|------|------|
| 不改（推荐） | 零风险 |
| 改 `boolean isSale` → `Boolean sale` | 需同步改 MyBatis resultMap/别名、JSON API 兼容性需 `@JsonProperty` |
| 加 `@JsonProperty("isSale")` | 纯装饰性改动，无实际收益 |

**建议：跳过此 Task，将 N15 标记为 `wontfix`，关闭 Bug。** 当前实现是 Spring Boot 项目中 `boolean` 字段的标准写法，功能正常。

若仍需修复，步骤：
1. 改字段名 `sale`，加 `@JsonProperty("isSale")`
2. 所有 Mapper XML/注解中 `AS isSale` → `AS sale`
3. 验证 JSON 序列化字段名不变

- [ ] **Step 3: 验证测试通过**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn test -DfailIfNoTests=false`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/model/Product.java
git commit -m "fix(N15): Product.isSale 字段命名规范化"
```

---

## 最终验证

- [ ] **全量测试**

Run: `cd AI-Shopping-backend_Eureka\product-service && mvn clean test`
Expected: BUILD SUCCESS, 120+ 测试全部通过

- [ ] **全量编译（含 common-api 变更）**

Run: `cd AI-Shopping-backend_Eureka && mvn compile`
Expected: BUILD SUCCESS

- [ ] **提交未提交的变更**

```bash
git add -A
git commit -m "fix: 完成 product-service 全量 Bug 修复（P0/P1/P2 共17个）"
```
