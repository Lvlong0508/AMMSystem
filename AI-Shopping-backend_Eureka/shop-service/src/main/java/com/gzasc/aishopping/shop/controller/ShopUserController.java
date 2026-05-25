package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/shop")
@RequiredArgsConstructor
public class ShopUserController {

    private final ShopService shopService;

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getShopList(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        if (userId == null) {
            throw new ShopException("请先登录");
        }
        List<Shop> shops = shopService.getActiveShops(page, size);
        int total = shopService.countActiveShops();
        Map<String, Object> data = new HashMap<>();
        data.put("shops", shops);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        return ApiResponse.success(data);
    }

    @GetMapping("/{shopId}")
    public ApiResponse<Map<String, Object>> getShopDetail(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        if (userId == null) {
            throw new ShopException("请先登录");
        }
        Shop shop = shopService.getShopById(shopId);
        if (shop != null && shop.getStatus() == 1) {
            return ApiResponse.success(Map.of("shop", shop));
        }
        throw new ShopException("店铺不存在或已关闭");
    }

    @GetMapping("/{shopId}/products")
    public ApiResponse<Map<String, Object>> getShopProducts(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        if (userId == null) {
            throw new ShopException("请先登录");
        }
        Shop shop = shopService.getShopById(shopId);
        if (shop == null || shop.getStatus() != 1) {
            throw new ShopException("店铺不存在");
        }
        Map<String, Object> result = shopService.getShopProductsWithPagination(shopId, page, size);
        return ApiResponse.success(result);
    }

    @GetMapping("/{shopId}/products/{productId}")
    public ApiResponse<Map<String, Object>> getProductDetail(
            @PathVariable("shopId") Long shopId,
            @PathVariable("productId") Long productId,
            @RequestHeader("X-User-Id") Long userId) {
        if (userId == null) {
            throw new ShopException("请先登录");
        }
        Shop shop = shopService.getShopById(shopId);
        if (shop == null || shop.getStatus() != 1) {
            throw new ShopException("店铺不存在");
        }
        ProductDTO product = shopService.getProductDetailByShop(shopId, productId);
        return ApiResponse.success(Map.of("product", product));
    }
}
