# 商品服务高优先级重构 Spec

> **文件名**: `2026-06-09-product-refactor-p1-high.md`
> **优先级**: P1 - 高优先级
> **对应报告问题**: #1, #2, #3, #4, #5
> **状态**: 设计稿（已简化，去掉了过度工程部分）

---

## 1. 背景与目标

### 1.1 现状问题

| 问题 | 文件 | 影响 |
|------|------|------|
| #1 ProductServiceImpl 职责过重 | `ProductServiceImpl.java:46` (439 行) | 修改任一业务容易影响其他路径 |
| #2 ProductService 接口过宽 | `ProductService.java:17` | 调用方依赖大接口，职责边界模糊 |
| #3 三端复用同一个详情查询 | `ProductUserController:43`, `ProductSellerController:30`, `InternalProductController:45` | 可见性和权限规则无法从方法名/服务边界看出 |
| #4 可售过滤在 Java 内存中 | `ProductServiceImpl.java` 多处 + `ProductMapper.java:33/74` | 分页语义错误（先分页再过滤） |
| #5 双重数据源 | `products.is_sale` + `salable_products` 表 | 一致性强依赖事务，`addSalable` 非幂等 |

### 1.2 重构目标

- 按业务领域拆分为多个专注的服务，每个服务的职责可被单句描述
- 按场景拆分接口，消除大接口依赖
- 查询语义接口显式化（买家/商家/内部调用路径分离）
- 可售过滤下推到 SQL 层，修复分页语义
- 统一上架状态的事实来源，去重 `salable_products`

---

## 2. 服务拆分方案

### 2.1 新服务架构

```
┌─────────────────────────────────────────────────────────┐
│                    Controller 层                        │
│  ProductUserController / ProductSellerController        │
│  InternalProductController                              │
└──────┬──────────┬─────────────┬──────────────┬──────────┘
       │          │             │              │
       ▼          ▼             ▼              ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐
│ Buyer    │ │ Seller   │ │ Internal │ │ Product      │
│ Product  │ │ Product  │ │ Product  │ │ Command      │
│ Service  │ │ Service  │ │ Service  │ │ Service      │
└────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘
     │            │             │              │
     └────────────┼─────────────┼──────────────┘
                  │             │
          ┌───────▼───────┐  ┌─▼──────────────┐
          │ ProductShop   │  │ Inventory      │
          │ InfoService   │  │ Service        │
          └───────────────┘  └────────────────┘
```

### 2.2 新建服务接口

#### 2.2.1 `BuyerProductService` — 买家端商品查询

```java
public interface BuyerProductService {
    ProductWithImageDetailDTO getBuyerVisibleProductDetail(Long productId);
    List<ProductWithImageAbstractDTO> getAbstractProductsForBuyer(List<Long> ids);
    List<ProductCardDTO> getSalableProductCards(int page);
    List<ProductCardDTO> getProductCardsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page);
    List<ProductCardDTO> getSalableProductCardsByShopId(Long shopId);
}
```

#### 2.2.2 `SellerProductService` — 商家端商品管理

```java
public interface SellerProductService {
    ProductWithImageDetailDTO getSellerProductDetail(Long productId);
    List<SellerProductAbstractDTO> getSellerProductsByShopId(Long shopId);
    List<SellerProductAbstractDTO> getSellerProductsAbstract(List<Long> ids);
    boolean listProduct(Long productId);
    boolean unlistProduct(Long productId);
    int deleteProduct(Long productId);
}
```

#### 2.2.3 `InternalProductService` — 内部服务查询

```java
public interface InternalProductService {
    ProductWithImageDetailDTO getInternalProductDetail(Long productId);
    ProductDTO getBasicProductById(Long productId);
    List<ProductWithImageDetailDTO> getProductsByName(String name);
}
```

#### 2.2.4 `ProductCommandService` — 商品创建/更新（含图片）

```java
public interface ProductCommandService {
    int createProductWithImage(Product product, MultipartFile imageFile);
    int updateProductWithImage(Product product, MultipartFile image);
}
```

#### 2.2.5 `ProductShopInfoService` — 店铺信息与缓存（从 ProductServiceImpl 抽离）

```java
public interface ProductShopInfoService {
    ShopInfoDTO getCachedShopInfo(Long shopId);
    Map<Long, ShopInfoDTO> batchGetShopInfo(Set<Long> shopIds);
}
```

#### 2.2.6 `InventoryService` — 库存管理

```java
public interface InventoryService {
    boolean restoreStock(Long productId, int quantity);
}
```

> InventoryService 在此阶段仅做库存恢复操作的抽取。预占/确认等复杂库存操作保留在 P2 统一。

### 2.3 旧接口兼容策略

- `ProductService` 接口保留一个版本，内部委托到新服务
- 在 `ProductService` 上标注 `@Deprecated`，javadoc 注明迁移到哪个新接口
- 一个版本后删除

---

## 3. 查询语义拆分

### 3.1 接口命名映射

| 当前方法 | 目标方法 | 所属服务 |
|---------|---------|---------|
| `getProductById` | `getBuyerVisibleProductDetail` | BuyerProductService |
| `getProductById` | `getSellerProductDetail` | SellerProductService |
| `getProductById` | `getInternalProductDetail` | InternalProductService |

### 3.2 Controller 调用变更

| Controller | 当前调用 | 变更为 |
|-----------|---------|-------|
| `ProductUserController.getProductById` | `productService.getProductById` | `buyerProductService.getBuyerVisibleProductDetail` |
| `ProductSellerController.getProductById` | `productService.getProductById` | `sellerProductService.getSellerProductDetail` |
| `InternalProductController.getProductById` | `productService.getProductById` | `internalProductService.getInternalProductDetail` |

### 3.3 事务处理

跨服务调用统一使用 `@Transactional`，出错时整体回滚。不引入分布式事务方案。

---

## 4. 可售过滤 SQL 下推

### 4.1 当前问题

```java
// 当前做法：先查全部再内存过滤
productMapper.selectByShopId(shopId)           // 无 is_sale 条件
    .stream().filter(Product::isSale)           // 内存过滤 —— 导致分页语义错误
```

### 4.2 改造方案

在 `ProductMapper` 中新增语义明确的 Mapper 方法：

```java
// 替代 selectByShopId + 内存过滤，SQL 层保证 is_sale = 1
@Select("SELECT ... FROM products WHERE shop_id = #{shopId} AND is_sale = 1")
List<Product> selectSalableByShopId(@Param("shopId") Long shopId);

// 替代 selectAbstractProductsByIds + 内存过滤
@Select("SELECT ... FROM products WHERE id IN (...) AND is_sale = 1")
List<Product> selectSalableByIds(@Param("ids") List<Long> ids);
```

### 4.3 受影响路径

| 位置 | 当前 | 目标 |
|------|------|------|
| `getAbstractProductsForBuyer` | 内存 filter | 使用 `selectSalableByIds` |
| `getSellerProductsByShopId` | 内存 filter | 使用 `selectSalableByShopId` |
| `getSellerProductsAbstract` | 内存 filter | 使用 `selectSalableByIds` |

---

## 5. 双重数据源统一

### 5.1 决策：以 `products.is_sale` 为唯一事实来源

`salable_products` 表废弃。上下架操作只操作 `products.is_sale`，不再写入 `salable_products`。

### 5.2 具体变更

| 文件 | 变更 |
|------|------|
| `ProductServiceImpl.listProduct` | 移除 `salable_products` 写入 |
| `ProductServiceImpl.unlistProduct` | 移除 `salable_products` 删除 |
| `SalableProductMapper`（若存在） | 直接删除 |

---

## 6. 测试策略

- 每个新 Service 新建对应的 `*ServiceTest`
- 重点覆盖：
  - `getBuyerVisibleProductDetail` 不返回已下架商品
  - `getSellerProductDetail` 返回商家所有商品（含下架）
  - 可售过滤 SQL 的正确性
- 旧接口委托测试：验证行为等价

---

## 7. 迁移计划

### Phase 1: 创建新接口和实现
- 创建 5 个新 Service 接口：`BuyerProductService`, `SellerProductService`, `InternalProductService`, `ProductCommandService`, `ProductShopInfoService`
- 从 `ProductServiceImpl` 提取对应实现
- `ProductServiceImpl` 改为委托模式

### Phase 2: 改造 Mapper
- 新增 `selectSalableByIds`、`selectSalableByShopId`
- 去掉 `stream().filter(Product::isSale)`

### Phase 3: 改造 Controller
- 每个 Controller 注入对应的新 Service
- 替换方法调用

### Phase 4: 双重数据源清理
- 上下架操作移除 `salable_products` 写入/删除
- 删除 `SalableProductMapper`

### Phase 5: 清理
- 标记 `ProductService` 为 `@Deprecated`
- 全量测试确认零回归
