package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.SellerOrderCardDTO;
import com.gzasc.aishopping.order.dto.ShipmentOrderCardDTO;
import com.gzasc.aishopping.order.dto.ReturnRequestDTO;
import com.gzasc.aishopping.order.dto.ReviewReturnRequest;
import com.gzasc.aishopping.order.dto.ShipOrderRequest;
import com.gzasc.aishopping.order.model.ReturnRequest;
import com.gzasc.aishopping.order.service.OrderService;
import com.gzasc.aishopping.order.service.ReturnRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/seller/order")
@RequiredArgsConstructor
public class OrderSellerController {

    private final OrderService orderService;
    private final ReturnRequestService returnRequestService;

    @GetMapping("/shop/{shopId}/list")
    public ApiResponse<List<SellerOrderCardDTO>> listShopOrders(
            @PathVariable("shopId") String shopId) {
        List<SellerOrderCardDTO> orders = orderService.getOrdersByShopId(shopId);
        return ApiResponse.success(orders);
    }

    @GetMapping("/shop/{shopId}/shipment-list")
    public ApiResponse<List<ShipmentOrderCardDTO>> listShipmentOrders(
            @PathVariable("shopId") String shopId) {
        List<ShipmentOrderCardDTO> orders = orderService.getShipmentOrdersByShopId(shopId);
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

    @PutMapping("/{orderId}/confirm-return")
    public ApiResponse<Void> confirmReturn(
            @PathVariable("orderId") String orderId,
            @RequestParam("shopId") String shopId) {
        orderService.confirmReturn(shopId, orderId);
        return ApiResponse.success("退货已确认", null);
    }

    @GetMapping("/return-requests/pending")
    public ApiResponse<List<ReturnRequestDTO>> listPendingReturns(
            @RequestParam("shopId") String shopId) {
        List<ReturnRequestDTO> list = returnRequestService.listByShop(shopId, ReturnRequest.APPLYING);
        return ApiResponse.success(list);
    }

    @GetMapping("/return-requests/processed")
    public ApiResponse<List<ReturnRequestDTO>> listProcessedReturns(
            @RequestParam("shopId") String shopId) {
        List<ReturnRequestDTO> processed = new ArrayList<>(returnRequestService.listByShop(shopId, ReturnRequest.AGREED));
        processed.addAll(returnRequestService.listByShop(shopId, ReturnRequest.REJECTED));
        return ApiResponse.success(processed);
    }

    @PutMapping("/return-requests/{orderId}/review")
    public ApiResponse<Void> reviewReturnRequest(
            @PathVariable("orderId") String orderId,
            @RequestParam("shopId") String shopId,
            @RequestBody @Valid ReviewReturnRequest request) {
        returnRequestService.reviewReturnRequest(shopId, orderId, request);
        return ApiResponse.success("审核完成", null);
    }
}
