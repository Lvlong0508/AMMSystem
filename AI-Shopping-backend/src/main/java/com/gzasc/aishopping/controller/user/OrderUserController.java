package com.gzasc.aishopping.controller.user;

import com.gzasc.aishopping.dto.child.PlaceOrderRequest.PlaceOrderWithContactRequest;
import com.gzasc.aishopping.mapper.ProductMapper;
import com.gzasc.aishopping.model.Order;
import com.gzasc.aishopping.model.Product;
import com.gzasc.aishopping.service.OrderService;
import com.gzasc.aishopping.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderUserController {

    private final OrderService orderService;
    private final ProductService productService;
    private final ProductMapper productMapper;

    // 下单
    @PostMapping("/place")
    public Map<String, String> placeOrder(@RequestBody PlaceOrderWithContactRequest request) {
        if (request == null || request.getProductId() == null) {
            return Map.of("message", "创建订单错误：商品信息为空（错误代码：O-001）");
        }
        if (request.getContact() == null || request.getContact().getId() == 0) {
            return Map.of("message", "创建订单错误：收货人信息为空（错误代码：O-002）");
        }
        try {
            // 校验购买数量
            if (request.getQuantity() <= 0) {
                return Map.of("message", "创建订单错误：购买数量必须大于0（错误代码：O-004）");
            }
            
            Product product = productService.getProductById(request.getProductId());
            if (product == null) {
                return Map.of("message", "创建订单错误：商品不存在（错误代码：O-003）");
            }
            
            // 校验库存是否充足
            if (product.getStock() < request.getQuantity()) {
                return Map.of("message", "创建订单错误：商品库存不足，当前库存：" + product.getStock() + "（错误代码：O-005）");
            }
            
            Order order = new Order();
            String orderId = orderService.generateOrderId();
            order = order.buildInitOrder(
                    orderId,
                    product.getId(),
                    request.getQuantity(),
                    product.getPrice() * request.getQuantity()
            );
            // 设置收货人信息ID（使用前端传来的联系人ID）
            order.setContactId(request.getContact().getId());
            orderService.createOrder(order);
            return Map.of("message", "创建订单成功", "orderId", orderId);
        } catch (Exception e) {
            return Map.of("message", "创建订单错误：" + e.getMessage());
        }
    }

    // 删除订单（支持库存恢复）
    @DeleteMapping("/{orderId}")
    public Map<String, String> deleteOrder(@PathVariable("orderId") String orderId) {
        try {
            // 先获取订单信息
            Order order = orderService.getOrderById(orderId);
            if (order != null) {
                // 如果订单已支付或待支付，恢复库存
                if (Order.PENDING.equals(order.getOrderStatus()) || Order.PAID.equals(order.getOrderStatus())) {
                    productMapper.restoreStock(order.getProductId(), order.getQuantity());
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

    // 更新订单信息（仅更新客户信息）
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

    // 更新订单状态（支持库存扣减和恢复）
    @PutMapping("/{orderId}/status")
    public Map<String, String> updateOrderStatus(@PathVariable("orderId") String orderId, @RequestParam("status") String status) {
        try {
            // 先获取当前订单信息
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return Map.of("message", "更新订单状态失败：订单不存在");
            }
            
            // 支付时扣减库存（从PENDING到PAID）
            if (Order.PENDING.equals(order.getOrderStatus()) && Order.PAID.equals(status)) {
                int deductResult = productMapper.deductStock(order.getProductId(), order.getQuantity());
                if (deductResult == 0) {
                    return Map.of("message", "更新订单状态失败：商品库存不足");
                }
            }
            
            // 取消时恢复库存（从PENDING或PAID到CANCELLED）
            if (Order.CANCELLED.equals(status) && 
                (Order.PENDING.equals(order.getOrderStatus()) || Order.PAID.equals(order.getOrderStatus()))) {
                productMapper.restoreStock(order.getProductId(), order.getQuantity());
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
}
