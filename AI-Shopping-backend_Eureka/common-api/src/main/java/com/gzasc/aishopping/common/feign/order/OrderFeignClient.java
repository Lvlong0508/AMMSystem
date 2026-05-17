package com.gzasc.aishopping.common.feign.order;

import com.gzasc.aishopping.common.dto.order.OrderDTO;
import com.gzasc.aishopping.common.dto.order.ShipOrderRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 订单服务 Feign 客户端
 * 供其他服务调用订单相关接口
 */
@FeignClient(name = "order-service")
public interface OrderFeignClient {

    /**
     * 根据ID查询订单（商家端接口）
     */
    @GetMapping("/api/seller/order/{orderId}")
    Map<String, Object> getOrderById(@PathVariable("orderId") String orderId);

    /**
     * 获取所有订单列表（商家端接口）
     */
    @GetMapping("/api/seller/order/list")
    Map<String, Object> getAllOrders();

    /**
     * 根据状态查询订单（商家端接口）
     */
    @GetMapping("/api/seller/order/status/{status}")
    Map<String, Object> getOrdersByStatus(@PathVariable("status") String status);

    /**
     * 根据商品ID获取店铺ID（内部服务调用）
     */
    @GetMapping("/internal/order/shop-id-by-product/{productId}")
    String getShopIdByProductId(@PathVariable("productId") String productId);

    /**
     * 批量查询订单（内部服务调用）
     */
    @GetMapping("/internal/order/batch")
    List<OrderDTO> getOrdersByIds(@RequestParam("orderIds") List<String> orderIds);

    /**
     * 发货（商家端接口）
     */
    @PutMapping("/api/seller/order/{orderId}/ship")
    Map<String, Object> shipOrder(@PathVariable("orderId") String orderId, @RequestBody ShipOrderRequest request);
}
