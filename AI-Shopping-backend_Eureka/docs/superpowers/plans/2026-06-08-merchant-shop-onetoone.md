# 商家账号 ↔ 店铺 一对一重构 · 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 将「一个商家账号可对应多个店铺（通过中间表 + 角色）」重构为「一个商家账号仅能对应一个店铺」

**架构方案：**
- 删除 `merchant_roles` 整张表和所有相关 Java 代码
- `shops.merchant_id` 加 UNIQUE 约束
- 商家注册与店铺创建合并为一次请求（auth-service 接收 → Feign 调 shop-service 内部接口）
- `ShopMerchantController` 的 `/myShop` 改为返回单个店铺对象，删除员工相关端点

**Tech Stack:** Spring Boot, MyBatis, MySQL, Feign, JUnit 5 + Mockito

---

### Task 1: 清理 MerchantRole 相关代码（shop-service + common-api）

**文件：**
- 删除：`shop-service/src/main/java/.../model/MerchantRole.java`
- 删除：`shop-service/src/main/java/.../mapper/MerchantRoleMapper.java`
- 删除：`shop-service/src/main/java/.../service/MerchantRoleService.java`
- 删除：`shop-service/src/main/java/.../service/impl/MerchantRoleServiceImpl.java`
- 删除：`shop-service/src/test/java/.../mapper/MerchantRoleMapperTest.java`
- 删除：`common-api/src/main/java/.../dto/shop/MerchantRoleDTO.java`

### Task 2: 修改 ShopMapper（shop-service）

**文件：**
- 修改：`shop-service/src/main/java/.../mapper/ShopMapper.java`

**变更：**
1. 删除 `selectShopsByUserId`（依赖 `merchant_roles` 表）
2. 删除 `selectSimpleShopsByMerchantId`（依赖 `merchant_roles` 表）
3. 新增 `selectShopByMerchantId`（利用 UNIQUE 约束，返回单个 Shop）

### Task 3: 修改 ShopService 接口

**文件：**
- 修改：`shop-service/src/main/java/.../service/ShopService.java`

**变更：**
1. 新增 `getMyShop(Long userId)` — 返回单个 `SimpleShopDTO`
2. 删除 `getSimpleShop(Long userId)` — 改为上面的单数语义
3. 删除 `getShopIdsByMerchantId(Long merchantId)`
4. 新增 `createShopForMerchant(CreateShopForMerchantRequest)` — 供内部 Feign 调用

### Task 4: 修改 ShopServiceImpl（核心重构）

**文件：**
- 修改：`shop-service/src/main/java/.../service/impl/ShopServiceImpl.java`

**变更：**
1. 删除 `MerchantRoleService` 依赖及所有相关代码
2. `createShop` — 删除 MerchantRole 创建逻辑
3. 权限验证从 merchantRoleService 改为校验 `shop.getMerchantId().equals(userId)`
4. `getSimpleShop` 改为 `getMyShop` — 通过 `shopMapper.selectShopByMerchantId` 查询
5. 删除 `getShopIdsByMerchantId` 和 `checkShopOwner` / `checkShopAccess`

### Task 5: 修改 ShopMerchantController

**文件：**
- 修改：`shop-service/src/main/java/.../controller/ShopMerchantController.java`

**变更：**
1. `/myShop` → 改为 `/my-shop`，使用 `getMyShop` 返回单个店铺
2. 修复 `/get` 端点（目前有 bug）→ 改为 `/{shopId}` 路径
3. 删除 `/register` 端点（合并到 auth-service）

### Task 6: 修改 InternalShopController + 添加 createForMerchant

**文件：**
- 修改：`shop-service/src/main/java/.../controller/internal/InternalShopController.java`

**变更：**
1. 删除 `MerchantRoleService` 依赖，删除 `/employees/roles/{merchantId}` 端点
2. 新增 `POST /internal/shop/create-for-merchant` 端点

### Task 7: 新增 DTO — CreateShopForMerchantRequest

**文件：**
- 创建：`shop-service/src/main/java/.../dto/CreateShopForMerchantRequest.java`

### Task 8: 新增 ShopInternalFeignClient（common-api）

**文件：**
- 创建：`common-api/src/main/java/.../feign/shop/ShopInternalFeignClient.java`

### Task 9: 修改 auth-service 注册流程

**文件：**
- 修改：`auth-service/src/main/java/.../dto/RegisterRequest.java` — 新增 `shop` 嵌套字段
- 修改：`auth-service/src/main/java/.../controller/MerchantAuthController.java` — 注册成功后返回 `shopId`
- 修改：`auth-service/src/main/java/.../service/impl/MerchantAuthServiceImpl.java` — 注入 `ShopInternalFeignClient`，注册后 Feign 调用创建店铺

### Task 10: 创建 auth-service 端的 CreateShopForMerchantRequest DTO

由于 `ShopInternalFeignClient` 在 common-api 中，auth-service 需要能够构造请求体。
将 `CreateShopForMerchantRequest` 放到 common-api 的 `dto/shop/` 中而不是 shop-service 本地。

**文件：**
- 创建：`common-api/src/main/java/.../dto/shop/CreateShopForMerchantRequest.java`

### Task 11: 更新 ShopServiceImplTest

**文件：**
- 修改：`shop-service/src/test/java/.../service/impl/ShopServiceImplTest.java`

**变更：**
1. 删除 `MerchantRoleService` 相关的 mock 和测试
2. 删除 createShop 中对 `merchantRoleService.insert` 的验证
3. 删除 `checkShopOwner` / `checkShopAccess` 相关测试
4. 新增 `getMyShop` 的测试

### Task 12: 更新 InternalShopControllerTest

**文件：**
- 修改：`shop-service/src/test/java/.../controller/internal/InternalShopControllerTest.java`

**变更：**
1. 删除 `MerchantRoleService` 依赖
2. 删除 `getMerchantRoles` 和 `getMerchantRoles_empty` 测试
3. 新增 `createForMerchant` 的测试

### Task 13: 更新 ShopMerchantControllerTest

**文件：**
- 修改：`shop-service/src/test/java/.../controller/ShopMerchantControllerTest.java`

**变更：**
1. 删除 `/register` 相关测试，删除 employee 相关测试
2. 适配 `/my-shop` 单数接口
3. 修改 `getShop_success` 适配新路径和返回值

### Task 14: 更新 MerchantAuthServiceImplTest

**文件：**
- 修改：`auth-service/src/test/java/.../service/impl/MerchantAuthServiceImplTest.java`

**变更：**
1. 新增 `ShopInternalFeignClient` mock
2. 新增合并注册（含 shop）成功的测试
3. 新增 Feign 调用失败的测试（验证回滚）

### Task 15: 新增 ShopMapperTest 中 selectShopByMerchantId 的测试

**文件：**
- 修改：`shop-service/src/test/java/.../mapper/ShopMapperTest.java`

**变更：**
1. 删除 `selectShopsByUserId_shouldReturnShops` 测试
2. 新增 `selectShopByMerchantId` 的测试

### Task 16: 验证编译 & 测试通过

- Maven 编译 shop-service 和 auth-service
- 运行所有单元测试
