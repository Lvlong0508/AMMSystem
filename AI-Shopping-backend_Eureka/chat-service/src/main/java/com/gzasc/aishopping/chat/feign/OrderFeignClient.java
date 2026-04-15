package com.gzasc.aishopping.chat.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "order-service")
public interface OrderFeignClient {

    @GetMapping("/api/order/{orderId}")
    Map<String, Object> getOrderById(@PathVariable("orderId") String orderId);

    @GetMapping("/api/order/list")
    Map<String, Object> getAllOrders();

    @GetMapping("/api/order/status/{status}")
    Map<String, Object> getOrdersByStatus(@PathVariable("status") String status);
}
