package com.gzasc.aishopping.common.feign.order;

import com.gzasc.aishopping.common.dto.order.ShipOrderRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "order-service")
public interface OrderFeignClient {

    @PutMapping("/api/seller/order/{orderId}/ship")
    Map<String, Object> shipOrder(@PathVariable("orderId") String orderId,
                                  @RequestParam("shopId") String shopId,
                                  @RequestBody ShipOrderRequest request);
}
