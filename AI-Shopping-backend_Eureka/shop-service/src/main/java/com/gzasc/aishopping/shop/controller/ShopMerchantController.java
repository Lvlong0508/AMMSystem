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
        log.info("閺屻儴顕楅幋鎴犳畱鎼存鎽? userId={}", userId);
        Map<String, Object> data = new HashMap<>();
        data.put("shop", shop);
        return ApiResponse.success(data);
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> register(
            @RequestBody @Valid CreateShopRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("鍟嗗娉ㄥ唽搴楅摵, userId={}", userId);
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
        log.info("鍟嗗娉ㄥ唽搴楅摵, userId={}", userId);
        Shop shop = shopService.createShop(request, userId, logo);
        return ApiResponse.success(Map.of("id", shop.getId()));
    }

    private void validateLogo(MultipartFile logo) {
        String contentType = logo.getContentType();
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            throw new ShopException(400, "浠呮敮鎸?JPG 鍜?PNG 鏍煎紡");
        }
    }

    @GetMapping("/{shopId}")
    public ApiResponse<Map<String, Object>> getShop(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("鑾峰彇鍟嗗簵, shopId={}, userId={}", shopId, userId);
        Shop shop = shopService.getShopWithAccessCheck(shopId, userId);
        ShopInfoDTO shopInfo = shopService.getShopInfoById(shopId);
        return ApiResponse.success(Map.of(
                "shop", shop,
                "shopInfo", shopInfo
        ));
    }

    @PutMapping("/{shopId}")
    public ApiResponse<Map<String, Object>> updateShop(
            @PathVariable("shopId") Long shopId,
            @RequestBody @Valid UpdateShopRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("鏇存柊鍟嗗簵, shopId={}", shopId);
        shopService.updateShop(shopId, request, userId);
        return ApiResponse.success("??????", null);
    }

    @PatchMapping("/{shopId}/close")
    public ApiResponse<Map<String, Object>> closeShop(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("鍏抽棴鍟嗗簵, shopId={}, userId={}", shopId, userId);
        shopService.closeShop(shopId, userId);
        return ApiResponse.success("??????", null);
    }

    @PatchMapping("/{shopId}/open")
    public ApiResponse<Map<String, Object>> openShop(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("鎵撳紑鍟嗗簵 shopId={}, userId={}", shopId, userId);
        shopService.openShop(shopId, userId);
        return ApiResponse.success("??????", null);
    }
}
