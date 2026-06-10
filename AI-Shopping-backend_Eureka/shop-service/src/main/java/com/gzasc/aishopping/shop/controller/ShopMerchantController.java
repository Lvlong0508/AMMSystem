package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.dto.SimpleShopDTO;
import com.gzasc.aishopping.shop.dto.UpdateShopRequest;
import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.ShopService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/seller/shop")
@RequiredArgsConstructor
public class ShopMerchantController {

    private final ShopService shopService;

    @GetMapping("/my-shop")
    public ApiResponse<Map<String, Object>> getMyShop(
            @RequestHeader("X-User-Id") Long userId) {
        SimpleShopDTO shop = shopService.getMyShop(userId);
        log.info("查询我的店铺, userId={}", userId);
        Map<String, Object> data = new HashMap<>();
        data.put("shop", shop);
        return ApiResponse.success(data);
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> register(
            @RequestBody @Valid CreateShopRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("商家注册店铺, userId={}", userId);
        Shop shop = shopService.createShop(request, userId);
        return ApiResponse.success(Map.of("id", shop.getId()));
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> registerWithLogo(
            @RequestPart("shop") @Valid CreateShopRequest request,
            @RequestPart(value = "logo", required = false) MultipartFile logo,
            @RequestHeader("X-User-Id") Long userId) {
        if (logo != null && !logo.isEmpty()) {
            validateLogo(logo);
        }
        log.info("商家注册店铺, userId={}", userId);
        Shop shop = shopService.createShop(request, userId, logo);
        return ApiResponse.success(Map.of("id", shop.getId()));
    }

    private void validateLogo(MultipartFile logo) {
        String contentType = logo.getContentType();
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            throw new ShopException(400, "仅支持 JPG 和 PNG 格式");
        }
    }

    @GetMapping("/{shopId}")
    public ApiResponse<Map<String, Object>> getShop(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("获取店铺, shopId={}, userId={}", shopId, userId);
        Shop shop = shopService.getShopWithAccessCheck(shopId, userId);
        ShopInfoDTO shopInfo = shopService.getShopInfoById(shopId);
        return ApiResponse.success(Map.of(
                "shop", shop,
                "shopInfo", shopInfo
        ));
    }

    @PutMapping(value = "/{shopId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> updateShop(
            @PathVariable("shopId") Long shopId,
            @RequestPart("shop") @Valid UpdateShopRequest request,
            @RequestPart(value = "logo", required = false) MultipartFile logo,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("更新店铺, shopId={}", shopId);
        if (logo != null && !logo.isEmpty()) {
            validateLogo(logo);
        }
        shopService.updateShop(shopId, request, userId, logo);
        return ApiResponse.success("更新店铺成功", null);
    }

    @PatchMapping("/{shopId}/close")
    public ApiResponse<Map<String, Object>> closeShop(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("关闭店铺, shopId={}, userId={}", shopId, userId);
        shopService.closeShop(shopId, userId);
        return ApiResponse.success("关闭店铺成功", null);
    }

    @PatchMapping("/{shopId}/open")
    public ApiResponse<Map<String, Object>> openShop(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("打开店铺 shopId={}, userId={}", shopId, userId);
        shopService.openShop(shopId, userId);
        return ApiResponse.success("开启店铺成功", null);
    }
}
