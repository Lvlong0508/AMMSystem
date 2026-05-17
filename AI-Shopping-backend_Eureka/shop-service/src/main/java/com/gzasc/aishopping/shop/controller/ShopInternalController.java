package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.dto.shop.OrderShopDTO;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.OrderShop;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
import com.gzasc.aishopping.shop.service.OrderShopService;
import com.gzasc.aishopping.shop.service.ProductShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/shop")
@RequiredArgsConstructor
public class ShopInternalController {

    private final ProductShopService productShopService;
    private final MerchantRoleService merchantRoleService;
    private final OrderShopService orderShopService;

    @GetMapping("/shop-id-by-product/{productId}")
    public Map<String, Object> getShopIdByProductId(@PathVariable("productId") String productId) {
        try {
            String shopId = productShopService.selectShopIdByProductId(productId);
            if (shopId != null) {
                return Map.of("success", true, "shopId", shopId);
            } else {
                return Map.of("success", false, "message", "未找到关联店铺");
            }
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @GetMapping("/check-owner/{shopId}/{merchantId}")
    public Map<String, Object> checkOwner(@PathVariable("shopId") String shopId,
                                          @PathVariable("merchantId") String merchantId) {
        try {
            MerchantRole role = merchantRoleService.selectByMerchantShopAndRole(merchantId, shopId, "1");
            return Map.of("success", true, "isOwner", role != null);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @GetMapping("/check-access/{shopId}/{merchantId}")
    public Map<String, Object> checkAccess(@PathVariable("shopId") String shopId,
                                          @PathVariable("merchantId") String merchantId) {
        try {
            MerchantRole role = merchantRoleService.selectByMerchantAndShop(merchantId, shopId);
            return Map.of("success", true, "hasAccess", role != null);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/associate-order")
    public Map<String, Object> associateOrder(@RequestBody OrderShopDTO request) {
        try {
            String orderId = request.getOrderId();
            String shopId = request.getShopId();
            if (orderId == null || shopId == null) {
                return Map.of("success", false, "message", "缺少必要参数");
            }
            OrderShop orderShop = new OrderShop();
            orderShop.setId(UUID.randomUUID().toString().replace("-", ""));
            orderShop.setOrderId(orderId);
            orderShop.setShopId(shopId);
            int result = orderShopService.insert(orderShop);
            return Map.of("success", result > 0, "message", result > 0 ? "关联成功" : "关联失败");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}