package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.feign.shop.ShopFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/shop")
@RequiredArgsConstructor
public class UserShopController {

    private final ShopFeignClient shopFeignClient;

    @GetMapping("/{shopId}/return-address")
    public ApiResponse<Map<String, String>> getReturnAddress(
            @PathVariable("shopId") Long shopId) {
        ApiResponse<ShopInfoDTO> resp = shopFeignClient.getShopInfo(shopId);
        if (resp == null || resp.getData() == null) {
            return ApiResponse.success(Map.of("address", "", "phone", ""));
        }
        ShopInfoDTO shop = resp.getData();
        return ApiResponse.success(Map.of(
                "address", shop.getAddress() != null ? shop.getAddress() : "",
                "phone", shop.getPhone() != null ? shop.getPhone() : ""
        ));
    }
}
