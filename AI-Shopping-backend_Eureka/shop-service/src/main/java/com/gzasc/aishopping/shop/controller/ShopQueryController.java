package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/shop/query")
@RequiredArgsConstructor
public class ShopQueryController {

    private final ShopService shopService;

    @GetMapping("/shop/{shopId}")
    public ApiResponse<Map<String, Object>> getShop(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(Map.of("shop", shopService.getShopWithAccessCheck(shopId, userId)));
    }

    @GetMapping("/{shopId}/products")
    public ApiResponse<Map<String, Object>> getProducts(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        List<Map<String, Object>> products = shopService.getShopProductsWithDetails(shopId, userId);
        return ApiResponse.success(Map.of("products", products, "total", products.size()));
    }

    @GetMapping("/{shopId}/employees")
    public ApiResponse<Map<String, Object>> getEmployees(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        List<Map<String, Object>> employees = shopService.getShopEmployees(shopId, userId);
        return ApiResponse.success(Map.of("employees", employees, "total", employees.size()));
    }
}