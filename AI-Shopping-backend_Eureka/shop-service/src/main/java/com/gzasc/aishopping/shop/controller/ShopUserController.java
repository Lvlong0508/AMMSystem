package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/shop")
@RequiredArgsConstructor
public class ShopUserController {

    private final ShopService shopService;

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getShopList(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        if (userId == null) {
            throw new ShopException("请先登录");
        }
        return ApiResponse.success(shopService.getUserShopList(page, size));
    }

    @GetMapping("/{shopId}")
    public ApiResponse<Map<String, Object>> getShopDetail(
            @PathVariable("shopId") Long shopId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            throw new ShopException("请先登录");
        }
        return ApiResponse.success(shopService.getActiveShopById(shopId));
    }

}
