package com.gzasc.aishopping.common.feign.order;

import com.gzasc.aishopping.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "order-service")
public interface OrderFeignClient {

    @GetMapping("/internal/order/{orderId}")
    ApiResponse<Map<String, Object>> getOrderById(@PathVariable("orderId") String orderId,
                                                  @RequestHeader("X-User-Id") Long userId);

    @GetMapping("/internal/order/list")
    ApiResponse<List<Map<String, Object>>> getAllOrders(@RequestHeader("X-User-Id") Long userId);
}
