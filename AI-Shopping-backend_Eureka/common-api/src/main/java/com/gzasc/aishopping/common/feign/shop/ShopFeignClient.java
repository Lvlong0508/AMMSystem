package com.gzasc.aishopping.common.feign.shop;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 店铺服务 Feign 客户端
 * 供其他服务调用店铺相关接口
 */
@FeignClient(name = "shop-service")
public interface ShopFeignClient {

    /**
     * 根据商品ID获取店铺ID
     */
    @GetMapping("/internal/shop/shop-id-by-product/{productId}")
    Map<String, Object> getShopIdByProductId(@PathVariable("productId") String productId);

    /**
     * 检查是否为店铺所有者
     */
    @GetMapping("/internal/shop/check-owner/{shopId}/{merchantId}")
    Boolean checkOwner(@PathVariable("shopId") String shopId, @PathVariable("merchantId") String merchantId);

    /**
     * 检查商家访问权限
     */
    @GetMapping("/internal/shop/check-access/{shopId}/{merchantId}")
    Boolean checkAccess(@PathVariable("shopId") String shopId, @PathVariable("merchantId") String merchantId);
}
