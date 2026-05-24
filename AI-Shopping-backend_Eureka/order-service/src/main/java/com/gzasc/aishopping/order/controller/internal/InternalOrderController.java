package com.gzasc.aishopping.order.controller.internal;

import com.gzasc.aishopping.common.dto.order.OrderAbstractSellerDTO;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/order")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    @GetMapping("/shop/{shopId}")
    public List<OrderAbstractSellerDTO> getOrdersByShopId(@PathVariable("shopId") String shopId) {
        return orderService.getOrdersByShopId(shopId).stream()
                .map(dto -> {
                    OrderAbstractSellerDTO result = new OrderAbstractSellerDTO();
                    result.setOrderId(dto.getOrderId());
                    result.setProductId(dto.getProductId());
                    result.setContactId(dto.getContactId());
                    result.setQuantity(dto.getQuantity());
                    result.setOrderStatus(dto.getOrderStatus());
                    return result;
                }).toList();
    }

    @GetMapping("/{orderId}/shop/{shopId}")
    public Map<String, Object> getOrderDetailByShop(@PathVariable("shopId") String shopId,
                                                     @PathVariable("orderId") String orderId) {
        try {
            OrderDetailDTO detail = orderService.getOrderDetailByShop(shopId, orderId);
            Map<String, Object> result = new HashMap<>();
            result.put("orderId", detail.getOrderId());
            result.put("userId", detail.getUserId());
            result.put("shopId", detail.getShopId());
            result.put("productId", detail.getProductId());
            result.put("quantity", detail.getQuantity());
            result.put("totalPrice", detail.getTotalPrice());
            result.put("orderStatus", detail.getOrderStatus());
            result.put("orderDate", detail.getOrderDate());
            result.put("contactId", detail.getContactId());
            result.put("contactName", detail.getContactName());
            result.put("contactPhone", detail.getContactPhone());
            result.put("contactAddress", detail.getContactAddress());
            result.put("trackingNumber", detail.getTrackingNumber());
            return result;
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}
