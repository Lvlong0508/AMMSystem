package com.gzasc.aishopping.common.feign.shop;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.Set;

@FeignClient(name = "shop-service")
public interface ShopFeignClient {

    @GetMapping("/internal/shop/info/{shopId}")
    ApiResponse<ShopInfoDTO> getShopInfo(@PathVariable("shopId") Long shopId);

    @PostMapping("/internal/shop/info/batch")
    ApiResponse<Map<Long, ShopInfoDTO>> batchGetShopInfo(@RequestBody Set<Long> shopIds);

    @GetMapping("/internal/shop/employees/roles/{merchantId}")
    ApiResponse<Map<String, Object>> getMerchantRoles(@PathVariable("merchantId") Long merchantId);
}
