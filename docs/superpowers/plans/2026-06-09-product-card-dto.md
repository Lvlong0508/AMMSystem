# 后端重构用户端商品 DTO 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 ProductCardDTO，用户端和 AI 商品列表接口只返回卡片所需字段（id, name, imageUrl, stock, price），去掉 shop/tags/imageId。

**Architecture:** common-api 新增 DTO → product-service 新增 converter/service 方法 → 切换用户端+internal 列表端点 → 简化 chat-service ProductTools。旧 DTO 及其消费者（订单服务 /batch 等）不动。

**Tech Stack:** Spring Boot, MyBatis, Feign, Lombok

---

### Task 1: 新增 ProductCardDTO

**Files:**
- Create: `common-api/src/main/java/com/gzasc/aishopping/common/dto/product/ProductCardDTO.java`

- [ ] **Step 1: 创建 DTO**

```java
package com.gzasc.aishopping.common.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCardDTO implements Serializable {
    private Long id;
    private String name;
    private String imageUrl;
    private Integer stock;
    private BigDecimal price;
}
```

- [ ] **Step 2: 验证编译**

Run: `mvnw compile -pl common-api -am`
Expected: BUILD SUCCESS

---

### Task 2: 新增 Card 专用 Mapper 方法

**Files:**
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/mapper/ProductMapper.java`

当前 `selectAbstractProductsByIds` 只选 `id,name,price,tags,image_id,shop_id`，缺 `stock`。不改它，新增一个 Card 专用方法（只需 id/name/price/stock/image_id）。

- [ ] **Step 1: 新增 selectCardProductsByIds**

```java
@Select("<script>" +
        "SELECT id,name,price,stock,image_id AS imageId FROM products WHERE id IN " +
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
        "#{id}" +
        "</foreach>" +
        "</script>")
List<Product> selectCardProductsByIds(@Param("ids") List<Long> ids);
```

- [ ] **Step 2: 验证编译**

Run: `mvnw compile -pl product-service -am`
Expected: BUILD SUCCESS

---

### Task 3: ProductConverter 新增 toCardDTO

**Files:**
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/converter/ProductConverter.java`

- [ ] **Step 1: 在类末尾新增转换方法**

```java
public ProductCardDTO toCardDTO(Product product, String imageUrl) {
    if (product == null) return null;
    return new ProductCardDTO(
        product.getId(), product.getName(), imageUrl,
        product.getStock(), product.getPrice()
    );
}

public List<ProductCardDTO> toCardDTOList(List<Product> products, Map<Integer, String> imageUrlMap) {
    if (products == null) return List.of();
    return products.stream().map(p -> {
        String url = p.getImageId() != null && imageUrlMap != null
            ? imageUrlMap.get(p.getImageId()) : null;
        return toCardDTO(p, url);
    }).collect(Collectors.toList());
}
```

需加 import：`com.gzasc.aishopping.common.dto.product.ProductCardDTO`

---

### Task 4: ProductService 新增 getSalableProductCards

**Files:**
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/service/ProductService.java`
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java`

- [ ] **Step 1: ProductService 接口新增声明**

```java
List<ProductCardDTO> getSalableProductCards(int page);
List<ProductCardDTO> getSalableProductCardsByShopId(Long shopId);
List<ProductCardDTO> getProductCardsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page);
```

- [ ] **Step 2: ProductServiceImpl 实现**

```java
@Override
public List<ProductCardDTO> getSalableProductCards(int page) {
    List<Long> salableIds = salableProductMapper.selectAll(page, 20);
    if (salableIds.isEmpty()) return List.of();
    List<Product> products = productMapper.selectCardProductsByIds(salableIds);
    if (products.isEmpty()) return List.of();
    Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
    return productConverter.toCardDTOList(products, imageUrlMap);
}

@Override
public List<ProductCardDTO> getSalableProductCardsByShopId(Long shopId) {
    List<Product> products = productMapper.selectByShopId(shopId).stream()
        .filter(Product::isSale).collect(Collectors.toList());
    if (products.isEmpty()) return List.of();
    Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
    return productConverter.toCardDTOList(products, imageUrlMap);
}

@Override
public List<ProductCardDTO> getProductCardsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page) {
    int offset = page * 20;
    List<Product> products = productMapper.selectByPriceRangeWithPage(minPrice, maxPrice, offset)
        .stream().filter(Product::isSale).collect(Collectors.toList());
    if (products.isEmpty()) return List.of();
    Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
    return productConverter.toCardDTOList(products, imageUrlMap);
}
```

需加 import：`com.gzasc.aishopping.common.dto.product.ProductCardDTO`

---

### Task 5: 切换 InternalProductController /page

**Files:**
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java`

- [ ] **Step 1: 修改 getProductPage**

```java
List<ProductCardDTO> products = productService.getSalableProductCards(page);
return ApiResponse.success(Map.of("products", products, "page", page, "size", products.size()));
```

需加 import：`com.gzasc.aishopping.common.dto.product.ProductCardDTO`

---

### Task 6: 切换 ProductUserController 列表端点

**Files:**
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/controller/ProductUserController.java`

- [ ] **Step 1: 修改三个端点**

`/all` 改为 `productService.getSalableProductCards(page)`
`/shop/{shopId}` 改为 `productService.getSalableProductCardsByShopId(shopId)`
`/price-range` 改为 `productService.getProductCardsByPriceRange(minPrice, maxPrice, page)`

需加 import：`com.gzasc.aishopping.common.dto.product.ProductCardDTO`

---

### Task 7: 简化 chat-service ProductTools

**Files:**
- Modify: `chat-service/src/main/java/com/gzasc/aishopping/chat/tools/ProductTools.java`

- [ ] **Step 1: 简化 getAllProducts**

去掉 shopName 提取逻辑，直接透传 products 列表：

```java
@Tool("获取商品列表（分页，每页20条）。返回商品信息包含：id, name, imageUrl, stock, price")
public List<Map<String, Object>> getAllProducts(@P("页码，从0开始，每页20条") int page) {
    ApiResponse<Map<String, Object>> response = productFeignClient.getAllProducts(page);
    if (response == null || response.getCode() != 200 || response.getData() == null) {
        return Collections.emptyList();
    }
    Object rawData = response.getData().get("products");
    if (!(rawData instanceof List)) return Collections.emptyList();
    return (List<Map<String, Object>>) rawData;
}
```

- [ ] **Step 2: 简化 getProductDetails**

去掉 shop/shopName 提取：

```java
@Tool("获取指定商品详情。返回商品完整信息")
public Map<String, Object> getProductDetails(@P("商品ID") String productId) {
    Long id;
    try { id = Long.valueOf(productId); }
    catch (NumberFormatException e) { throw new AiToolException("商品ID格式不正确"); }
    ApiResponse<Map<String, Object>> response = productFeignClient.getProductByIdExternal(id);
    if (response == null || response.getCode() != 200 || response.getData() == null) {
        throw new AiToolException("id不存在或商品已下架");
    }
    return response.getData();
}
```

---

### Task 8: 更新测试

**Files:**
- Modify: `product-service/src/test/java/com/gzasc/aishopping/product/controller/internal/InternalProductControllerTest.java`
- Modify: `product-service/src/test/java/com/gzasc/aishopping/product/service/ProductServiceImplTest.java`
- Modify: `product-service/src/test/java/com/gzasc/aishopping/product/mapper/ProductMapperTest.java`
- Modify: `chat-service/src/test/java/com/gzasc/aishopping/chat/tools/ProductToolsTest.java`

- [ ] **Step 1: InternalProductControllerTest — 修改 testGetProductPage**

mock 改为 `getSalableProductCards`，返回 `ProductCardDTO`：

```java
@Test
@DisplayName("GET /internal/product/page - 分页查询可售商品成功")
void testGetProductPage() throws Exception {
    ProductCardDTO product = new ProductCardDTO();
    product.setId(1L);
    product.setName("测试商品");
    product.setPrice(BigDecimal.valueOf(99.99));
    product.setStock(10);
    when(productService.getSalableProductCards(0)).thenReturn(List.of(product));

    mockMvc.perform(get("/internal/product/page").param("page", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.products[0].id").value(1))
            .andExpect(jsonPath("$.data.products[0].name").value("测试商品"))
            .andExpect(jsonPath("$.data.products[0].stock").value(10))
            .andExpect(jsonPath("$.data.page").value(0));
}
```

- [ ] **Step 2: ProductServiceImplTest — 新增 getSalableProductCards 测试**

```java
@Test
@DisplayName("PR-CARD-001 - getSalableProductCards 有数据")
void testGetSalableProductCardsWithData() {
    when(salableProductMapper.selectAll(0, 20)).thenReturn(List.of(1L, 2L));
    Product p1 = new Product(); p1.setId(1L); p1.setImageId(1); p1.setStock(10);
    Product p2 = new Product(); p2.setId(2L); p2.setImageId(2); p2.setStock(5);
    when(productMapper.selectAbstractProductsByIds(List.of(1L, 2L))).thenReturn(List.of(p1, p2));
    when(productImageInfoMapper.selectByIds(anyList())).thenReturn(List.of());
    when(productConverter.toCardDTOList(anyList(), anyMap())).thenReturn(List.of(new ProductCardDTO(), new ProductCardDTO()));

    List<ProductCardDTO> result = productService.getSalableProductCards(0);
    assertEquals(2, result.size());
}

@Test
@DisplayName("PR-CARD-002 - getSalableProductCards 无数据")
void testGetSalableProductCardsEmpty() {
    when(salableProductMapper.selectAll(0, 20)).thenReturn(List.of());
    List<ProductCardDTO> result = productService.getSalableProductCards(0);
    assertTrue(result.isEmpty());
}
```

- [ ] **Step 3: ProductMapperTest — 验证 stock 字段被正确映射**

创建商品时 setStock(100)，查询后 assert stock 为 100。

- [ ] **Step 4: ProductToolsTest — 去掉 shop/shopName 相关测试**

原测试中关于 shop 字段和 shopName 提取的用例（CH-017, CH-018）需要：
- CH-017（无 shop 字段）：不再相关，改为验证基本字段
- CH-018（shop 非 Map）：不再相关，可以删除

新增 stock 字段的验证。

- [ ] **Step 5: 运行测试验证**

Run: `mvnw test -pl product-service -am` 和 `mvnw test -pl chat-service -am`
Expected: 所有测试通过

---

### Task 9: 提交

- [ ] **Step 1: Commit**

```bash
git add common-api/src/main/java/com/gzasc/aishopping/common/dto/product/ProductCardDTO.java
git add product-service/src/main/java/com/gzasc/aishopping/product/mapper/ProductMapper.java
git add product-service/src/main/java/com/gzasc/aishopping/product/converter/ProductConverter.java
git add product-service/src/main/java/com/gzasc/aishopping/product/service/ProductService.java
git add product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java
git add product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java
git add product-service/src/main/java/com/gzasc/aishopping/product/controller/ProductUserController.java
git add chat-service/src/main/java/com/gzasc/aishopping/chat/tools/ProductTools.java
git add product-service/src/test/
git add chat-service/src/test/
git commit -m "feat: add ProductCardDTO for user-facing card display, replace ProductWithImageAbstractDTO in list endpoints"
```