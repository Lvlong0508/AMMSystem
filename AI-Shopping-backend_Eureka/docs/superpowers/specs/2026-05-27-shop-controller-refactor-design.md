# Shop Service Controller 层重构设计

## 概述

对 shop-service 的 Controller 层进行精准重构，解决 URL 路径不一致、Controller 职责过重、分层不规范等问题，同时移除已迁移到 product-service 的商品操作。

## 重构范围

全部 3 个 Controller：
- `ShopMerchantController` — 商家端店铺控制器
- `ShopUserController` — 用户端店铺控制器
- `InternalShopController` — 内部 Feign 调用控制器

## 变更清单

### 1. URL 路径统一（ShopMerchantController）

**类级 `@RequestMapping("/api/seller/shop")` 保持不变**，只修改方法级路径：

| 方法 | 原路径 | 新路径 | 原因 |
|------|--------|--------|------|
| POST 注册 | `/shop/register` | `/register` | 消除 `/api/seller/shop/shop/register` 双重 shop |
| GET 详情 | `/shop/{shopId}` | `/{shopId}` | 消除 `/api/seller/shop/shop/{shopId}` 双重 shop |
| PUT 更新 | `/shop/{shopId}` | `/{shopId}` | 同上 |
| DELETE 关闭 | `/shop/{shopId}` | `/{shopId}` | 同上 |
| GET 商品列表 | `/{shopId}/products` | 不变 | 路径已正确 |
| GET 员工列表 | `/{shopId}/employees` | 不变 | 路径已正确 |
| POST 添加店员 | `/{shopId}/employees/register` | 不变 | 路径已正确 |
| DELETE 移除店员 | `/{shopId}/employees/{merchantId}` | 不变 | 路径已正确 |

### 2. 移除商品操作（ShopMerchantController）

移除以下方法，商品 CRUD 已全部收归 product-service 管理：

- `createProduct()` — `POST /{shopId}/products`
- `updateProduct()` — `PUT /{shopId}/products/{productId}`
- `deleteProduct()` — `DELETE /{shopId}/products/{productId}`

同时移除：
- `ProductFeignClient` 注入
- `MerchantRoleService` 注入（仅被 checkShopOwner 使用）
- `checkShopOwner()` 私有方法

**Controller 精简结果：** 注入从 3 个（ShopService + ProductFeignClient + MerchantRoleService）减少为 1 个（ShopService），方法从 11 个减少为 8 个。

### 3. InternalController 修复

移除 `ShopMapper` 和 `ShopInfoService` 直接注入，统一改为通过 ShopService 层查询：

- `getShopInfo(Long shopId)` — 改为调用 `ShopService.getShopInfoById(shopId)`
- `batchGetShopInfo(Set<Long> shopIds)` — 改为调用 `ShopService.batchGetShopInfo(shopIds)`

**注意：** `getShopInfoById` 找不到店铺时**返回 null（不抛异常）**，因为 InternalController 原逻辑返回 `ApiResponse.success(null)`，其他服务（如 product-service）依赖此静默失败行为。

### 4. Mapper 层新增方法（修复 N+1）

`ShopMapper` 新增批量查询方法，配合 `ShopInfoMapper.selectBatch()` 一次性查完所有 Shop，避免 N+1：

```java
@Select({"<script>",
         "SELECT * FROM shops WHERE id IN",
         "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>",
         "</script>"})
List<Shop> selectShopsByIds(@Param("ids") Collection<Long> ids);
```

### 5. Service 层新增方法（ShopService 接口 + 实现）

新增：

```java
// 按 shopId 查询店铺基本信息（含 Shop → ShopInfo 关联），找不到返回 null
ShopInfoDTO getShopInfoById(Long shopId);

// 批量查询店铺基本信息，使用 Mapper 批量查询修复 N+1
Map<Long, ShopInfoDTO> batchGetShopInfo(Set<Long> shopIds);
```

实现逻辑：`getShopInfoById` 先查 Shop（允许 null），再查 ShopInfo 拼装 ShopInfoDTO；`batchGetShopInfo` 一次查出所有 Shop，按 shopInfoId 分组，批量查 ShopInfo 后拼装。

### 6. ShopServiceImpl 中 ProductFeignClient 的说明

`ShopServiceImpl` 仍持有 `ProductFeignClient`，用于只读商品查询（`getShopProductsWithDetails`、`getUserShopProducts`、`getUserShopProductDetail`）。这是合理的跨服务查询，属于正常业务依赖，不属于"绕过 Service 层"的问题。本次不处理。

### 5. 不变的部分

| 模块 | 说明 |
|------|------|
| ShopUserController | 结构良好，仅确认路径和返回类型一致性 |
| Mapper 层 | ShopMapper 新增 `selectShopsByIds()` 批量查询方法 |
| Model 层 | 无需修改 |
| DTO 层 | 无需修改 |
| Exception / GlobalExceptionHandler | 无需修改 |

## 重构后 API 清单

### 商家端 — `/api/seller/shop`

| 方法 | 路径 | Controller 方法 | 说明 |
|------|------|----------------|------|
| GET | `/api/seller/shop/{shopId}` | `getShop()` | 查询店铺详情 |
| GET | `/api/seller/shop/{shopId}/products` | `getProducts()` | 分页查询店铺商品 |
| GET | `/api/seller/shop/{shopId}/employees` | `getEmployees()` | 查询店铺员工 |
| POST | `/api/seller/shop/register` | `createShop()` | 创建店铺 |
| PUT | `/api/seller/shop/{shopId}` | `updateShop()` | 更新店铺 |
| DELETE | `/api/seller/shop/{shopId}` | `closeShop()` | 关闭店铺 |
| POST | `/api/seller/shop/{shopId}/employees/register` | `addEmployee()` | 添加店员 |
| DELETE | `/api/seller/shop/{shopId}/employees/{merchantId}` | `removeEmployee()` | 移除店员 |

### 用户端 — `/api/user/shop`

| 方法 | 路径 | Controller 方法 | 说明 |
|------|------|----------------|------|
| GET | `/api/user/shop/list` | `getShopList()` | 分页获取活跃店铺 |
| GET | `/api/user/shop/{shopId}` | `getShopDetail()` | 店铺详情 |
| GET | `/api/user/shop/{shopId}/products` | `getShopProducts()` | 分页查询店铺商品 |
| GET | `/api/user/shop/{shopId}/products/{productId}` | `getProductDetail()` | 商品详情 |

### 内部接口 — `/internal/shop`

| 方法 | 路径 | Controller 方法 | 说明 |
|------|------|----------------|------|
| GET | `/internal/shop/employees/roles/{merchantId}` | `getMerchantRoles()` | 查商家角色 |
| GET | `/internal/shop/info/{shopId}` | `getShopInfo()` | 查店铺基本信息 |
| POST | `/internal/shop/info/batch` | `batchGetShopInfo()` | 批量查店铺信息 |

## 影响分析

- **已有 API 路径变化**：商家端 4 条路径变化（去掉 `/shop/`），前端需要同步更新
- **商品操作移除**：前端不能再通过 shop-service 进行商品 CRUD，需走 product-service
- **内部接口不变**：/internal/shop 路径和返回结构不变，不影响其他微服务
- **返回格式不变**：全部保持 `ApiResponse.success()` / `ApiResponse.error()` 风格

## 不在此次范围

- 权限注解/AOP 方案（后续可迭代）
- 类型化 VO 替换 Map（后续可迭代）
- 前端路径同步（独立重构）
