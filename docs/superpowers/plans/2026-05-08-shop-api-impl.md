# 缺失接口实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 ShopSellerController、ShopUserController 增加缺失的店铺商品/订单/店员管理接口，并在网关注册内部路由

**Architecture:** shop-service 通过 RestTemplate 调用 product-service、order-service 的内部接口完成跨服务操作，网关增加 /internal/** 路由绕过认证

**Tech Stack:** Spring Cloud Gateway, RestTemplate, MyBatis

---

## 文件结构

- Modify: `AI-Shopping-backend_Eureka/shop-service/src/main/java/com/gzasc/aishopping/shop/controller/ShopSellerController.java`
- Modify: `AI-Shopping-backend_Eureka/shop-service/src/main/java/com/gzasc/aishopping/shop/controller/ShopUserController.java`
- Modify: `AI-Shopping-backend_Eureka/gateway-service/src/main/resources/application.yml`

---

### Task 1: ShopSellerController 增加商品管理接口

**Files:**
- Modify: `AI-Shopping-backend_Eureka/shop-service/src/main/java/com/gzasc/aishopping/shop/controller/ShopSellerController.java:119`

- [ ] **Step 1: 添加商品管理接口**

在 ShopSellerController 末尾追加以下接口（需注入 ProductShopMapper 和 RestTemplate）：

```java
private final ProductShopMapper productShopMapper;
private final org.springframework.web.client.RestTemplate restTemplate;

@GetMapping("/{shopId}/products")
public Map<String, Object> getShopProducts(@PathVariable("shopId") String shopId) {
    try {
        List<ProductShop> productShops = productShopMapper.selectByShopId(shopId);
        return Map.of("success", true, "products", productShops, "total", productShops.size());
    } catch (Exception e) {
        return Map.of("success", false, "message", "查询商品失败");
    }
}

@PostMapping("/{shopId}/products")
public Map<String, Object> createProduct(
        @PathVariable("shopId") String shopId,
        @RequestHeader(value = "X-User-Id", required = false) String userId,
        @RequestBody Map<String, Object> productData) {
    if (userId == null || userId.trim().isEmpty()) {
        return Map.of("success", false, "message", "未提供用户ID");
    }
    if (merchantRoleMapper.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
        return Map.of("success", false, "message", "无权限操作");
    }
    try {
        String url = "http://shop-service/internal/product/create";
        @SuppressWarnings("unchecked")
        Map<String, String> response = restTemplate.postForObject(url, productData, Map.class);
        if (response != null && "创建商品成功".equals(response.get("message"))) {
            String productId = response.get("id");
            ProductShop ps = new ProductShop();
            ps.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
            ps.setProductId(productId);
            ps.setShopId(shopId);
            productShopMapper.insert(ps);
            return Map.of("success", true, "message", "创建商品成功", "id", productId);
        }
        return Map.of("success", false, "message", response != null ? response.get("message") : "创建商品失败");
    } catch (Exception e) {
        return Map.of("success", false, "message", "创建商品失败：" + e.getMessage());
    }
}

@GetMapping("/{shopId}/products/{productId}")
public Map<String, Object> getProductDetail(
        @PathVariable("shopId") String shopId,
        @PathVariable("productId") String productId) {
    try {
        String shopIdFromDb = productShopMapper.selectShopIdByProductId(productId);
        if (shopIdFromDb == null || !shopIdFromDb.equals(shopId)) {
            return Map.of("success", false, "message", "商品不存在");
        }
        String url = "http://product-service/internal/product/" + productId;
        Object product = restTemplate.getForObject(url, Object.class);
        return Map.of("success", true, "product", product);
    } catch (Exception e) {
        return Map.of("success", false, "message", "查询商品失败");
    }
}

@PutMapping("/{shopId}/products/{productId}")
public Map<String, Object> updateProduct(
        @PathVariable("shopId") String shopId,
        @PathVariable("productId") String productId,
        @RequestHeader(value = "X-User-Id", required = false) String userId,
        @RequestBody Map<String, Object> productData) {
    if (userId == null || userId.trim().isEmpty()) {
        return Map.of("success", false, "message", "未提供用户ID");
    }
    if (merchantRoleMapper.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
        return Map.of("success", false, "message", "无权限操作");
    }
    try {
        String url = "http://product-service/internal/product/" + productId;
        restTemplate.put(url, productData);
        return Map.of("success", true, "message", "更新商品成功");
    } catch (Exception e) {
        return Map.of("success", false, "message", "更新商品失败");
    }
}

@DeleteMapping("/{shopId}/products/{productId}")
public Map<String, Object> deleteProduct(
        @PathVariable("shopId") String shopId,
        @PathVariable("productId") String productId,
        @RequestHeader(value = "X-User-Id", required = false) String userId) {
    if (userId == null || userId.trim().isEmpty()) {
        return Map.of("success", false, "message", "未提供用户ID");
    }
    if (merchantRoleMapper.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
        return Map.of("success", false, "message", "无权限操作");
    }
    try {
        productShopMapper.deleteByShopAndProduct(shopId, productId);
        String url = "http://product-service/internal/product/" + productId;
        restTemplate.delete(url);
        return Map.of("success", true, "message", "删除商品成功");
    } catch (Exception e) {
        return Map.of("success", false, "message", "删除商品失败");
    }
}
```

---

### Task 2: ShopSellerController 增加订单/店员管理接口

**Files:**
- Modify: `AI-Shopping-backend_Eureka/shop-service/src/main/java/com/gzasc/aishopping/shop/controller/ShopSellerController.java`

- [ ] **Step 2: 添加订单管理接口**

```java
@GetMapping("/{shopId}/orders/all")
public Map<String, Object> getShopOrders(@PathVariable("shopId") String shopId) {
    try {
        List<OrderShop> orderShops = orderShopMapper.selectByShopId(shopId);
        return Map.of("success", true, "orders", orderShops, "total", orderShops.size());
    } catch (Exception e) {
        return Map.of("success", false, "message", "查询订单失败");
    }
}

@GetMapping("/{shopId}/orders/{orderId}")
public Map<String, Object> getOrderDetail(
        @PathVariable("shopId") String shopId,
        @PathVariable("orderId") String orderId) {
    try {
        List<OrderShop> orderShops = orderShopMapper.selectByOrderId(orderId);
        if (orderShops.isEmpty() || !orderShops.get(0).getShopId().equals(shopId)) {
            return Map.of("success", false, "message", "订单不存在");
        }
        String url = "http://order-service/internal/order/" + orderId;
        Object order = restTemplate.getForObject(url, Object.class);
        return Map.of("success", true, "order", order);
    } catch (Exception e) {
        return Map.of("success", false, "message", "查询订单失败");
    }
}
```

- [ ] **Step 3: 添加店员管理接口**

```java
private final OrderShopMapper orderShopMapper;

@GetMapping("/{shopId}/employees")
public Map<String, Object> getEmployees(@PathVariable("shopId") String shopId) {
    try {
        List<MerchantRole> employees = merchantRoleMapper.selectByShopId(shopId);
        return Map.of("success", true, "employees", employees, "total", employees.size());
    } catch (Exception e) {
        return Map.of("success", false, "message", "查询店员失败");
    }
}

@PostMapping("/{shopId}/employees/register")
public Map<String, Object> addEmployee(
        @PathVariable("shopId") String shopId,
        @RequestHeader(value = "X-User-Id", required = false) String userId,
        @RequestBody MerchantRole merchantRole) {
    if (userId == null || userId.trim().isEmpty()) {
        return Map.of("success", false, "message", "未提供用户ID");
    }
    if (merchantRoleMapper.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
        return Map.of("success", false, "message", "仅店长可添加店员");
    }
    try {
        merchantRole.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
        merchantRole.setShopId(shopId);
        merchantRole.setAssignedBy(userId);
        int result = merchantRoleMapper.insert(merchantRole);
        return Map.of("success", result > 0, "message", result > 0 ? "添加店员成功" : "添加店员失败");
    } catch (Exception e) {
        return Map.of("success", false, "message", "添加店员失败：" + e.getMessage());
    }
}

@DeleteMapping("/{shopId}/employees/{merchantId}")
public Map<String, Object> removeEmployee(
        @PathVariable("shopId") String shopId,
        @PathVariable("merchantId") String merchantId,
        @RequestHeader(value = "X-User-Id", required = false) String userId) {
    if (userId == null || userId.trim().isEmpty()) {
        return Map.of("success", false, "message", "未提供用户ID");
    }
    if (merchantRoleMapper.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
        return Map.of("success", false, "message", "仅店长可移除店员");
    }
    try {
        MerchantRole mr = merchantRoleMapper.selectByMerchantAndShop(merchantId, shopId);
        if (mr != null) {
            merchantRoleMapper.deleteById(mr.getId());
        }
        return Map.of("success", true, "message", "移除店员成功");
    } catch (Exception e) {
        return Map.of("success", false, "message", "移除店员失败");
    }
}
```

---

### Task 3: ShopUserController 增加商品查看接口

**Files:**
- Modify: `AI-Shopping-backend_Eureka/shop-service/src/main/java/com/gzasc/aishopping/shop/controller/ShopUserController.java:51`

- [ ] **Step 4: 添加商品查看接口**

在 ShopUserController 末尾添加：

```java
private final ProductShopMapper productShopMapper;
private final org.springframework.web.client.RestTemplate restTemplate;

@GetMapping("/{shopId}/products")
public Map<String, Object> getShopProducts(
        @PathVariable("shopId") String shopId,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "10") int size) {
    try {
        Shop shop = shopService.getShopById(shopId);
        if (shop == null || shop.getStatus() != 1) {
            return Map.of("success", false, "message", "店铺不存在");
        }
        List<ProductShop> productShops = productShopMapper.selectByShopId(shopId);
        int start = (page - 1) * size;
        int end = Math.min(start + size, productShops.size());
        List<ProductShop> paged = productShops.subList(start, end);
        return Map.of("success", true, "products", paged, "total", productShops.size(), "page", page, "size", size);
    } catch (Exception e) {
        return Map.of("success", false, "message", "查询商品失败");
    }
}

@GetMapping("/{shopId}/products/{productId}")
public Map<String, Object> getProductDetail(
        @PathVariable("shopId") String shopId,
        @PathVariable("productId") String productId) {
    try {
        Shop shop = shopService.getShopById(shopId);
        if (shop == null || shop.getStatus() != 1) {
            return Map.of("success", false, "message", "店铺不存在");
        }
        String shopIdFromDb = productShopMapper.selectShopIdByProductId(productId);
        if (shopIdFromDb == null || !shopIdFromDb.equals(shopId)) {
            return Map.of("success", false, "message", "商品不存在");
        }
        String url = "http://product-service/internal/product/" + productId;
        Object product = restTemplate.getForObject(url, Object.class);
        return Map.of("success", true, "product", product);
    } catch (Exception e) {
        return Map.of("success", false, "message", "查询商品失败");
    }
}
```

---

### Task 4: 网关增加内部路由

**Files:**
- Modify: `AI-Shopping-backend_Eureka/gateway-service/src/main/resources/application.yml:142-150`

- [ ] **Step 5: 在 internal 路由区域追加内部路由**

在 `# ========== 内部路由 ==========` 部分，internal-shop 路由之后添加：

```yaml
        # 内部认证服务 - 绕过Sa-Token校验
        - id: internal-auth
          uri: lb://auth-service
          predicates:
            - Path=/internal/auth/**
          filters:
            - StripPrefix=1

        # 内部商品服务
        - id: internal-product
          uri: lb://product-service
          predicates:
            - Path=/internal/product/**
          filters:
            - StripPrefix=1

        # 内部订单服务
        - id: internal-order
          uri: lb://order-service
          predicates:
            - Path=/internal/order/**
          filters:
            - StripPrefix=1
```

- [ ] **Step 6: 验证并测试**

使用 Postman 或浏览器测试各接口返回正常 JSON。

---

## 自检

1. **Spec覆盖检查：** 所有 14 个接口均已覆盖
2. **Placeholder扫描：** 无 TBD/TODO 占位符
3. **类型一致性：** RestTemplate 统一使用 `restTemplate`，Mapper 方法名与现有代码一致
