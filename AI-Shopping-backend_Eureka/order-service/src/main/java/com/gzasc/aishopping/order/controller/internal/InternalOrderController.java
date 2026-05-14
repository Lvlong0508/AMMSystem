package com.gzasc.aishopping.order.controller.internal;

import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/internal/order")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    @GetMapping("/batch")
    public List<Order> getOrdersByIds(@RequestParam("orderIds") String orderIds) {
        List<String> idList = Arrays.asList(orderIds.split(","));
        return orderService.getOrdersByIds(idList);
    }

    @GetMapping("/shop-id-by-product/{productId}")
    public String getShopIdByProductId(@PathVariable("productId") String productId) {
        return orderService.getShopIdByProductId(productId);
    }
}