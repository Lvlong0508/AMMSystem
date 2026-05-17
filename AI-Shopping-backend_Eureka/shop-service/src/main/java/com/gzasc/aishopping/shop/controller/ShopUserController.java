package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.shop.model.ProductShop;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.ProductShopService;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/shop")
@RequiredArgsConstructor
public class ShopUserController {

    private final ShopService shopService;
    private final ProductShopService productShopService;
    private final ProductFeignClient productFeignClient;

    @GetMapping("/list")
    public Map<String, Object> getShopList(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        if (userId == null || userId.trim().isEmpty()) {
            return Map.of("success", false, "message", "请先登录");
        }
        try {
            List<Shop> shops = shopService.getActiveShops(page, size);
            int total = shopService.countActiveShops();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("shops", shops);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            return result;
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @GetMapping("/{shopId}")
    public Map<String, Object> getShopDetail(
            @PathVariable("shopId") String shopId,
            @RequestHeader("X-User-Id") String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Map.of("success", false, "message", "请先登录");
        }
        try {
            Shop shop = shopService.getShopById(shopId);
            if (shop != null && shop.getStatus() == 1) {
                return Map.of("success", true, "shop", shop);
            } else {
                return Map.of("success", false, "message", "店铺不存在或已关闭");
            }
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @GetMapping("/{shopId}/products")
    public Map<String, Object> getShopProducts(
            @PathVariable("shopId") String shopId,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        if (userId == null || userId.trim().isEmpty()) {
            return Map.of("success", false, "message", "请先登录");
        }
        try {
            Shop shop = shopService.getShopById(shopId);
            if (shop == null || shop.getStatus() != 1) {
                return Map.of("success", false, "message", "店铺不存在");
            }
            List<ProductShop> productShops = productShopService.selectByShopId(shopId);
            int total = productShops.size();
            int start = (page - 1) * size;
            int end = Math.min(start + size, productShops.size());
            if (start >= total) {
                return Map.of("success", true, "products", List.of(), "total", total, "page", page, "size", size);
            }
            List<ProductShop> paged = productShops.subList(start, end);
            
            List<Map<String, Object>> productDetails = new ArrayList<>();
            for (ProductShop ps : paged) {
                try {
                    Map<String, Object> productMap = productFeignClient.getProductById(ps.getProductId());
                    if (productMap != null && productMap.containsKey("id")) {
                        Map<String, Object> detail = new HashMap<>();
                        detail.put("id", productMap.get("id"));
                        detail.put("name", productMap.get("name"));
                        detail.put("description", productMap.get("description"));
                        detail.put("price", productMap.get("price"));
                        detail.put("stock", productMap.get("stock"));
                        detail.put("tags", productMap.get("tags"));
                        productDetails.add(detail);
                    }
                } catch (Exception e) {
                }
            }
            
            return Map.of("success", true, "products", productDetails, "total", total, "page", page, "size", size);
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询商品失败: " + e.getMessage());
        }
    }

    @GetMapping("/{shopId}/products/{productId}")
    public Map<String, Object> getProductDetail(
            @PathVariable("shopId") String shopId,
            @PathVariable("productId") String productId,
            @RequestHeader("X-User-Id") String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Map.of("success", false, "message", "请先登录");
        }
        try {
            Shop shop = shopService.getShopById(shopId);
            if (shop == null || shop.getStatus() != 1) {
                return Map.of("success", false, "message", "店铺不存在");
            }
            String shopIdFromDb = productShopService.selectShopIdByProductId(productId);
            if (shopIdFromDb == null || !shopIdFromDb.equals(shopId)) {
                return Map.of("success", false, "message", "商品不存在");
            }
            Map<String, Object> productMap = productFeignClient.getProductById(productId);
            if (productMap == null) {
                return Map.of("success", false, "message", "商品不存在");
            }
            // 从 Map 转换为 ProductDTO
            ProductDTO product = new ProductDTO();
            product.setId((String) productMap.get("id"));
            product.setName((String) productMap.get("name"));
            product.setDescription((String) productMap.get("description"));
            product.setPrice(productMap.get("price") != null ? ((Number) productMap.get("price")).doubleValue() : 0.0);
            product.setStock(productMap.get("stock") != null ? ((Number) productMap.get("stock")).intValue() : 0);
            return Map.of("success", true, "product", product);
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询商品失败");
        }
    }
}