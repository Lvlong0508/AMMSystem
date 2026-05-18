# Shop 服务 Controller 层重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 shop 服务的单一 ShopSellerController 拆分为 ShopUserController、ShopQueryController、ShopManageController，按 query/manage 路径分离便于网关权限控制。

**Architecture:** 保持现有 Service、Mapper 不变，只在 Controller 层重新组织代码。ShopQueryController 处理查询类接口，ShopManageController 处理管理类接口。

**Tech Stack:** Java Spring Boot, MyBatis, Feign Client

---

## 文件结构

```
shop-service/src/main/java/com/gzasc/aishopping/shop/controller/
├── ShopUserController.java      (现有，修改)
├── ShopQueryController.java     (新建)
├── ShopManageController.java    (新建)
├── ShopInternalController.java  (保留，内部服务)
└── InternalShopController.java  (保留，内部服务)
```

---

## Task 1: 创建 ShopQueryController

**Files:**
- Create: `AI-Shopping-backend_Eureka/shop-service/src/main/java/com/gzasc/aishopping/shop/controller/ShopQueryController.java`

- [ ] **Step 1: 创建 ShopQueryController.java**

```java
package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.feign.contact.ContactFeignClient;
import com.gzasc.aishopping.common.feign.order.OrderFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.shop.mapper.MerchantRoleMapper;
import com.gzasc.aishopping.shop.mapper.OrderShopMapper;
import com.gzasc.aishopping.shop.mapper.ProductShopMapper;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.OrderShop;
import com.gzasc.aishopping.shop.model.ProductShop;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/shop/query")
@RequiredArgsConstructor
public class ShopQueryController {

    private final ShopService shopService;
    private final MerchantRoleMapper merchantRoleMapper;
    private final ProductShopMapper productShopMapper;
    private final OrderShopMapper orderShopMapper;
    private final ProductFeignClient productFeignClient;
    private final OrderFeignClient orderFeignClient;
    private final ContactFeignClient contactFeignClient;

    // 权限验证方法
    private boolean hasShopAccess(String userId, String shopId) {
        return merchantRoleMapper.selectByMerchantAndShop(userId, shopId) != null;
    }

    @GetMapping("/shop/{shopId}")
    public Map<String, Object> getShop(
            @PathVariable("shopId") String shopId,
            @RequestHeader("userId") String userId) {
        if (!hasShopAccess(userId, shopId)) {
            return Map.of("success", false, "message", "无权限访问该店铺");
        }
        Shop shop = shopService.getShopById(shopId);
        if (shop == null) {
            return Map.of("success", false, "message", "店铺不存在");
        }
        return Map.of("success", true, "shop", shop);
    }

    @GetMapping("/{shopId}/products")
    public Map<String, Object> getProducts(
            @PathVariable("shopId") String shopId,
            @RequestHeader("userId") String userId) {
        if (!hasShopAccess(userId, shopId)) {
            return Map.of("success", false, "message", "无权限访问该店铺");
        }
        List<ProductShop> productShops = productShopMapper.selectByShopId(shopId);
        List<Map<String, Object>> products = new ArrayList<>();
        for (ProductShop ps : productShops) {
            try {
                Map<String, Object> productMap = productFeignClient.getProductById(ps.getProductId());
                if (productMap != null && productMap.containsKey("id")) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("productId", productMap.get("id"));
                    detail.put("name", productMap.get("name"));
                    detail.put("description", productMap.get("description"));
                    detail.put("price", productMap.get("price"));
                    detail.put("stock", productMap.get("stock"));
                    products.add(detail);
                }
            } catch (Exception e) {
                // 忽略
            }
        }
        return Map.of("success", true, "products", products, "total", products.size());
    }

    @GetMapping("/{shopId}/orders")
    public Map<String, Object> getOrders(
            @PathVariable("shopId") String shopId,
            @RequestHeader("userId") String userId) {
        if (!hasShopAccess(userId, shopId)) {
            return Map.of("success", false, "message", "无权限访问该店铺");
        }
        List<OrderShop> orderShops = orderShopMapper.selectByShopId(shopId);
        if (orderShops.isEmpty()) {
            return Map.of("success", true, "orders", new ArrayList<>(), "total", 0);
        }
        
        List<Map<String, Object>> orderDetails = new ArrayList<>();
        for (OrderShop os : orderShops) {
            try {
                Map<String, Object> orderMap = orderFeignClient.getOrderById(os.getOrderId());
                if (orderMap != null && orderMap.containsKey("order")) {
                    Map<String, Object> orderData = (Map<String, Object>) orderMap.get("order");
                    orderData.put("orderId", os.getOrderId());
                    
                    // 商品名称
                    Object productIdObj = orderData.get("productId");
                    if (productIdObj != null) {
                        try {
                            Map<String, Object> productMap = productFeignClient.getProductById(String.valueOf(productIdObj));
                            if (productMap != null && productMap.containsKey("name")) {
                                orderData.put("productName", productMap.get("name"));
                            }
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                    
                    // 收货地址
                    Object contactIdObj = orderData.get("contactId");
                    if (contactIdObj != null && contactIdObj instanceof Number) {
                        int contactId = ((Number) contactIdObj).intValue();
                        if (contactId > 0) {
                            try {
                                Map<String, Object> contactMap = contactFeignClient.getContactByIdWithUser(contactId, userId);
                                if (contactMap != null && contactMap.containsKey("data")) {
                                    Map<String, Object> contactData = (Map<String, Object>) contactMap.get("data");
                                    Map<String, Object> contact = new HashMap<>();
                                    contact.put("name", contactData.get("name"));
                                    contact.put("phone", contactData.get("phone"));
                                    contact.put("address", contactData.get("address"));
                                    orderData.put("contact", contact);
                                }
                            } catch (Exception e) {
                                // 忽略
                            }
                        }
                    }
                    
                    // 物流信息在 orderData 中已包含
                    orderDetails.add(orderData);
                }
            } catch (Exception e) {
                // 忽略
            }
        }
        return Map.of("success", true, "orders", orderDetails, "total", orderDetails.size());
    }

    @GetMapping("/{shopId}/orders/{orderId}")
    public Map<String, Object> getOrderDetail(
            @PathVariable("shopId") String shopId,
            @PathVariable("orderId") String orderId,
            @RequestHeader("userId") String userId) {
        if (!hasShopAccess(userId, shopId)) {
            return Map.of("success", false, "message", "无权限访问该店铺");
        }
        List<OrderShop> orderShops = orderShopMapper.selectByOrderId(orderId);
        if (orderShops.isEmpty() || !orderShops.get(0).getShopId().equals(shopId)) {
            return Map.of("success", false, "message", "订单不存在");
        }
        Map<String, Object> order = orderFeignClient.getOrderById(orderId);
        return Map.of("success", true, "order", order);
    }

    @GetMapping("/{shopId}/employees")
    public Map<String, Object> getEmployees(
            @PathVariable("shopId") String shopId,
            @RequestHeader("userId") String userId) {
        if (!hasShopAccess(userId, shopId)) {
            return Map.of("success", false, "message", "无权限访问该店铺");
        }
        List<MerchantRole> employees = merchantRoleMapper.selectByShopId(shopId);
        return Map.of("success", true, "employees", employees, "total", employees.size());
    }
}
```

- [ ] **Step 2: Commit**

---

## Task 2: 创建 ShopManageController

**Files:**
- Create: `AI-Shopping-backend_Eureka/shop-service/src/main/java/com/gzasc/aishopping/shop/controller/ShopManageController.java`

- [ ] **Step 1: 创建 ShopManageController.java**

```java
package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.feign.auth.AuthFeignClient;
import com.gzasc.aishopping.common.feign.auth.UserInfoFeignClient;
import com.gzasc.aishopping.common.feign.logistics.LogisticsFeignClient;
import com.gzasc.aishopping.common.feign.order.OrderFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.shop.dto.ShipOrderRequest;
import com.gzasc.aishopping.shop.mapper.MerchantRoleMapper;
import com.gzasc.aishopping.shop.mapper.OrderShopMapper;
import com.gzasc.aishopping.shop.mapper.ProductShopMapper;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.OrderShop;
import com.gzasc.aishopping.shop.model.ProductShop;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller/shop/manage")
@RequiredArgsConstructor
public class ShopManageController {

    private final ShopService shopService;
    private final MerchantRoleMapper merchantRoleMapper;
    private final ProductShopMapper productShopMapper;
    private final OrderShopMapper orderShopMapper;
    private final ProductFeignClient productFeignClient;
    private final OrderFeignClient orderFeignClient;
    private final LogisticsFeignClient logisticsFeignClient;
    private final AuthFeignClient authFeignClient;
    private final UserInfoFeignClient userInfoFeignClient;

    // 验证店长权限
    private boolean isShopOwner(String userId, String shopId) {
        return merchantRoleMapper.selectByMerchantShopAndRole(userId, shopId, "1") != null;
    }

    // 验证店铺成员权限（店长或店员）
    private boolean hasShopAccess(String userId, String shopId) {
        return merchantRoleMapper.selectByMerchantAndShop(userId, shopId) != null;
    }

    // ===== 店铺管理 =====

    @PostMapping("/shop/register")
    public Map<String, Object> createShop(
            @RequestBody Shop shop,
            @RequestHeader("userId") String userId) {
        if (shop == null || shop.getName() == null || shop.getName().trim().isEmpty()) {
            return Map.of("success", false, "message", "店铺名称不能为空");
        }
        shop.setId(UUID.randomUUID().toString().replace("-", ""));
        shop.setMerchantId(userId);
        shop.setStatus(1);
        int result = shopService.createShop(shop);
        if (result > 0) {
            return Map.of("success", true, "message", "创建店铺成功", "id", shop.getId());
        }
        return Map.of("success", false, "message", "创建店铺失败");
    }

    @PutMapping("/shop/{shopId}")
    public Map<String, Object> updateShop(
            @PathVariable("shopId") String shopId,
            @RequestBody Shop shop,
            @RequestHeader("userId") String userId) {
        if (!isShopOwner(userId, shopId)) {
            return Map.of("success", false, "message", "仅店长可操作");
        }
        shop.setId(shopId);
        int result = shopService.updateShop(shop);
        if (result > 0) {
            return Map.of("success", true, "message", "更新店铺成功");
        }
        return Map.of("success", false, "message", "更新店铺失败");
    }

    @DeleteMapping("/shop/{shopId}")
    public Map<String, Object> closeShop(
            @PathVariable("shopId") String shopId,
            @RequestHeader("userId") String userId) {
        if (!isShopOwner(userId, shopId)) {
            return Map.of("success", false, "message", "仅店长可操作");
        }
        int result = shopService.closeShop(shopId);
        if (result > 0) {
            return Map.of("success", true, "message", "关闭店铺成功");
        }
        return Map.of("success", false, "message", "关闭店铺失败");
    }

    // ===== 商品管理 =====

    @PostMapping("/{shopId}/products")
    public Map<String, Object> createProduct(
            @PathVariable("shopId") String shopId,
            @RequestBody ProductDTO productDTO,
            @RequestHeader("userId") String userId) {
        if (!isShopOwner(userId, shopId)) {
            return Map.of("success", false, "message", "仅店长可操作");
        }
        try {
            Map<String, Object> result = productFeignClient.createProduct(productDTO);
            if (result != null && "创建商品成功".equals(result.get("message"))) {
                String productId = (String) result.get("id");
                ProductShop ps = new ProductShop();
                ps.setId(UUID.randomUUID().toString().replace("-", ""));
                ps.setProductId(productId);
                ps.setShopId(shopId);
                productShopMapper.insert(ps);
                return Map.of("success", true, "message", "创建商品成功", "id", productId);
            }
            return Map.of("success", false, "message", "创建商品失败");
        } catch (Exception e) {
            return Map.of("success", false, "message", "创建商品失败：" + e.getMessage());
        }
    }

    @PutMapping("/{shopId}/products/{productId}")
    public Map<String, Object> updateProduct(
            @PathVariable("shopId") String shopId,
            @PathVariable("productId") String productId,
            @RequestBody ProductDTO productDTO,
            @RequestHeader("userId") String userId) {
        if (!isShopOwner(userId, shopId)) {
            return Map.of("success", false, "message", "仅店长可操作");
        }
        String shopIdFromDb = productShopMapper.selectShopIdByProductId(productId);
        if (shopIdFromDb == null || !shopIdFromDb.equals(shopId)) {
            return Map.of("success", false, "message", "商品不存在");
        }
        try {
            productFeignClient.updateProduct(productId, productDTO);
            return Map.of("success", true, "message", "更新商品成功");
        } catch (Exception e) {
            return Map.of("success", false, "message", "更新商品失败");
        }
    }

    @DeleteMapping("/{shopId}/products/{productId}")
    public Map<String, Object> deleteProduct(
            @PathVariable("shopId") String shopId,
            @PathVariable("productId") String productId,
            @RequestHeader("userId") String userId) {
        if (!isShopOwner(userId, shopId)) {
            return Map.of("success", false, "message", "仅店长可操作");
        }
        String shopIdFromDb = productShopMapper.selectShopIdByProductId(productId);
        if (shopIdFromDb == null || !shopIdFromDb.equals(shopId)) {
            return Map.of("success", false, "message", "商品不存在");
        }
        try {
            productShopMapper.deleteByShopAndProduct(shopId, productId);
            productFeignClient.deleteProduct(productId);
            return Map.of("success", true, "message", "删除商品成功");
        } catch (Exception e) {
            return Map.of("success", false, "message", "删除商品失败");
        }
    }

    // ===== 订单管理（发货） =====

    @PostMapping("/{shopId}/orders/ship")
    public Map<String, Object> shipOrder(
            @PathVariable("shopId") String shopId,
            @RequestBody ShipOrderRequest request,
            @RequestHeader("userId") String userId) {
        // 店员和店长都可以发货
        if (!hasShopAccess(userId, shopId)) {
            return Map.of("success", false, "message", "无权限访问该店铺");
        }
        // 验证订单属于该店铺
        List<OrderShop> orderShops = orderShopMapper.selectByOrderId(request.getOrderId());
        if (orderShops.isEmpty() || !orderShops.get(0).getShopId().equals(shopId)) {
            return Map.of("success", false, "message", "订单不存在");
        }
        
        try {
            // 调用 order-service 发货
            Map<String, Object> result = orderFeignClient.shipOrder(
                request.getOrderId(),
                request.getTrackingNumber(),
                request.getContactId(),
                request.getShippingDate()
            );
            if (result != null && result.containsKey("message")) {
                String message = (String) result.get("message");
                if (message.contains("成功")) {
                    return Map.of("success", true, "message", message);
                }
                return Map.of("success", false, "message", message);
            }
            return Map.of("success", false, "message", "发货失败");
        } catch (Exception e) {
            return Map.of("success", false, "message", "发货失败：" + e.getMessage());
        }
    }

    // ===== 员工管理 =====

    @PostMapping("/{shopId}/employees/register")
    public Map<String, Object> addEmployee(
            @PathVariable("shopId") String shopId,
            @RequestBody Map<String, String> request,
            @RequestHeader("userId") String userId) {
        if (!isShopOwner(userId, shopId)) {
            return Map.of("success", false, "message", "仅店长可添加店员");
        }

        String username = request.get("username");
        String password = request.get("password");
        String phone = request.get("phone");
        String name = request.get("name");

        if (username == null || username.trim().isEmpty()) {
            return Map.of("success", false, "message", "账号不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Map.of("success", false, "message", "密码不能为空");
        }

        try {
            Integer infoId = null;
            if (name != null && !name.trim().isEmpty()) {
                Map<String, String> userInfoRequest = new HashMap<>();
                userInfoRequest.put("nickname", name);
                Map<String, Object> userInfoResult = userInfoFeignClient.createUserInfo(userInfoRequest);
                if (userInfoResult != null && Boolean.TRUE.equals(userInfoResult.get("success"))) {
                    infoId = (Integer) userInfoResult.get("infoId");
                }
            }

            Map<String, Object> registerRequest = new HashMap<>();
            registerRequest.put("username", username);
            registerRequest.put("password", password);
            if (phone != null && !phone.trim().isEmpty()) {
                registerRequest.put("phone", phone);
            }
            if (infoId != null) {
                registerRequest.put("infoId", infoId);
            }

            Map<String, Object> registerResult = authFeignClient.registerEmployee(registerRequest);
            if (registerResult == null || !registerResult.containsKey("merchantId")) {
                return Map.of("success", false, "message", "注册店员失败");
            }

            String merchantId = String.valueOf(registerResult.get("merchantId"));
            MerchantRole merchantRole = new MerchantRole();
            merchantRole.setId(UUID.randomUUID().toString().replace("-", ""));
            merchantRole.setMerchantId(merchantId);
            merchantRole.setShopId(shopId);
            merchantRole.setRole("2");
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
            @RequestHeader("userId") String userId) {
        if (!isShopOwner(userId, shopId)) {
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
}
```

- [ ] **Step 2: 创建 ShipOrderRequest DTO**

需要创建 DTO 文件: `shop-service/src/main/java/com/gzasc/aishopping/shop/dto/ShipOrderRequest.java`

```java
package com.gzasc.aishopping.shop.dto;

import lombok.Data;

@Data
public class ShipOrderRequest {
    private String orderId;
    private String trackingNumber;
    private Integer contactId;
    private String shippingDate;
}
```

- [ ] **Step 3: Commit**

---

## Task 3: 修改 ShopUserController

**Files:**
- Modify: `AI-Shopping-backend_Eureka/shop-service/src/main/java/com/gzasc/aishopping/shop/controller/ShopUserController.java`

- [ ] **Step 1: 添加 userId 请求头验证**

在每个接口添加 `@RequestHeader("userId") String userId` 参数，并验证用户登录。

- [ ] **Step 2: Commit**

---

## Task 4: 修改网关权限配置

**Files:**
- Modify: `AI-Shopping-backend_Eureka/gateway-service/src/main/java/com/gzasc/aishopping/gateway/filter/SaTokenAuthGlobalFilter.java`

- [ ] **Step 1: 更新 isShopOwnerOnlyApi 方法**

```java
private boolean isShopOwnerOnlyApi(String path) {
    // 查询类接口：店铺成员可访问
    if (pathMatcher.match("/api/seller/shop/query/**", path)) {
        return false;
    }
    // 管理类接口：仅店长可访问，但发货接口店员可访问
    if (pathMatcher.match("/api/seller/shop/manage/**/ship", path)) {
        return false;
    }
    return pathMatcher.match("/api/seller/shop/manage/**", path) ||
           pathMatcher.match("/api/seller/shop/register", path) ||
           pathMatcher.match("/api/seller/shop/*", path) ||
           pathMatcher.match("/api/seller/shop/*/products/*", path) ||
           pathMatcher.match("/api/seller/shop/*/employees/register", path) ||
           pathMatcher.match("/api/seller/shop/*/employees/*", path) ||
           pathMatcher.match("/api/seller/shop/*/addresses/*", path);
}
```

- [ ] **Step 2: 更新 userId 请求头传递**

确保网关将 loginId 传递给下游服务。

- [ ] **Step 3: Commit**

---

## Task 5: 更新前端 API 调用

**Files:**
- Modify: `AI-Shopping-frontier/frontier-seller/src/api/shop.js`

- [ ] **Step 1: 更新 API 路径**

将接口路径从 `/api/seller/shop/**` 更新为:
- 查询类: `/api/seller/shop/query/**`
- 管理类: `/api/seller/shop/manage/**`

- [ ] **Step 2: Commit**

---

**Plan complete and saved to `docs/superpowers/plans/2026-05-17-shop-controller-refactor-plan.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**