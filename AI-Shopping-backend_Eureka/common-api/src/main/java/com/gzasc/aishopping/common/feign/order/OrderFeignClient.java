package com.gzasc.aishopping.common.feign.order;

import com.gzasc.aishopping.common.dto.order.OrderAbstractSellerDTO;
import com.gzasc.aishopping.common.dto.order.ShipOrderRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "order-service")
public interface OrderFeignClient {

    @GetMapping("/internal/order/shop/{shopId}")
    List<OrderAbstractSellerDTO> getOrdersByShopId(@PathVariable("shopId") String shopId);

    @GetMapping("/internal/order/{orderId}/shop/{shopId}")
    Map<String, Object> getOrderDetailByShop(@PathVariable("shopId") String shopId,
                                              @PathVariable("orderId") String orderId);

    @PutMapping("/api/seller/order/{orderId}/ship")
    Map<String, Object> shipOrder(@PathVariable("orderId") String orderId,
                                  @RequestParam("shopId") String shopId,
                                  @RequestBody ShipOrderRequest request);
}
