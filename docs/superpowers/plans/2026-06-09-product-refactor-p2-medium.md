# P2 中优先级重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (\- [ ]\) for tracking.

**Goal:** 统一库存操作到 InventoryService，统一图片校验到 ImageStorageService，清理废弃 DTO

**Architecture:** 
- 新建 ProductStockMapper 专注库存扣减/恢复，ProductReservationMapper 不再直接操作 products 表
- ImageStorageService 增加 validateImage 方法和完整 URL 返回
- Controller 去除重复图片校验
- ProductWithImageAbstractDTO/DetailDTO 清理 @Deprecated

**Tech Stack:** Java 17+, Spring Boot 3.x, MyBatis, MySQL

---

## Phase 1: 库存统一

- [ ] Task 1: 创建 ProductStockMapper.java — 专注 products 表库存操作（deductStock、restoreStock）
- [ ] Task 2: 创建 InventoryService 接口 + InventoryServiceImpl — 提供 restoreStock
- [ ] Task 3: 改造 ProductReservationServiceImpl.confirm — 使用 ProductStockMapper.deductStock 替代 ProductReservationMapper.deductProductStock
- [ ] Task 4: 改造 ProductServiceImpl.restoreStock — 委托给 InventoryService
- [ ] Task 5: 删除 ProductReservationMapper 中的 deductProductStock 方法

## Phase 2: 图片校验 + URL 统一

- [ ] Task 6: ImageStorageService 增加 validateImage(MultipartFile) 方法
- [ ] Task 7: ImageStorageServiceImpl 实现 validateImage（空文件、类型白名单、大小限制）
- [ ] Task 8: ImageStorageServiceImpl.saveImage 返回完整 URL（imageBaseUrl + relativePath）
- [ ] Task 9: ProductSellerController 创建/更新方法中移除重复图片校验，改用 ImageStorageService.validateImage
- [ ] Task 10: ProductCommandServiceImpl 中移除 imageBaseUrl + relativePath 拼接（saveImage 已返回完整 URL）

## Phase 3: DTO 清理

- [ ] Task 11: 确认 DTO 状态 — 删除 ProductWithImageAbstractDTO @Deprecated（确认已被 ProductCardDTO 替代）
- [ ] Task 12: ProductWithImageDetailDTO 去掉 @Deprecated 注解
- [ ] Task 13: 同步清理 ProductConverter 中对应 @Deprecated 方法
- [ ] Task 14: 全量测试验证零回归
