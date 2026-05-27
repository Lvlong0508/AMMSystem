package com.gzasc.aishopping.shop.controller.Internal;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.mapper.ShopMapper;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.model.ShopInfo;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
import com.gzasc.aishopping.shop.service.ShopInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/internal/shop")
@RequiredArgsConstructor
public class InternalShopController {

    private final MerchantRoleService merchantRoleService;
    private final ShopInfoService shopInfoService;
    private final ShopMapper shopMapper;

    @GetMapping("/employees/roles/{merchantId}")
    public ApiResponse<Map<String, Object>> getMerchantRoles(@PathVariable("merchantId") Long merchantId) {
        List<MerchantRole> roles = merchantRoleService.selectByMerchantId(merchantId);
        return ApiResponse.success(Map.of("roles", roles));
    }

    @GetMapping("/info/{shopId}")
    public ApiResponse<ShopInfoDTO> getShopInfo(@PathVariable("shopId") Long shopId) {
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null || shop.getShopInfoId() == null) {
            return ApiResponse.success(null);
        }
        ShopInfo shopInfo = shopInfoService.getById(shop.getShopInfoId());
        if (shopInfo == null) {
            return ApiResponse.success(null);
        }
        ShopInfoDTO dto = new ShopInfoDTO(shopInfo.getId(), shopInfo.getName(), shopInfo.getDescription(), shopInfo.getLogoUrl());
        return ApiResponse.success(dto);
    }

    @PostMapping("/info/batch")
    public ApiResponse<Map<Long, ShopInfoDTO>> batchGetShopInfo(@RequestBody Set<Long> shopIds) {
        Map<Long, ShopInfoDTO> result = new HashMap<>();
        for (Long shopId : shopIds) {
            Shop shop = shopMapper.selectShopById(shopId);
            if (shop == null || shop.getShopInfoId() == null) continue;
            ShopInfo shopInfo = shopInfoService.getById(shop.getShopInfoId());
            if (shopInfo == null) continue;
            ShopInfoDTO dto = new ShopInfoDTO(shopInfo.getId(), shopInfo.getName(), shopInfo.getDescription(), shopInfo.getLogoUrl());
            result.put(shopId, dto);
        }
        return ApiResponse.success(result);
    }
}