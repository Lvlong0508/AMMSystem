package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.shop.mapper.ProductShopMapper;
import com.gzasc.aishopping.shop.model.ProductShop;
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
    private final ProductShopMapper productShopMapper;
    private final ProductFeignClient productFeignClient;

    @GetMapping("/list")
    public Map<String, Object> getShopList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
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
    public Map<String, Object> getShopDetail(@PathVariable("shopId") String shopId) {
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
            if (start >= productShops.size()) {
                return Map.of("success", true, "products", List.of(), "total", productShops.size(), "page", page, "size", size);
            }
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
            ProductDTO product = productFeignClient.getProductById(productId);
            return Map.of("success", true, "product", product);
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询商品失败");
        }
    }
}