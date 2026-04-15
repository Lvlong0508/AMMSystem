package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.logistics.dto.LogisticsRequest;
import com.gzasc.aishopping.order.dto.PlaceOrderRequest;
import com.gzasc.aishopping.order.dto.ProductDTO;
import com.gzasc.aishopping.order.feign.LogisticsFeignClient;
import com.gzasc.aishopping.order.feign.ProductFeignClient;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ProductFeignClient productFeignClient;
    private final LogisticsFeignClient logisticsFeignClient;

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

    @PostMapping("/place")
    public Map<String, String> placeOrder(@RequestBody PlaceOrderRequest request) {
        if (request == null || request.getProductId() == null) {
            return Map.of("message", "创建订单错误：商品信息为空（错误代码：O-001）");
        }
        if (request.getContact() == null || request.getContact().getId() == null) {
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
            order.setContactId(request.getContact().getId());
            orderService.createOrder(order);
            return Map.of("message", "创建订单成功", "orderId", orderId);
        } catch (Exception e) {
            return Map.of("message", "创建订单错误：" + e.getMessage());
        }
    }

    @DeleteMapping("/{orderId}")
    public Map<String, String> deleteOrder(@PathVariable("orderId") String orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order != null) {
                if (Order.PENDING.equals(order.getOrderStatus()) || Order.PAID.equals(order.getOrderStatus())) {
                    productFeignClient.restoreStock(new ProductFeignClient.StockDeductRequest(order.getProductId(), order.getQuantity()));
                }
            }

            int result = orderService.deleteOrder(orderId);
            if (result > 0) {
                return Map.of("message", "删除订单成功");
            } else {
                return Map.of("message", "删除订单失败：订单不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "删除订单错误：" + e.getMessage());
        }
    }

    @PutMapping("/{orderId}")
    public Map<String, String> updateOrder(@PathVariable("orderId") String orderId, @RequestBody Order order) {
        try {
            order.setOrderId(orderId);
            int result = orderService.updateOrder(order);
            if (result > 0) {
                return Map.of("message", "更新订单成功");
            } else {
                return Map.of("message", "更新订单失败：订单不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "更新订单错误：" + e.getMessage());
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
                Map<String, Object> result = productFeignClient.deductStock(new ProductFeignClient.StockDeductRequest(order.getProductId(), order.getQuantity()));
                Boolean success = (Boolean) result.get("success");
                if (!Boolean.TRUE.equals(success)) {
                    return Map.of("message", "更新订单状态失败：商品库存不足");
                }
            }

            if (Order.CANCELLED.equals(status) && (Order.PENDING.equals(order.getOrderStatus()) || Order.PAID.equals(order.getOrderStatus()))) {
                productFeignClient.restoreStock(new ProductFeignClient.StockDeductRequest(order.getProductId(), order.getQuantity()));
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

    // 发货：创建物流信息并更新订单
    @PutMapping("/{orderId}/ship")
    public Map<String, String> shipOrder(
            @PathVariable("orderId") String orderId,
            @RequestParam("trackingNumber") String trackingNumber,
            @RequestParam("contactId") Integer contactId,
            @RequestParam(value = "shippingDate", required = false) String shippingDate) {
        try {
            // 1. 创建物流记录
            LogisticsRequest request = new LogisticsRequest(contactId, trackingNumber, shippingDate);

            Map<String, Object> logisticsResult = logisticsFeignClient.createLogistics(request);
            Object data = logisticsResult.get("data");
            if (data == null) {
                return Map.of("message", "发货失败：创建物流返回数据为空");
            }
            Integer logisticsId = null;
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

            // 2. 更新订单的 logistics_id 和状态
            Order order = new Order();
            order.setOrderId(orderId);
            order.setLogisticsId(logisticsId);
            order.setOrderStatus(Order.SHIPPED);
            int result = orderService.updateOrder(order);

            if (result > 0) {
                return Map.of("message", "发货成功", "logisticsId", String.valueOf(logisticsId));
            } else {
                return Map.of("message", "发货失败：订单不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "发货错误：" + e.getMessage());
        }
    }
}
