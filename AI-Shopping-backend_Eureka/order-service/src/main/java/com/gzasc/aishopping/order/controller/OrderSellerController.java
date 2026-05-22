package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.dto.product.StockDeductRequest;
import com.gzasc.aishopping.common.feign.logistics.LogisticsFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.order.dto.ShipOrderRequest;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/order")
@RequiredArgsConstructor
public class OrderSellerController {

    private final OrderService orderService;
    private final ProductFeignClient productFeignClient;
    private final LogisticsFeignClient logisticsFeignClient;

    @GetMapping("/{orderId}")
    public Map<String, Object> getOrderById(@PathVariable("orderId") String orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return Map.of("success", false, "message", "订单不存在");
            }
            Map<String, Object> orderMap = new java.util.HashMap<>();
            orderMap.put("orderId", order.getOrderId());
            orderMap.put("productId", order.getProductId());
            orderMap.put("quantity", order.getQuantity());
            orderMap.put("totalPrice", order.getTotalPrice());
            orderMap.put("orderStatus", order.getOrderStatus());
            orderMap.put("orderDate", order.getOrderDate());
            orderMap.put("contactId", order.getContactId());
            try {
                Map<String, Object> logisticsResult = logisticsFeignClient.getLatestLogistics(orderId, "DELIVERY");
                if (logisticsResult != null && logisticsResult.containsKey("data")) {
                    Object data = logisticsResult.get("data");
                    if (data instanceof Map) {
                        Map<String, Object> logistics = (Map<String, Object>) data;
                        orderMap.put("trackingNumber", logistics.get("trackingNumber"));
                        orderMap.put("logistics", logistics);
                    }
                }
            } catch (Exception e) {
                System.err.println("获取物流信息失败: " + e.getMessage());
            }
            return Map.of("success", true, "order", orderMap);
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询订单错误：" + e.getMessage());
        }
    }

    @PutMapping("/{orderId}/status")
    public Map<String, String> updateOrderStatus(@PathVariable("orderId") String orderId, @RequestParam("status") String status) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return Map.of("message", "更新订单状态失败：订单不存在");
            }

            if (Order.PENDING.equals(order.getOrderStatus()) && Order.PAID.equals(status)) {
                Map<String, Object> result = productFeignClient.deductStock(new StockDeductRequest(order.getProductId(), order.getQuantity()));
                Boolean success = (Boolean) result.get("success");
                if (!Boolean.TRUE.equals(success)) {
                    return Map.of("message", "更新订单状态失败：商品库存不足");
                }
            }

            if (Order.CANCELLED.equals(status) && (Order.PENDING.equals(order.getOrderStatus()) || Order.PAID.equals(order.getOrderStatus()))) {
                productFeignClient.restoreStock(new StockDeductRequest(order.getProductId(), order.getQuantity()));
            }

            int result = orderService.updateOrderStatus(orderId, status);
            if (result > 0) {
                return Map.of("message", "更新订单状态成功");
            } else {
                return Map.of("message", "更新订单状态失败：订单不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "更新订单状态错误：" + e.getMessage());
        }
    }

    @PutMapping("/{orderId}/ship")
    public Map<String, String> shipOrder(
            @PathVariable("orderId") String orderId,
            @RequestBody ShipOrderRequest request) {

        if (orderId == null || orderId.trim().isEmpty()) {
            return Map.of("message", "发货失败：订单ID不能为空");
        }
        if (request == null) {
            return Map.of("message", "发货失败：请求体不能为空");
        }
        if (request.getTrackingNumber() == null || request.getTrackingNumber().trim().isEmpty()) {
            return Map.of("message", "发货失败：物流单号不能为空");
        }
        if (request.getContactId() == null) {
            return Map.of("message", "发货失败：联系人ID不能为空");
        }

        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return Map.of("message", "发货失败：订单不存在");
        }

        if (Order.SHIPPED.equals(order.getOrderStatus())) {
            return Map.of("message", "发货成功（已发货）");
        }

        if (!Order.PAID.equals(order.getOrderStatus())) {
            return Map.of("message", "发货失败：订单状态为【" + order.getOrderStatus() + "】，只有已支付订单才能发货");
        }

        try {
            LogisticsRequest logisticsRequest = new LogisticsRequest();
            logisticsRequest.setOrderId(orderId);
            logisticsRequest.setType("DELIVERY");
            logisticsRequest.setContactId(request.getContactId());
            logisticsRequest.setTrackingNumber(request.getTrackingNumber());

            Map<String, Object> logisticsResult = logisticsFeignClient.createLogistics(logisticsRequest);
            Object data = logisticsResult.get("data");
            if (data == null) {
                String logisticsMessage = (String) logisticsResult.get("message");
                return Map.of("message", "发货失败：" + (logisticsMessage != null ? logisticsMessage : "创建物流返回数据为空"));
            }

            int result = orderService.updateOrderStatus(orderId, Order.SHIPPED);

            if (result > 0) {
                return Map.of("message", "发货成功");
            } else {
                return Map.of("message", "发货失败：更新订单状态失败");
            }
        } catch (Exception e) {
            return Map.of("message", "发货错误：" + e.getMessage());
        }
    }
}
