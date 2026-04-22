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

/**
 * 订单商家端控制器
 * 提供商家订单管理、发货、状态更新功能
 */
@RestController
@RequestMapping("/api/seller/order")
@RequiredArgsConstructor
public class OrderSellerController {

    private final OrderService orderService;
    private final ProductFeignClient productFeignClient;
    private final LogisticsFeignClient logisticsFeignClient;

    @GetMapping("/list")
    public Map<String, Object> getAllOrders() {
        try {
            List<Order> orders = orderService.getAllOrders();
            return Map.of("message", "查询成功", "orders", orders, "total", orders.size());
        } catch (Exception e) {
            return Map.of("message", "查询订单错误：" + e.getMessage());
        }
    }

    @GetMapping("/status/{status}")
    public Map<String, Object> getOrdersByStatus(@PathVariable("status") String status) {
        try {
            List<Order> orders = orderService.getOrdersByStatus(status);
            return Map.of("message", "查询成功", "orders", orders, "total", orders.size());
        } catch (Exception e) {
            return Map.of("message", "查询订单错误：" + e.getMessage());
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
            return Map.of("message", "发货成功（已发货）", "logisticsId", String.valueOf(order.getLogisticsId()));
        }

        if (!Order.PAID.equals(order.getOrderStatus())) {
            return Map.of("message", "发货失败：订单状态为【" + order.getOrderStatus() + "】，只有已支付订单才能发货");
        }

        if (order.getLogisticsId() != null) {
            return Map.of("message", "发货成功（已发货）", "logisticsId", String.valueOf(order.getLogisticsId()));
        }

        Integer logisticsId = null;
        try {
            LogisticsRequest logisticsRequest = new LogisticsRequest(
                    request.getContactId(),
                    request.getTrackingNumber(),
                    request.getShippingDate()
            );
            Map<String, Object> logisticsResult = logisticsFeignClient.createLogistics(logisticsRequest);

            Object data = logisticsResult.get("data");
            if (data == null) {
                String logisticsMessage = (String) logisticsResult.get("message");
                return Map.of("message", "发货失败：" + (logisticsMessage != null ? logisticsMessage : "创建物流返回数据为空"));
            }

            if (data instanceof java.util.Map) {
                java.util.Map<?, ?> logisticsData = (java.util.Map<?, ?>) data;
                Object id = logisticsData.get("id");
                if (id instanceof Number) {
                    logisticsId = ((Number) id).intValue();
                }
            }
            if (logisticsId == null) {
                return Map.of("message", "发货失败：无法获取物流ID");
            }

            Order updateOrder = new Order();
            updateOrder.setOrderId(orderId);
            updateOrder.setLogisticsId(logisticsId);
            updateOrder.setOrderStatus(Order.SHIPPED);
            int result = orderService.updateOrder(updateOrder);

            if (result > 0) {
                return Map.of("message", "发货成功", "logisticsId", String.valueOf(logisticsId));
            } else {
                compensateCloseLogistics(logisticsId);
                return Map.of("message", "发货失败：更新订单状态失败，已回滚物流记录");
            }
        } catch (Exception e) {
            if (logisticsId != null) {
                compensateCloseLogistics(logisticsId);
            }
            return Map.of("message", "发货错误：" + e.getMessage());
        }
    }

    private void compensateCloseLogistics(Integer logisticsId) {
        try {
            logisticsFeignClient.closeLogistics(logisticsId);
        } catch (Exception ex) {
            System.err.println("[发货补偿失败] 物流ID=" + logisticsId + "，需要人工处理，错误：" + ex.getMessage());
        }
    }
}
