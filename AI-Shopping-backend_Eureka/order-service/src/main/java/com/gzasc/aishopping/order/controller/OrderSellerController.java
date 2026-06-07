package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.dto.OrderAbstractSellerDTO;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.ShipOrderRequest;
import com.gzasc.aishopping.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/order")
@RequiredArgsConstructor
public class OrderSellerController {

    private final OrderService orderService;

    @GetMapping("/shop/{shopId}/list")
    public ApiResponse<List<OrderAbstractSellerDTO>> listShopOrders(
            @PathVariable("shopId") String shopId) {
        List<OrderAbstractSellerDTO> orders = orderService.getOrdersByShopId(shopId);
        return ApiResponse.success(orders);
    }

    @GetMapping("/shop/{shopId}/{orderId}")
    public ApiResponse<OrderDetailDTO> getShopOrderDetail(
            @PathVariable("shopId") String shopId,
            @PathVariable("orderId") String orderId) {
        OrderDetailDTO detail = orderService.getOrderDetailByShop(shopId, orderId);
        return ApiResponse.success(detail);
    }

    @PutMapping("/{orderId}/ship")
    public ApiResponse<Void> shipOrder(
            @PathVariable("orderId") String orderId,
            @RequestBody @Valid ShipOrderRequest request,
            @RequestParam("shopId") String shopId) {
        orderService.shipOrder(shopId, orderId, request);
        return ApiResponse.success("发货成功", null);
    }

    @PutMapping("/{orderId}/approve-return")
    public ApiResponse<Void> approveReturn(
            @PathVariable("orderId") String orderId,
            @RequestParam("shopId") String shopId) {
        orderService.approveReturn(shopId, orderId);
        return ApiResponse.success("退货审核通过", null);
    }

    @PutMapping("/{orderId}/confirm-return")
    public ApiResponse<Void> confirmReturn(
            @PathVariable("orderId") String orderId,
            @RequestParam("shopId") String shopId) {
        orderService.confirmReturn(shopId, orderId);
        return ApiResponse.success("退货已确认", null);
    }
}
