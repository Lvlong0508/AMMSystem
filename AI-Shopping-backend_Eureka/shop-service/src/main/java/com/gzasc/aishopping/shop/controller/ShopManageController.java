package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.shop.dto.AddEmployeeRequest;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.ShopService;
import com.gzasc.aishopping.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/seller/shop/manage")
@RequiredArgsConstructor
public class ShopManageController {

    private final ShopService shopService;

    // ===== 店铺管理 =====

    @PostMapping("/shop/register")
    public ApiResponse<Map<String, Object>> createShop(
            @RequestBody @Valid CreateShopRequest request,
            @RequestHeader("X-User-Id") String userId) {
        Shop shop = shopService.createShop(request, Long.valueOf(userId));
        return ApiResponse.success("创建店铺成功", Map.of("id", shop.getId()));
    }

    @PutMapping("/shop/{shopId}")
    public ApiResponse<Map<String, Object>> updateShop(
            @PathVariable("shopId") String shopId,
            @RequestBody Shop shop,
            @RequestHeader("X-User-Id") String userId) {
        shopService.updateShop(Long.valueOf(shopId), shop, Long.valueOf(userId));
        return ApiResponse.success("更新店铺成功", null);
    }

    @DeleteMapping("/shop/{shopId}")
    public ApiResponse<Map<String, Object>> closeShop(
            @PathVariable("shopId") String shopId,
            @RequestHeader("X-User-Id") String userId) {
        shopService.closeShop(Long.valueOf(shopId), Long.valueOf(userId));
        return ApiResponse.success("关闭店铺成功", null);
    }

    // ===== 商品管理 =====

    @PostMapping("/{shopId}/products")
    public ApiResponse<Map<String, Object>> createProduct(
            @PathVariable("shopId") String shopId,
            @RequestBody ProductDTO productDTO,
            @RequestHeader("X-User-Id") String userId) {
        shopService.createProduct(Long.valueOf(shopId), productDTO, Long.valueOf(userId));
        return ApiResponse.success("创建商品成功", null);
    }

    @PutMapping("/{shopId}/products/{productId}")
    public ApiResponse<Map<String, Object>> updateProduct(
            @PathVariable("shopId") String shopId,
            @PathVariable("productId") String productId,
            @RequestBody ProductDTO productDTO,
            @RequestHeader("X-User-Id") String userId) {
        shopService.updateProduct(Long.valueOf(shopId), Long.valueOf(productId), productDTO, Long.valueOf(userId));
        return ApiResponse.success("更新商品成功", null);
    }

    @DeleteMapping("/{shopId}/products/{productId}")
    public ApiResponse<Map<String, Object>> deleteProduct(
            @PathVariable("shopId") String shopId,
            @PathVariable("productId") String productId,
            @RequestHeader("X-User-Id") String userId) {
        shopService.deleteProduct(Long.valueOf(shopId), Long.valueOf(productId), Long.valueOf(userId));
        return ApiResponse.success("删除商品成功", null);
    }

    // ===== 员工管理 =====

    @PostMapping("/{shopId}/employees/register")
    public ApiResponse<Map<String, Object>> addEmployee(
            @PathVariable("shopId") String shopId,
            @RequestBody @Valid AddEmployeeRequest request,
            @RequestHeader("X-User-Id") String userId) {
        shopService.addEmployee(Long.valueOf(shopId), request, Long.valueOf(userId));
        return ApiResponse.success("添加店员成功", null);
    }

    @DeleteMapping("/{shopId}/employees/{merchantId}")
    public ApiResponse<Map<String, Object>> removeEmployee(
            @PathVariable("shopId") String shopId,
            @PathVariable("merchantId") String merchantId,
            @RequestHeader("X-User-Id") String userId) {
        shopService.removeEmployee(Long.valueOf(shopId), Long.valueOf(merchantId), Long.valueOf(userId));
        return ApiResponse.success("移除店员成功", null);
    }

}