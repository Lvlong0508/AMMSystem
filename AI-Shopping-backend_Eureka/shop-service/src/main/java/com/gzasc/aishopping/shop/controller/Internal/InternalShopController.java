package com.gzasc.aishopping.shop.controller.Internal;

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
    public Map<String, Object> getMerchantRoles(@PathVariable("merchantId") String merchantId) {
        try {
            List<MerchantRole> roles = merchantRoleService.selectByMerchantId(merchantId);
            return Map.of(
                "success", true,
                "roles", roles
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "获取角色信息失败：" + e.getMessage()
            );
        }
    }
}