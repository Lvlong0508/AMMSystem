package com.gzasc.aishopping.common.feign.order;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service")
public interface OrderFeignClient {

    @GetMapping("/internal/order/{orderId}")
    Object getOrderById(@PathVariable("orderId") String orderId,
                        @RequestHeader("X-User-Id") Long userId);

    @GetMapping("/internal/order/list")
    Object getAllOrders(@RequestHeader("X-User-Id") Long userId);
}
