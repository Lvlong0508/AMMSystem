package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.dto.AddEmployeeRequest;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.dto.UpdateShopRequest;
import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/shop")
@RequiredArgsConstructor
public class ShopMerchantController {

    private final ShopService shopService;

    @GetMapping("/merchant/{merchantId}")
    public ApiResponse<Map<String, Object>> getShopsByMerchant(
            @PathVariable("merchantId") Long merchantId,
            @RequestHeader("X-User-Id") Long userId) {
        if (!merchantId.equals(userId)) {
            throw new ShopException("无权限查看该商户的店铺列表");
        }
        List<Long> shopIds = shopService.getShopIdsByMerchantId(merchantId);
        return ApiResponse.success(Map.of("shopIds", shopIds));
    }

    @GetMapping("/{shopId}")
    public ApiResponse<Map<String, Object>> getShop(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(Map.of("shop", shopService.getShopWithAccessCheck(shopId, userId)));
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

    @PutMapping("/{shopId}/open")
    public ApiResponse<Map<String, Object>> openShop(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        shopService.openShop(shopId, userId);
        return ApiResponse.success("重新开店成功", null);
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
