package com.gzasc.aishopping.common.feign.order;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 订单服务 Feign 客户端
 * 供其他服务调用订单相关接口
 */
@FeignClient(name = "order-service")
public interface OrderFeignClient {

    /**
     * 根据ID查询订单
     */
    @GetMapping("/api/order/{orderId}")
    Map<String, Object> getOrderById(@PathVariable("orderId") String orderId);

    /**
     * 获取所有订单列表
     */
    @GetMapping("/api/order/list")
    Map<String, Object> getAllOrders();

    /**
     * 根据状态查询订单
     */
    @GetMapping("/api/order/status/{status}")
    Map<String, Object> getOrdersByStatus(@PathVariable("status") String status);
}
