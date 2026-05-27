# Shop Service Controller 层重构 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) for syntax tracking.

**Goal:** 对 shop-service 的 Controller 层进行精准重构 — 统一 URL 路径、移除商品操作、修复分层违规、修复 N+1 查询

**Architecture:** 按自底向上顺序执行：Mapper → Service → Controller。每层变更自包含，可增量验证。

**Tech Stack:** Spring Boot 3.2.3 / MyBatis / Java 17 / Maven

---

### Task 1: ShopMapper 新增批量查询方法

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\shop-service\src\main\java\com\gzasc\aishopping\shop\mapper\ShopMapper.java`

- [ ] **Step 1: 在 ShopMapper 接口中添加 selectShopsByIds 方法**

在 `countActiveShops()` 方法后面新增：

```java
@Select({"<script>",
         "SELECT * FROM shops WHERE id IN",
         "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>",
         "</script>"})
List<Shop> selectShopsByIds(@Param("ids") Collection<Long> ids);
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl shop-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add shop-service/src/main/java/com/gzasc/aishopping/shop/mapper/ShopMapper.java
git commit -m "refactor(shop): add selectShopsByIds batch query method"
```

---

### Task 2: ShopService 接口 + 实现新增方法

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\shop-service\src\main\java\com\gzasc\aishopping\shop\service\ShopService.java`
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\shop-service\src\main\java\com\gzasc\aishopping\shop\service\impl\ShopServiceImpl.java`

- [ ] **Step 1: ShopService 接口新增两个方法**

添加在 `getUserShopProductDetail` 之后：

```java
// ===== 内部接口查询（Feign 调用） =====
ShopInfoDTO getShopInfoById(Long shopId);
Map<Long, ShopInfoDTO> batchGetShopInfo(Set<Long> shopIds);
```

需要新增 import：
```java
import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import java.util.Set;
```

- [ ] **Step 2: ShopServiceImpl 实现两个方法**

在 `getProductDetailByShop` 私有方法之后、在文件末尾 `}` 之前新增：

```java
@Override
public ShopInfoDTO getShopInfoById(Long shopId) {
    Shop shop = shopMapper.selectShopById(shopId);
    if (shop == null || shop.getShopInfoId() == null) {
        return null;
    }
    ShopInfo shopInfo = shopInfoService.getById(shop.getShopInfoId());
    if (shopInfo == null) {
        return null;
    }
    return new ShopInfoDTO(shopInfo.getId(), shopInfo.getName(),
            shopInfo.getDescription(), shopInfo.getLogoUrl());
}

@Override
public Map<Long, ShopInfoDTO> batchGetShopInfo(Set<Long> shopIds) {
    if (shopIds == null || shopIds.isEmpty()) {
        return Collections.emptyMap();
    }
    // 批量查 Shop（修复 N+1）
    List<Shop> shops = shopMapper.selectShopsByIds(shopIds);
    // 收集所有 shopInfoId
    List<Long> infoIds = shops.stream()
            .map(Shop::getShopInfoId)
            .filter(Objects::nonNull)
            .toList();
    // 批量查 ShopInfo
    List<ShopInfo> shopInfos = shopInfoService.getByIds(infoIds);
    Map<Long, ShopInfo> infoMap = shopInfos.stream()
            .collect(Collectors.toMap(ShopInfo::getId, si -> si));
    // 拼装结果
    Map<Long, ShopInfoDTO> result = new HashMap<>();
    for (Shop shop : shops) {
        if (shop.getShopInfoId() == null) continue;
        ShopInfo si = infoMap.get(shop.getShopInfoId());
        if (si == null) continue;
        result.put(shop.getId(), new ShopInfoDTO(si.getId(), si.getName(),
                si.getDescription(), si.getLogoUrl()));
    }
    return result;
}
```

需要新增 import：
```java
import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import java.util.*;
import java.util.stream.Collectors;
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -pl shop-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add shop-service/src/main/java/com/gzasc/aishopping/shop/service/ShopService.java shop-service/src/main/java/com/gzasc/aishopping/shop/service/impl/ShopServiceImpl.java
git commit -m "refactor(shop): add getShopInfoById and batchGetShopInfo to ShopService"
```

---

### Task 3: InternalShopController 重构

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\shop-service\src\main\java\com\gzasc\aishopping\shop\controller\Internal\InternalShopController.java`

- [ ] **Step 1: InternalShopController 替换注入和方法实现**

将：

```java
private final MerchantRoleService merchantRoleService;
private final ShopInfoService shopInfoService;
private final ShopMapper shopMapper;
```

改为：

```java
private final MerchantRoleService merchantRoleService;
private final ShopService shopService;
```

移除 `import com.gzasc.aishopping.shop.mapper.ShopMapper;` 和 `import com.gzasc.aishopping.shop.model.ShopInfo;`，新增 `import com.gzasc.aishopping.shop.service.ShopService;`

将 `getShopInfo` 方法：

```java
@GetMapping("/info/{shopId}")
public ApiResponse<ShopInfoDTO> getShopInfo(@PathVariable("shopId") Long shopId) {
    Shop shop = shopMapper.selectShopById(shopId);
    if (shop == null || shop.getShopInfoId() == null) {
        return ApiResponse.success(null);
    }
    ShopInfo shopInfo = shopInfoService.getById(shop.getShopInfoId());
    if (shopInfo == null) {
        return ApiResponse.success(null);
    }
    ShopInfoDTO dto = new ShopInfoDTO(shopInfo.getId(), shopInfo.getName(), shopInfo.getDescription(), shopInfo.getLogoUrl());
    return ApiResponse.success(dto);
}
```

改为：

```java
@GetMapping("/info/{shopId}")
public ApiResponse<ShopInfoDTO> getShopInfo(@PathVariable("shopId") Long shopId) {
    return ApiResponse.success(shopService.getShopInfoById(shopId));
}
```

将 `batchGetShopInfo` 方法：

```java
@PostMapping("/info/batch")
public ApiResponse<Map<Long, ShopInfoDTO>> batchGetShopInfo(@RequestBody Set<Long> shopIds) {
    Map<Long, ShopInfoDTO> result = new HashMap<>();
    for (Long shopId : shopIds) {
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null || shop.getShopInfoId() == null) continue;
        ShopInfo shopInfo = shopInfoService.getById(shop.getShopInfoId());
        if (shopInfo == null) continue;
        ShopInfoDTO dto = new ShopInfoDTO(shopInfo.getId(), shopInfo.getName(), shopInfo.getDescription(), shopInfo.getLogoUrl());
        result.put(shopId, dto);
    }
    return ApiResponse.success(result);
}
```

改为：

```java
@PostMapping("/info/batch")
public ApiResponse<Map<Long, ShopInfoDTO>> batchGetShopInfo(@RequestBody Set<Long> shopIds) {
    return ApiResponse.success(shopService.batchGetShopInfo(shopIds));
}
```

移除不再使用的 import: `ShopInfo`, `ShopInfoService`, `ShopMapper` 相关导入。
移除不再使用的 field: `shopInfoService`, `shopMapper`。

完整重构后的 Controller 应为：

```java
package com.gzasc.aishopping.shop.controller.Internal;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/internal/shop")
@RequiredArgsConstructor
public class InternalShopController {

    private final MerchantRoleService merchantRoleService;
    private final ShopService shopService;

    @GetMapping("/employees/roles/{merchantId}")
    public ApiResponse<Map<String, Object>> getMerchantRoles(@PathVariable("merchantId") Long merchantId) {
        List<MerchantRole> roles = merchantRoleService.selectByMerchantId(merchantId);
        return ApiResponse.success(Map.of("roles", roles));
    }

    @GetMapping("/info/{shopId}")
    public ApiResponse<ShopInfoDTO> getShopInfo(@PathVariable("shopId") Long shopId) {
        return ApiResponse.success(shopService.getShopInfoById(shopId));
    }

    @PostMapping("/info/batch")
    public ApiResponse<Map<Long, ShopInfoDTO>> batchGetShopInfo(@RequestBody Set<Long> shopIds) {
        return ApiResponse.success(shopService.batchGetShopInfo(shopIds));
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl shop-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add shop-service/src/main/java/com/gzasc/aishopping/shop/controller/Internal/InternalShopController.java
git commit -m "refactor(shop): clean InternalShopController - use ShopService instead of direct Mapper"
```

---

### Task 4: ShopMerchantController 重构 — URL 路径修正 + 移除商品操作

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\shop-service\src\main\java\com\gzasc\aishopping\shop\controller\ShopMerchantController.java`

- [ ] **Step 1: ShopMerchantController 重构**

完整重构后的 Controller：

```java
package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.dto.AddEmployeeRequest;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.dto.UpdateShopRequest;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/seller/shop")
@RequiredArgsConstructor
public class ShopMerchantController {

    private final ShopService shopService;

    @GetMapping("/{shopId}")
    public ApiResponse<Map<String, Object>> getShop(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(Map.of("shop", shopService.getShopWithAccessCheck(shopId, userId)));
    }

    @GetMapping("/{shopId}/products")
    public ApiResponse<Map<String, Object>> getProducts(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ApiResponse.success(shopService.getShopProductsWithDetails(shopId, userId, page, size));
    }

    @GetMapping("/{shopId}/employees")
    public ApiResponse<Map<String, Object>> getEmployees(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(shopService.getShopEmployees(shopId, userId));
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> createShop(
            @RequestBody @Valid CreateShopRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        Shop shop = shopService.createShop(request, userId);
        return ApiResponse.success("创建店铺成功", Map.of("id", shop.getId()));
    }

    @PutMapping("/{shopId}")
    public ApiResponse<Map<String, Object>> updateShop(
            @PathVariable("shopId") Long shopId,
            @RequestBody @Valid UpdateShopRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        shopService.updateShop(shopId, request, userId);
        return ApiResponse.success("更新店铺成功", null);
    }

    @DeleteMapping("/{shopId}")
    public ApiResponse<Map<String, Object>> closeShop(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        shopService.closeShop(shopId, userId);
        return ApiResponse.success("关闭店铺成功", null);
    }

    @PostMapping("/{shopId}/employees/register")
    public ApiResponse<Map<String, Object>> addEmployee(
            @PathVariable("shopId") Long shopId,
            @RequestBody @Valid AddEmployeeRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        shopService.addEmployee(shopId, request, userId);
        return ApiResponse.success("添加店员成功", null);
    }

    @DeleteMapping("/{shopId}/employees/{merchantId}")
    public ApiResponse<Map<String, Object>> removeEmployee(
            @PathVariable("shopId") Long shopId,
            @PathVariable("merchantId") Long merchantId,
            @RequestHeader("X-User-Id") Long userId) {
        shopService.removeEmployee(shopId, merchantId, userId);
        return ApiResponse.success("移除店员成功", null);
    }
}
```

主要变更：
1. `@GetMapping("/shop/{shopId}")` → `@GetMapping("/{shopId}")`
2. `@PostMapping("/shop/register")` → `@PostMapping("/register")`
3. `@PutMapping("/shop/{shopId}")` → `@PutMapping("/{shopId}")`
4. `@DeleteMapping("/shop/{shopId}")` → `@DeleteMapping("/{shopId}")`
5. 移除了 `createProduct()` / `updateProduct()` / `deleteProduct()` 三个方法
6. 移除了 `ProductFeignClient` / `MerchantRoleService` 注入 + 相关 import
7. 移除了 `checkShopOwner()` 私有方法

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl shop-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add shop-service/src/main/java/com/gzasc/aishopping/shop/controller/ShopMerchantController.java
git commit -m "refactor(shop): fix URLs and remove product CRUD from ShopMerchantController"
```

---

### Task 5: 全局验证

**Files:** （无需修改文件，仅验证）

- [ ] **Step 1: 完整编译**

Run: `mvn compile -pl shop-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 2: 检查 Feign 调用不受影响**

确认：
- `ShopFeignClient` 调用的 `/internal/shop/info/{shopId}` 路径不变 ✅
- `ShopFeignClient` 调用的 `/internal/shop/info/batch` 路径不变 ✅
- `ShopFeignClientForRoles` 调用的 `/internal/shop/employees/roles/{merchantId}` 路径不变 ✅

- [ ] **Step 3: 最终提交**

```bash
git add -A
git status
git commit -m "refactor(shop): controller layer cleanup - URL paths, service layering, N+1 fix"
```
