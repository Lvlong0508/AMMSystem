package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.dto.OrderAbstractUserDTO;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.PlaceOrderRequest;
import com.gzasc.aishopping.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/order")
@RequiredArgsConstructor
public class OrderUserController {

    private final OrderService orderService;

    @GetMapping("/list")
    public ApiResponse<List<OrderAbstractUserDTO>> listOrders(
            @RequestHeader("X-User-Id") Long userId) {
        List<OrderAbstractUserDTO> orders = orderService.getOrdersByUserId(userId);
        return ApiResponse.success(orders);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailDTO> getOrderDetail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        OrderDetailDTO detail = orderService.getOrderDetailByUser(userId, orderId);
        if (detail == null) {
            return ApiResponse.error(404, "订单不存在");
        }
        return ApiResponse.success(detail);
    }

    @PostMapping("/place")
    public ApiResponse<String> placeOrder(
            @RequestBody @Valid PlaceOrderRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        String orderId = orderService.createOrder(request, userId);
        return ApiResponse.success("创建订单成功", orderId);
    }

    @PutMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancelOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        orderService.cancelOrder(userId, orderId);
        return ApiResponse.success("取消订单成功", null);
    }

    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> deleteOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        orderService.deleteOrder(userId, orderId);
        return ApiResponse.success("删除订单成功", null);
    }

    @PutMapping("/{orderId}/pay")
    public ApiResponse<Void> payOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        orderService.payOrder(userId, orderId);
        return ApiResponse.success("支付成功", null);
    }

    @PutMapping("/{orderId}/deliver")
    public ApiResponse<Void> deliverOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        orderService.deliverOrder(userId, orderId);
        return ApiResponse.success("确认收货成功", null);
    }

    @PostMapping("/{orderId}/return-request")
    public ApiResponse<Void> requestReturn(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        orderService.requestReturn(userId, orderId);
        return ApiResponse.success("退货申请已提交", null);
    }
}
