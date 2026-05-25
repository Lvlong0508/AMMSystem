package com.gzasc.aishopping.shop.controller.Internal;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/shop")
@RequiredArgsConstructor
public class InternalShopController {

    private final MerchantRoleService merchantRoleService;

    @GetMapping("/employees/roles/{merchantId}")
    public ApiResponse<Map<String, Object>> getMerchantRoles(@PathVariable("merchantId") String merchantId) {
        List<MerchantRole> roles = merchantRoleService.selectByMerchantId(Long.valueOf(merchantId));
        return ApiResponse.success(Map.of("roles", roles));
    }
}