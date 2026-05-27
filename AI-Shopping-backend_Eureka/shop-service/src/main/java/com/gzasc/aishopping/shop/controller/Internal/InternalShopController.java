package com.gzasc.aishopping.shop.controller.Internal;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/internal/shop")
@RequiredArgsConstructor
public class InternalShopController {

    private final MerchantRoleService merchantRoleService;
    private final ShopService shopService;

    @GetMapping("/employees/roles/{merchantId}")
    public ApiResponse<Map<String, Object>> getMerchantRoles(@PathVariable("merchantId") Long merchantId) {
        List<MerchantRole> roles = merchantRoleService.selectByMerchantId(merchantId);
        return ApiResponse.success(Map.of("roles", roles));
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