# P1 高优先级重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (\- [ ]\) for tracking.

**Goal:** 拆分 ProductServiceImpl，按业务领域创建 5 个新服务接口，修复可售过滤和双重数据源

**Architecture:** 
- 新服务接口：BuyerProductService、SellerProductService、InternalProductService、ProductCommandService、ProductShopInfoService
- 从 ProductServiceImpl 提取对应实现，ProductServiceImpl 改为 @Deprecated 委托模式
- Controller 注入新 Service，去除 ProductService 依赖
- 移除 salable_products 表相关操作

**Tech Stack:** Java 17+, Spring Boot 3.x, MyBatis, MySQL

---

## Phase 1: 创建新 Service 接口

- [ ] Task 1: 创建 BuyerProductService.java — 买家端查询（getBuyerVisibleProductDetail、getAbstractProductsForBuyer、getSalableProductCards、getProductCardsByPriceRange、getSalableProductCardsByShopId）
- [ ] Task 2: 创建 SellerProductService.java — 商家端管理（getSellerProductDetail、getSellerProductsByShopId、getSellerProductsAbstract、listProduct、unlistProduct、deleteProduct）
- [ ] Task 3: 创建 InternalProductService.java — 内部查询（getInternalProductDetail、getBasicProductById、getProductsByName）
- [ ] Task 4: 创建 ProductCommandService.java — 创建/更新商品含图片
- [ ] Task 5: 创建 ProductShopInfoService.java — 店铺缓存，从 ProductServiceImpl 提取

## Phase 2: 创建新 Service 实现

- [ ] Task 6: 创建 BuyerProductServiceImpl.java — 从 ProductServiceImpl 提取买家端逻辑 + 可售过滤
- [ ] Task 7: 创建 SellerProductServiceImpl.java — 提取商家端逻辑（上下架、删除）
- [ ] Task 8: 创建 InternalProductServiceImpl.java — 提取内部查询逻辑
- [ ] Task 9: 创建 ProductCommandServiceImpl.java — 提取创建/更新逻辑
- [ ] Task 10: 创建 ProductShopInfoServiceImpl.java — 提取 shopInfoCache 逻辑

## Phase 3: 改造 Controller

- [ ] Task 11: 改造 ProductUserController.java — 注入 BuyerProductService
- [ ] Task 12: 改造 ProductSellerController.java — 注入 SellerProductService + ProductCommandService
- [ ] Task 13: 改造 InternalProductController.java — 注入 InternalProductService

## Phase 4: 清理

- [ ] Task 14: ProductServiceImpl + ProductService 标记 @Deprecated，委托到新服务
- [ ] Task 15: 删除 SalableProductMapper（若存在）
- [ ] Task 16: 全量测试验证零回归
