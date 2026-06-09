# 商品服务移除 salable_products Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 移除 `salable_products` 表和 Mapper，改为通过 `products.is_sale` 过滤用户端可售商品。

**Architecture:** `products.is_sale` 是上下架唯一事实源；用户端查询在 `ProductMapper` SQL 中过滤 `is_sale = 1`；商家端查询继续返回全部商品。

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis, JUnit 5, Mockito, Maven。

---

## Task 1: Mapper 与 SQL

**Files:**
- Modify: `AI-Shopping-backend_Eureka/sql/init/01-product-init.sql`
- Modify: `AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/mapper/ProductMapper.java`
- Delete: `AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/mapper/SalableProductMapper.java`
- Modify: `AI-Shopping-backend_Eureka/product-service/src/test/java/com/gzasc/aishopping/product/mapper/ProductMapperTest.java`
- Delete: `AI-Shopping-backend_Eureka/product-service/src/test/java/com/gzasc/aishopping/product/mapper/SalableProductMapperTest.java`

- [ ] Add ProductMapper tests proving user queries exclude `is_sale = 0` and merchant `selectByShopId` still includes both states.
- [ ] Run `mvn test -pl product-service -Dtest=ProductMapperTest` and confirm failure before implementation.
- [ ] Add `selectCardProductsPage(offset, limit)` and `selectSalableByShopId(shopId)`.
- [ ] Add `is_sale = 1` to user-facing mapper queries: name, price range, buyer batch IDs, card IDs.
- [ ] Delete `SalableProductMapper.java` and `SalableProductMapperTest.java`.
- [ ] Remove `salable_products` table from init SQL.
- [ ] Run `mvn test -pl product-service -Dtest=ProductMapperTest`.

## Task 2: Service 与单元测试

**Files:**
- Modify: `AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/service/ProductService.java`
- Modify: `AI-Shopping-backend_Eureka/product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java`
- Modify: `AI-Shopping-backend_Eureka/product-service/src/test/java/com/gzasc/aishopping/product/service/ProductServiceImplTest.java`

- [ ] Remove `getSalableProductsAbstract` from interface, implementation, and tests.
- [ ] Remove `SalableProductMapper` injection and mocks.
- [ ] Change `getSalableProductCards` to call `productMapper.selectCardProductsPage(page * 20, 20)`.
- [ ] Change shop user queries to call `productMapper.selectSalableByShopId(shopId)`.
- [ ] Remove Java-side `.filter(Product::isSale)` where SQL now filters.
- [ ] Change `listProduct` and `unlistProduct` to only update `products.is_sale`.
- [ ] Run `mvn test -pl product-service -Dtest=ProductServiceImplTest`.

## Task 3: Project-wide cleanup and verification

**Files:**
- Search all production/test code.

- [ ] Verify no Java production/test reference to `SalableProductMapper` remains.
- [ ] Verify no SQL/schema reference to `salable_products` remains except historical docs if intentionally left.
- [ ] Run `mvn test -pl product-service --also-make` from `AI-Shopping-backend_Eureka`.
- [ ] Run `mvn test -pl product-service -DskipTests` as compile/typecheck fallback if DB integration tests cannot run locally.
