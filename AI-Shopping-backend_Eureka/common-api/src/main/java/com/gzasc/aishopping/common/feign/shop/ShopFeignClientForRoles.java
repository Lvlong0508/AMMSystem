package com.gzasc.aishopping.common.feign.shop;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "shop-service", contextId = "shopFeignClientForRoles")
public interface ShopFeignClientForRoles {

    @GetMapping("/internal/shop/employees/roles/{merchantId}")
    Map<String, Object> getMerchantRoles(@PathVariable("merchantId") String merchantId);
}