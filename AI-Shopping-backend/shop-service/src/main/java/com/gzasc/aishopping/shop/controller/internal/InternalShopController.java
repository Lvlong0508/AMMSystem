package com.gzasc.aishopping.shop.controller.internal;

import com.gzasc.aishopping.common.dto.shop.CreateShopForMerchantRequest;
import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/internal/shop")
@RequiredArgsConstructor
public class InternalShopController {

    private final ShopService shopService;

    @PostMapping("/create-for-merchant")
    public ApiResponse<Map<String, Object>> createShopForMerchant(
            @RequestHeader(value = "X-Internal-Source", required = false) String source,
            @Valid @RequestBody CreateShopForMerchantRequest request) {
        log.info("内部接口 - 为商家创建店铺, merchantId={}, name={}", request.getMerchantId(), request.getName());
        CreateShopRequest createRequest = new CreateShopRequest();
        createRequest.setName(request.getName());
        createRequest.setDescription(request.getDescription());
        createRequest.setLogoId(request.getLogoUrl());
        try {
            Shop shop = shopService.createShop(createRequest, request.getMerchantId());
            return ApiResponse.success(Map.of("id", shop.getId()));
        } catch (Exception e) {
            log.error("为商家创建店铺失败, merchantId={}", request.getMerchantId(), e);
            return ApiResponse.error(500, "创建店铺失败");
        }
    }

    @GetMapping("/info/{shopId}")
    public ApiResponse<ShopInfoDTO> getShopInfo(@PathVariable("shopId") Long shopId) {
        return ApiResponse.success(shopService.getShopInfoById(shopId));
    }

    @PostMapping("/info/batch")
    public ApiResponse<Map<Long, ShopInfoDTO>> batchGetShopInfo(@RequestBody Set<Long> shopIds) {
        return ApiResponse.success(shopService.batchGetShopInfo(shopIds));
    }
}