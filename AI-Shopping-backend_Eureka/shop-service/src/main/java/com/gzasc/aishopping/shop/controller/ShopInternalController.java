package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.dto.shop.OrderShopDTO;
import com.gzasc.aishopping.shop.mapper.MerchantRoleMapper;
import com.gzasc.aishopping.shop.mapper.OrderShopMapper;
import com.gzasc.aishopping.shop.mapper.ProductShopMapper;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.OrderShop;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/shop")
@RequiredArgsConstructor
public class ShopInternalController {

    private final ProductShopMapper productShopMapper;
    private final MerchantRoleMapper merchantRoleMapper;
    private final OrderShopMapper orderShopMapper;

    @GetMapping("/shop-id-by-product/{productId}")
    public Map<String, Object> getShopIdByProductId(@PathVariable("productId") String productId) {
        try {
            String shopId = productShopMapper.selectShopIdByProductId(productId);
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
            MerchantRole role = merchantRoleMapper.selectByMerchantShopAndRole(merchantId, shopId, "1");
            return Map.of("success", true, "isOwner", role != null);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @GetMapping("/check-access/{shopId}/{merchantId}")
    public Map<String, Object> checkAccess(@PathVariable("shopId") String shopId,
                                          @PathVariable("merchantId") String merchantId) {
        try {
            MerchantRole role = merchantRoleMapper.selectByMerchantAndShop(merchantId, shopId);
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
            int result = orderShopMapper.insert(orderShop);
            return Map.of("success", result > 0, "message", result > 0 ? "关联成功" : "关联失败");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}