# 商品服务移除 salable_products 设计

## 背景

当前商品服务同时使用 `products.is_sale` 和 `salable_products` 表表达商品上架状态。`ProductServiceImpl` 在上架时同时更新 `products.is_sale` 并写入 `salable_products`，下架时同时更新 `products.is_sale` 并删除 `salable_products`。这种双数据源会增加维护成本，并可能在部分写入失败、重复上架或人工修复数据时产生不一致。

数据库当前没有需要保留的数据，本次只需要调整新库初始化 SQL 和代码适配方案，不需要历史数据迁移脚本。

## 目标

- 移除 `salable_products` 表。
- 使用 `products.is_sale` 作为商品上架状态的唯一事实源。
- 用户端可售商品查询统一在 SQL 层增加 `is_sale = 1` 条件。
- 保持现有用户端、内部端 API 语义不变。
- 保留现有 Service 中带 `Salable` 的方法名，减少 Controller、Feign、前端调用扩散。
- 删除已废弃的 `getSalableProductsAbstract` 接口与实现。

## 非目标

- 不做历史数据迁移。
- 不重命名外部 API 路径。
- 不重命名仍在使用的 Service 方法。
- 不重构商品服务其它职责，例如图片、店铺信息缓存、库存预占。

## 推荐方案

直接改 Mapper，让可售查询基于 `products` 表过滤 `is_sale = 1`。上下架流程只更新 `products.is_sale`，不再维护派生表。

相比保留双写，此方案消除状态不一致风险。相比创建兼容视图，此方案更直接，减少多余数据库对象，符合当前无历史数据的前提。

## 改动范围

### SQL 初始化

文件：`AI-Shopping-backend_Eureka/sql/init/01-product-init.sql`

- 删除 `salable_products` 建表语句。
- 保留 `products.is_sale TINYINT(1) NOT NULL DEFAULT 1`。
- 保留 `idx_is_sale_price`，用于加速可售商品与价格区间查询。

### Mapper 层

文件：`AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/mapper/SalableProductMapper.java`

- 删除整个 Mapper。

文件：`AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/mapper/ProductMapper.java`

按以下方式新增或调整查询方法：

- `selectCardProductsPage(offset, limit)`：从 `products` 查询卡片字段，条件 `is_sale = 1`，分页返回。
- `selectSalableByShopId(shopId)`：按店铺查询用户端可售商品，条件 `shop_id = ? AND is_sale = 1`。
- `selectByPriceRangeWithPage(minPrice, maxPrice, offset)`：增加 `is_sale = 1`，避免先分页再在 Java 过滤导致页大小错误。
- `selectProductsByName(name)`：增加 `is_sale = 1`，确保搜索结果只返回可售商品。
- `selectAbstractProductsByIds(ids)`：作为用户端批量查询方法，增加 `is_sale = 1`；商家端继续使用 `selectAbstractProductsByIdsJustMerchant(ids)` 返回全部状态。

保留：

- `updateSaleStatus(id, isSale)`，作为上下架唯一写入口。
- `selectByShopId(shopId)`，作为商家端查询全部商品的方法，不增加 `is_sale = 1`。

### Service 层

文件：`AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/service/ProductService.java`

- 删除废弃方法 `getSalableProductsAbstract(int page)`。
- 保留 `getSalableProductCards`、`getSalableProductCardsByShopId` 等方法名。

文件：`AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java`

- 删除 `SalableProductMapper` import、字段和构造注入。
- 删除 `getSalableProductsAbstract` 实现。
- `getSalableProductCards(page)` 改为直接调用 `productMapper.selectCardProductsPage(page * 20, 20)`。
- `getSalableProductsByShopId(shopId)` 和 `getSalableProductCardsByShopId(shopId)` 改用 `productMapper.selectSalableByShopId(shopId)`，删除 Java 侧 `.filter(Product::isSale)`。
- `getProductCardsByPriceRange`、`getProductsByPriceRange`、`getProductsByName` 删除 Java 侧 `.filter(Product::isSale)`，依赖 SQL 过滤。
- `listProduct(productId)` 只执行商品存在检查和 `productMapper.updateSaleStatus(productId, true)`。
- `unlistProduct(productId)` 只执行商品存在检查和 `productMapper.updateSaleStatus(productId, false)`。

### Controller 与外部调用

- `ProductUserController` 路径不变：`/api/user/product/all`、`/api/user/product/shop/{shopId}`、`/api/user/product/price-range`。
- `InternalProductController` 路径不变：`/internal/product/page`。
- `common-api` 的 `ProductFeignClient` 不需要改路径。
- 前端 `frontier-user/src/api/product.js` 不需要改路径。

### 测试层

- 删除 `SalableProductMapperTest.java`。
- 修改 `ProductServiceImplTest.java`：
  - 删除 `SalableProductMapper` mock。
  - 将 `salableProductMapper.selectAll` 相关 mock 改为新 `ProductMapper` 查询方法。
  - 上架、下架测试只验证 `updateSaleStatus`。
  - 删除 `getSalableProductsAbstract` 相关测试。
- 修改 `ProductMapperTest.java`：
  - 增加可售分页、按店铺可售、按价格可售、按名称可售的 SQL 行为测试。
  - 覆盖下架商品不会出现在用户端查询结果中。
- `ProductUserControllerTest` 和 `InternalProductControllerTest` 保持现有 API 路径断言不变，按删除后的 Service 接口更新 mock。

## 数据流

用户端查询商品列表时，Controller 调用现有 ProductService 方法，ProductService 直接调用 ProductMapper 的可售查询方法。Mapper 查询 `products` 表并在 SQL 中过滤 `is_sale = 1`，返回商品列表后继续复用现有图片 URL 映射和 DTO 转换流程。

商家端查询商品时，继续使用返回全部商品的 Mapper 方法，并通过 DTO 暴露 `isSale` 状态。

上架和下架时，Service 校验商品存在后，只更新 `products.is_sale`。后续用户端查询会自然根据该字段过滤。

## 错误处理

- 页码为负数继续抛出 `ProductException(400, "页码不能为负数")`。
- 上架或下架不存在商品继续抛出 `ProductException(404, "商品不存在: " + productId)`。
- 删除商品前仍检查 `product.isSale()`，防止删除上架中商品。
- 查询为空返回空列表，保持现有行为。

## 索引与性能

保留 `idx_is_sale_price` 支持价格区间可售查询。本次最小方案不新增索引，避免过早增加索引复杂度。后续如查询量上升，可评估：

- `(is_sale, id)`：用于首页分页可售商品。
- `(shop_id, is_sale)`：用于店铺页可售商品。
- `(is_sale, name)` 对 `LIKE '%keyword%'` 帮助有限，优先级较低。

## 验收标准

- 初始化 SQL 不再创建 `salable_products`。
- 项目生产代码和测试代码中不存在对 `SalableProductMapper` 或 `salable_products` 的引用。
- 用户端列表、搜索、店铺、价格区间查询不会返回 `is_sale = 0` 商品。
- 商家端商品查询仍能返回全部商品及上下架状态。
- 上架、下架只更新 `products.is_sale`。
- 删除已废弃的 `getSalableProductsAbstract` 后编译通过。
- product-service 单元测试通过。

## 风险与注意事项

- `selectAbstractProductsByIds` 当前名称不体现用户端过滤语义，但接口注释为“用户端批量查询”，商家端已有独立方法，因此本次明确改为只返回 `is_sale = 1` 商品。
- 原来 `getProductById` 用户端详情接口没有过滤下架商品，本设计不改变该行为。如需用户端详情也隐藏下架商品，应另起需求明确。
- 删除废弃方法会影响任何仍直接调用 `getSalableProductsAbstract` 的测试或旧代码，需要一并清理。
