package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.feign.auth.AuthFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.shop.dto.AddEmployeeRequest;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.ProductShop;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
import com.gzasc.aishopping.shop.service.ProductShopService;
import com.gzasc.aishopping.shop.service.ShopService;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.exception.ShopException;
import jakarta.validation.Valid;
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
    private final ProductFeignClient productFeignClient;
    private final AuthFeignClient authFeignClient;

    private boolean isShopOwner(String userId, String shopId) {
        return merchantRoleService.selectByMerchantShopAndRole(userId, shopId, "1") != null;
    }

    // ===== 店铺管理 =====

    @PostMapping("/shop/register")
    public ApiResponse<Map<String, Object>> createShop(
            @RequestBody @Valid CreateShopRequest request,
            @RequestHeader("X-User-Id") String userId) {
        Shop shop = new Shop();
        shop.setId(UUID.randomUUID().toString().replace("-", ""));
        shop.setMerchantId(userId);
        shop.setName(request.getName());
        shop.setDescription(request.getDescription());
        shop.setLogoId(request.getLogoId());
        shop.setStatus(1);
        int result = shopService.createShop(shop);
        if (result > 0) {
            return ApiResponse.success("创建店铺成功", Map.of("id", shop.getId()));
        }
        throw new ShopException("创建店铺失败");
    }

    @PutMapping("/shop/{shopId}")
    public ApiResponse<Map<String, Object>> updateShop(
            @PathVariable("shopId") String shopId,
            @RequestBody Shop shop,
            @RequestHeader("X-User-Id") String userId) {
        if (!isShopOwner(userId, shopId)) {
            throw new ShopException("仅店长可操作");
        }
        shop.setId(shopId);
        int result = shopService.updateShop(shop);
        if (result > 0) {
            return ApiResponse.success("更新店铺成功", null);
        }
        throw new ShopException("更新店铺失败");
    }

    @DeleteMapping("/shop/{shopId}")
    public ApiResponse<Map<String, Object>> closeShop(
            @PathVariable("shopId") String shopId,
            @RequestHeader("X-User-Id") String userId) {
        if (!isShopOwner(userId, shopId)) {
            throw new ShopException("仅店长可操作");
        }
        int result = shopService.closeShop(shopId);
        if (result > 0) {
            return ApiResponse.success("关闭店铺成功", null);
        }
        throw new ShopException("关闭店铺失败");
    }

    // ===== 商品管理 =====

    @PostMapping("/{shopId}/products")
    public ApiResponse<Map<String, Object>> createProduct(
            @PathVariable("shopId") String shopId,
            @RequestBody ProductDTO productDTO,
            @RequestHeader("X-User-Id") String userId) {
        if (!isShopOwner(userId, shopId)) {
            throw new ShopException("仅店长可操作");
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
                return ApiResponse.success("创建商品成功", Map.of("id", productId));
            }
            throw new ShopException("创建商品失败");
        } catch (Exception e) {
            throw new ShopException("创建商品失败: " + e.getMessage());
        }
    }

    @PutMapping("/{shopId}/products/{productId}")
    public ApiResponse<Map<String, Object>> updateProduct(
            @PathVariable("shopId") String shopId,
            @PathVariable("productId") String productId,
            @RequestBody ProductDTO productDTO,
            @RequestHeader("X-User-Id") String userId) {
        if (!isShopOwner(userId, shopId)) {
            throw new ShopException("仅店长可操作");
        }
        String shopIdFromDb = productShopService.selectShopIdByProductId(productId);
        if (shopIdFromDb == null || !shopIdFromDb.equals(shopId)) {
            throw new ShopException("商品不存在");
        }
        try {
            productFeignClient.updateProduct(productId, productDTO);
            return ApiResponse.success("更新商品成功", null);
        } catch (Exception e) {
            throw new ShopException("更新商品失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{shopId}/products/{productId}")
    public ApiResponse<Map<String, Object>> deleteProduct(
            @PathVariable("shopId") String shopId,
            @PathVariable("productId") String productId,
            @RequestHeader("X-User-Id") String userId) {
        if (!isShopOwner(userId, shopId)) {
            throw new ShopException("仅店长可操作");
        }
        String shopIdFromDb = productShopService.selectShopIdByProductId(productId);
        if (shopIdFromDb == null || !shopIdFromDb.equals(shopId)) {
            throw new ShopException("商品不存在");
        }
        try {
            productShopService.deleteByShopAndProduct(shopId, productId);
            productFeignClient.deleteProduct(productId);
            return ApiResponse.success("删除商品成功", null);
        } catch (Exception e) {
            throw new ShopException("删除商品失败: " + e.getMessage());
        }
    }

    // ===== 员工管理 =====

    @PostMapping("/{shopId}/employees/register")
    public ApiResponse<Map<String, Object>> addEmployee(
            @PathVariable("shopId") String shopId,
            @RequestBody @Valid AddEmployeeRequest request,
            @RequestHeader("X-User-Id") String userId) {
        try {
            Map<String, Object> registerRequest = new java.util.HashMap<>();
            registerRequest.put("username", request.getUsername());
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                registerRequest.put("password", request.getPassword());
            }
            if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
                registerRequest.put("phone", request.getPhone());
            }
            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                registerRequest.put("nickname", request.getName());
            }

            Map<String, Object> registerResult = authFeignClient.registerEmployee(registerRequest);
            if (registerResult == null || !registerResult.containsKey("merchantId")) {
                String errorMsg = registerResult != null ? (String) registerResult.get("message") : "注册失败";
                throw new ShopException("添加店员失败: " + errorMsg);
            }

            String merchantId = String.valueOf(registerResult.get("merchantId"));
            MerchantRole merchantRole = new MerchantRole();
            merchantRole.setMerchantId(merchantId);
            merchantRole.setShopId(shopId);
            merchantRole.setRole("2");
            merchantRole.setAssignedBy(userId);

            int result = merchantRoleService.insert(merchantRole);
            if (result > 0) {
                return ApiResponse.success("添加店员成功", null);
            }
            throw new ShopException("添加店员失败");
        } catch (ShopException e) {
            throw e;
        } catch (Exception e) {
            throw new ShopException("添加店员失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{shopId}/employees/{merchantId}")
    public ApiResponse<Map<String, Object>> removeEmployee(
            @PathVariable("shopId") String shopId,
            @PathVariable("merchantId") String merchantId,
            @RequestHeader("X-User-Id") String userId) {
        if (!isShopOwner(userId, shopId)) {
            throw new ShopException("仅店长可移除店员");
        }
        try {
            MerchantRole mr = merchantRoleService.selectByMerchantAndShop(merchantId, shopId);
            if (mr != null) {
                merchantRoleService.deleteById(mr.getId());
            }
            return ApiResponse.success("移除店员成功", null);
        } catch (Exception e) {
            throw new ShopException("移除店员失败: " + e.getMessage());
        }
    }

}