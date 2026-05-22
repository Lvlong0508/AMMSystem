package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.dto.shop.OrderShopDTO;
import com.gzasc.aishopping.common.feign.logistics.LogisticsFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.common.feign.shop.ShopFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.dto.PlaceOrderRequest;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final LogisticsFeignClient logisticsFeignClient;

    @GetMapping("/{orderId}")
    public Map<String, Object> getOrderById(
            @PathVariable("orderId") String orderId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            return Map.of("message", "查询订单错误：未登录（错误代码：O-006）");
        }
        try {
            Order order = orderService.getOrderByUserId(userId, orderId);
            if (order != null) {
                Map<String, Object> orderMap = convertToMapWithLogistics(order);
                return Map.of("message", "查询成功", "order", orderMap);
            } else {
                return Map.of("message", "查询失败：订单不存在或无权限查看");
            }
        } catch (Exception e) {
            return Map.of("message", "查询订单错误：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Map<String, Object> getUserOrders(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            return Map.of("message", "查询订单错误：未登录（错误代码：O-007）");
        }
        try {
            List<Order> orders = orderService.getOrdersByUserId(userId);
            List<Map<String, Object>> orderList = new ArrayList<>();
            for (Order order : orders) {
                orderList.add(convertToMapWithLogistics(order));
            }
            return Map.of("message", "查询成功", "orders", orderList, "total", orders.size());
        } catch (Exception e) {
            return Map.of("message", "查询订单错误：" + e.getMessage());
        }
    }

    @PostMapping("/place")
    public Map<String, String> placeOrder(
            @RequestBody PlaceOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            return Map.of("message", "创建订单错误：未登录（错误代码：O-008）");
        }
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

Map<String, Object> productMap = productFeignClient.getProductById(request.getProductId());
            if (productMap == null) {
                return Map.of("message", "创建订单错误：商品不存在（错误代码：O-003）");
            }
            // 从 Map 中提取商品数据
            ProductDTO product = new ProductDTO();
            product.setId((String) productMap.get("id"));
            product.setName((String) productMap.get("name"));
            product.setPrice(productMap.get("price") != null ?
                ((Number) productMap.get("price")).doubleValue() : 0.0);
            product.setStock(productMap.get("stock") != null ?
                ((Number) productMap.get("stock")).intValue() : 0);

            if (product.getStock() < request.getQuantity()) {
                return Map.of("message", "创建订单错误：商品库存不足，当前库存：" + product.getStock() + "（错误代码：O-005）");
            }

            Order order = new Order();
            String orderId = orderService.generateOrderId();
            order = order.buildInitOrder(orderId, product.getId(), request.getQuantity(), product.getPrice() * request.getQuantity());
            order.setContactId(request.getContactId());
            orderService.createOrder(order);

            // 创建用户订单关联
            orderService.createUserOrder(userId, orderId);

            try {
                Map<String, Object> shopResult = shopFeignClient.getShopIdByProductId(product.getId());
                if (shopResult != null && Boolean.TRUE.equals(shopResult.get("success"))) {
                    String shopId = String.valueOf(shopResult.get("shopId"));
                    OrderShopDTO orderShopDTO = new OrderShopDTO();
                    orderShopDTO.setOrderId(orderId);
                    orderShopDTO.setShopId(shopId);
                    shopFeignClient.associateOrder(orderShopDTO);
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
    public Map<String, String> cancelOrder(
            @PathVariable("orderId") String orderId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            return Map.of("message", "取消订单错误：未登录（错误代码：O-009）");
        }
        try {
            // 验证用户是否有权限删除该订单
            Order order = orderService.getOrderByUserId(userId, orderId);
            if (order == null) {
                return Map.of("message", "取消订单失败：订单不存在或无权限操作");
            }

            if (Order.PENDING.equals(order.getOrderStatus()) || Order.PAID.equals(order.getOrderStatus())) {
                productFeignClient.restoreStock(new com.gzasc.aishopping.common.dto.product.StockDeductRequest(order.getProductId(), order.getQuantity()));
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

    @PutMapping("/{orderId}/status")
    public Map<String, String> updateOrderStatus(
            @PathVariable("orderId") String orderId,
            @RequestParam("status") String status,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            return Map.of("message", "更新订单状态错误：未登录（错误代码：O-010）");
        }
        try {
            Order order = orderService.getOrderByUserId(userId, orderId);
            if (order == null) {
                return Map.of("message", "更新订单状态失败：订单不存在或无权限操作");
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

    private Integer parseUserId(String userIdStr) {
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, Object> convertToMapWithLogistics(Order order) {
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("orderId", order.getOrderId());
        orderMap.put("productId", order.getProductId());
        orderMap.put("quantity", order.getQuantity());
        orderMap.put("totalPrice", order.getTotalPrice());
        orderMap.put("orderStatus", order.getOrderStatus());
        orderMap.put("orderDate", order.getOrderDate());
        orderMap.put("contactId", order.getContactId());

        try {
            ApiResponse<Map<String, Object>> logisticsResponse = logisticsFeignClient.getLatestLogistics(order.getOrderId(), "DELIVERY");
            if (logisticsResponse != null && logisticsResponse.getData() != null) {
                orderMap.put("trackingNumber", logisticsResponse.getData().get("trackingNumber"));
                orderMap.put("logistics", logisticsResponse.getData());
            }
        } catch (Exception e) {
            System.err.println("获取物流信息失败: " + e.getMessage());
        }

        return orderMap;
    }
}
