package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.common.feign.shop.ShopFeignClient;
import com.gzasc.aishopping.order.dto.PlaceOrderRequest;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单用户端控制器
 * 提供用户下单、查询、取消订单功能
 */
@RestController
@RequestMapping("/api/user/order")
@RequiredArgsConstructor
public class OrderUserController {

    private final OrderService orderService;
    private final ProductFeignClient productFeignClient;
    private final ShopFeignClient shopFeignClient;

    @GetMapping("/{orderId}")
    public Map<String, Object> getOrderById(@PathVariable("orderId") String orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order != null) {
                return Map.of("message", "查询成功", "order", order);
            } else {
                return Map.of("message", "查询失败：订单不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "查询订单错误：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Map<String, Object> getUserOrders() {
        try {
            List<Order> orders = orderService.getAllOrders();
            return Map.of("message", "查询成功", "orders", orders, "total", orders.size());
        } catch (Exception e) {
            return Map.of("message", "查询订单错误：" + e.getMessage());
        }
    }

    @PostMapping("/place")
    public Map<String, String> placeOrder(@RequestBody PlaceOrderRequest request) {
        if (request == null || request.getProductId() == null) {
            return Map.of("message", "创建订单错误：商品信息为空（错误代码：O-001）");
        }
        if (request.getContactId() == null) {
            return Map.of("message", "创建订单错误：收货人信息为空（错误代码：O-002）");
        }
        try {
            if (request.getQuantity() <= 0) {
                return Map.of("message", "创建订单错误：购买数量必须大于0（错误代码：O-004）");
            }

            ProductDTO product = productFeignClient.getProductById(request.getProductId());
            if (product == null) {
                return Map.of("message", "创建订单错误：商品不存在（错误代码：O-003）");
            }

            if (product.getStock() < request.getQuantity()) {
                return Map.of("message", "创建订单错误：商品库存不足，当前库存：" + product.getStock() + "（错误代码：O-005）");
            }

            Order order = new Order();
            String orderId = orderService.generateOrderId();
            order = order.buildInitOrder(orderId, product.getId(), request.getQuantity(), product.getPrice() * request.getQuantity());
            order.setContactId(request.getContactId());
            orderService.createOrder(order);

            try {
                Map<String, Object> shopResult = shopFeignClient.getShopIdByProductId(product.getId());
                if (shopResult != null && Boolean.TRUE.equals(shopResult.get("success"))) {
                    String shopId = String.valueOf(shopResult.get("shopId"));
                    shopFeignClient.associateOrder(orderId, shopId);
                }
            } catch (Exception e) {
                System.err.println("关联订单到店铺失败: " + e.getMessage());
            }

            return Map.of("message", "创建订单成功", "orderId", orderId);
        } catch (Exception e) {
            return Map.of("message", "创建订单错误：" + e.getMessage());
        }
    }

    @DeleteMapping("/{orderId}")
    public Map<String, String> cancelOrder(@PathVariable("orderId") String orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order != null) {
                if (Order.PENDING.equals(order.getOrderStatus()) || Order.PAID.equals(order.getOrderStatus())) {
                    productFeignClient.restoreStock(new com.gzasc.aishopping.common.dto.product.StockDeductRequest(order.getProductId(), order.getQuantity()));
                }
            }

            int result = orderService.deleteOrder(orderId);
            if (result > 0) {
                return Map.of("message", "取消订单成功");
            } else {
                return Map.of("message", "取消订单失败：订单不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "取消订单错误：" + e.getMessage());
        }
    }
}
