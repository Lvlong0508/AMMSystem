package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.ProductShop;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
import com.gzasc.aishopping.shop.service.ProductShopService;
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
    private final MerchantRoleService merchantRoleService;
    private final ProductShopService productShopService;
    private final ProductFeignClient productFeignClient;

    private boolean hasShopAccess(String userId, String shopId) {
        return merchantRoleService.selectByMerchantAndShop(userId, shopId) != null;
    }

    @GetMapping("/shop/{shopId}")
    public ApiResponse<Map<String, Object>> getShop(
            @PathVariable("shopId") String shopId,
            @RequestHeader("X-User-Id") String userId) {
        if (!hasShopAccess(userId, shopId)) {
            throw new ShopException("无权限访问该店铺");
        }
        Shop shop = shopService.getShopById(shopId);
        if (shop == null) {
            throw new ShopException("店铺不存在");
        }
        return ApiResponse.success(Map.of("shop", shop));
    }

    @GetMapping("/{shopId}/products")
    public ApiResponse<Map<String, Object>> getProducts(
            @PathVariable("shopId") String shopId,
            @RequestHeader("X-User-Id") String userId) {
        if (!hasShopAccess(userId, shopId)) {
            throw new ShopException("无权限访问该店铺");
        }
        List<ProductShop> productShops = productShopService.selectByShopId(shopId);
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
            }
        }
        return ApiResponse.success(Map.of("products", products, "total", products.size()));
    }

    @GetMapping("/{shopId}/employees")
    public ApiResponse<Map<String, Object>> getEmployees(
            @PathVariable("shopId") String shopId,
            @RequestHeader("X-User-Id") String userId) {
        if (!hasShopAccess(userId, shopId)) {
            throw new ShopException("无权限访问该店铺");
        }
        List<MerchantRole> employees = merchantRoleService.selectByShopId(shopId);
        return ApiResponse.success(Map.of("employees", employees, "total", employees.size()));
    }
}