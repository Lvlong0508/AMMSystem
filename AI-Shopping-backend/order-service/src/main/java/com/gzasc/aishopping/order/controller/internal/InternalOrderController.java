package com.gzasc.aishopping.order.controller.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.UserOrderCardDTO;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/order")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{orderId}")
    public ApiResponse<Map<String, Object>> getOrderById(@PathVariable("orderId") String orderId,
                                                         @RequestHeader("X-User-Id") Long userId) {
        OrderDetailDTO dto = orderService.getOrderDetailByUser(userId, orderId);
        return ApiResponse.success(objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {}));
    }

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> getAllOrders(@RequestHeader("X-User-Id") Long userId) {
        List<UserOrderCardDTO> orders = orderService.getOrdersByUserId(userId);
        return ApiResponse.success(objectMapper.convertValue(orders, new TypeReference<List<Map<String, Object>>>() {}));
    }
}
