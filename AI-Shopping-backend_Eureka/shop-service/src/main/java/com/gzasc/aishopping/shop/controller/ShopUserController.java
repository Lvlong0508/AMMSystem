package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.service.ShopService;
import com.gzasc.aishopping.shop.service.impl.ShopConverter;
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
    private final ShopConverter shopConverter;

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getShopList(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        if (userId == null) {
            throw new ShopException("请先登录");
        }
        Map<String, Object> result = new HashMap<>(shopService.getUserShopList(page, size));
        List<Shop> shops = (List<Shop>) result.get("shops");
        result.put("shops", shops.stream().map(shopConverter::toShopVO).toList());
        return ApiResponse.success(result);
    }

    @GetMapping("/{shopId}")
    public ApiResponse<Map<String, Object>> getShopDetail(
            @PathVariable("shopId") Long shopId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            throw new ShopException("请先登录");
        }
        Map<String, Object> result = new HashMap<>(shopService.getActiveShopById(shopId));
        Shop shop = (Shop) result.get("shop");
        ShopInfoDTO shopInfo = (ShopInfoDTO) result.get("shopInfo");
        result.put("shop", shopConverter.toShopVO(shop));
        result.put("shopInfo", shopConverter.toShopInfoVO(shopInfo));
        return ApiResponse.success(result);
    }

}
