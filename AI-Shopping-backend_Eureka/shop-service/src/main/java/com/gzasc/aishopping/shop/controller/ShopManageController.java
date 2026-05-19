package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.dto.order.ShipOrderRequest;
import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.feign.auth.AuthFeignClient;
import com.gzasc.aishopping.common.feign.order.OrderFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.OrderShop;
import com.gzasc.aishopping.shop.model.ProductShop;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
import com.gzasc.aishopping.shop.service.OrderShopService;
import com.gzasc.aishopping.shop.service.ProductShopService;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller/shop/manage")
@RequiredArgsConstructor
public class ShopManageController {

    private final ShopService shopService;
    private final MerchantRoleService merchantRoleService;
    private final ProductShopService productShopService;
    private final OrderShopService orderShopService;
    private final ProductFeignClient productFeignClient;
    private final OrderFeignClient orderFeignClient;
    private final AuthFeignClient authFeignClient;

    private boolean isShopOwner(String userId, String shopId) {
        return merchantRoleService.selectByMerchantShopAndRole(userId, shopId, "1") != null;
    }

    private boolean hasShopAccess(String userId, String shopId) {
        return merchantRoleService.selectByMerchantAndShop(userId, shopId) != null;
    }

    // ===== 店铺管理 =====

    @PostMapping("/shop/register")
    public Map<String, Object> createShop(
            @RequestBody Shop shop,
            @RequestHeader("X-User-Id") String userId) {
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
            @RequestHeader("X-User-Id") String userId) {
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
            @RequestHeader("X-User-Id") String userId) {
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
            @RequestHeader("X-User-Id") String userId) {
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
                productShopService.insert(ps);
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
            @RequestHeader("X-User-Id") String userId) {
        if (!isShopOwner(userId, shopId)) {
            return Map.of("success", false, "message", "仅店长可操作");
        }
        String shopIdFromDb = productShopService.selectShopIdByProductId(productId);
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
            @RequestHeader("X-User-Id") String userId) {
        if (!isShopOwner(userId, shopId)) {
            return Map.of("success", false, "message", "仅店长可操作");
        }
        String shopIdFromDb = productShopService.selectShopIdByProductId(productId);
        if (shopIdFromDb == null || !shopIdFromDb.equals(shopId)) {
            return Map.of("success", false, "message", "商品不存在");
        }
        try {
            productShopService.deleteByShopAndProduct(shopId, productId);
            productFeignClient.deleteProduct(productId);
            return Map.of("success", true, "message", "删除商品成功");
        } catch (Exception e) {
            return Map.of("success", false, "message", "删除商品失败");
        }
    }

    // ===== 员工管理 =====

    @PostMapping("/{shopId}/employees/register")
    public Map<String, Object> addEmployee(
            @PathVariable("shopId") String shopId,
            @RequestBody Map<String, String> request,
            @RequestHeader("X-User-Id") String userId) {
        String username = request.get("username");
        String password = request.get("password");
        String phone = request.get("phone");
        String name = request.get("name");

        if (username == null || username.trim().isEmpty()) {
            return Map.of("success", false, "message", "账号不能为空");
        }

        try {
            Map<String, Object> registerRequest = new java.util.HashMap<>();
            registerRequest.put("username", username);
            if (password != null && !password.trim().isEmpty()) {
                registerRequest.put("password", password);
            }
            if (phone != null && !phone.trim().isEmpty()) {
                registerRequest.put("phone", phone);
            }
            if (name != null && !name.trim().isEmpty()) {
                registerRequest.put("nickname", name);
            }

            Map<String, Object> registerResult = authFeignClient.registerEmployee(registerRequest);
            if (registerResult == null || !registerResult.containsKey("merchantId")) {
                String errorMsg = registerResult != null ? (String) registerResult.get("message") : "注册失败";
                return Map.of("success", false, "message", "添加店员失败：" + errorMsg);
            }

            String merchantId = String.valueOf(registerResult.get("merchantId"));
            MerchantRole merchantRole = new MerchantRole();
            merchantRole.setMerchantId(merchantId);
            merchantRole.setShopId(shopId);
            merchantRole.setRole("2");
            merchantRole.setAssignedBy(userId);

            int result = merchantRoleService.insert(merchantRole);
            return Map.of("success", result > 0, "message", result > 0 ? "添加店员成功" : "添加店员失败");
        } catch (Exception e) {
            return Map.of("success", false, "message", "添加店员失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{shopId}/employees/{merchantId}")
    public Map<String, Object> removeEmployee(
            @PathVariable("shopId") String shopId,
            @PathVariable("merchantId") String merchantId,
            @RequestHeader("X-User-Id") String userId) {
        if (!isShopOwner(userId, shopId)) {
            return Map.of("success", false, "message", "仅店长可移除店员");
        }
        try {
            MerchantRole mr = merchantRoleService.selectByMerchantAndShop(merchantId, shopId);
            if (mr != null) {
                merchantRoleService.deleteById(mr.getId());
            }
            return Map.of("success", true, "message", "移除店员成功");
        } catch (Exception e) {
            return Map.of("success", false, "message", "移除店员失败");
        }
    }

    // ===== 订单管理（发货） =====

    @PostMapping("/{shopId}/orders/ship")
    public Map<String, Object> shipOrder(
            @PathVariable("shopId") String shopId,
            @RequestBody ShipOrderRequest request,
            @RequestHeader("X-User-Id") String userId) {
        // 店员和店长都可以发货
        if (!hasShopAccess(userId, shopId)) {
            return Map.of("success", false, "message", "无权限访问该店铺");
        }
        // 验证订单属于该店铺
        List<OrderShop> orderShops = orderShopService.selectByOrderId(request.getOrderId());
        if (orderShops.isEmpty() || !orderShops.get(0).getShopId().equals(shopId)) {
            return Map.of("success", false, "message", "订单不存在");
        }

        try {
            Map<String, Object> result = orderFeignClient.shipOrder(request.getOrderId(), request);
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
}