package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.dto.OrderAbstractSellerDTO;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.ShipOrderRequest;
import com.gzasc.aishopping.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public ApiResponse<Map<String, Object>> getShopOrderDetail(
            @PathVariable("shopId") String shopId,
            @PathVariable("orderId") String orderId) {
        OrderDetailDTO detail = orderService.getOrderDetailByShop(shopId, orderId);
        return ApiResponse.success(toOrderVO(detail));
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

    private Map<String, Object> toOrderVO(OrderDetailDTO dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", dto.getOrderId());
        map.put("userId", String.valueOf(dto.getUserId()));
        map.put("shopId", dto.getShopId());
        map.put("productId", dto.getProductId());
        map.put("quantity", dto.getQuantity());
        map.put("totalPrice", dto.getTotalPrice());
        map.put("orderStatus", dto.getOrderStatus());
        map.put("orderDate", dto.getOrderDate());
        map.put("contactId", dto.getContactId());
        map.put("contactName", dto.getContactName());
        map.put("contactPhone", dto.getContactPhone());
        map.put("contactAddress", dto.getContactAddress());
        map.put("trackingNumber", dto.getTrackingNumber());
        return map;
    }
}
