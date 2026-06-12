package com.gzasc.aishopping.order.controller.internal;

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

@RestController
@RequestMapping("/internal/order")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailDTO> getOrderById(@PathVariable("orderId") String orderId,
                                                    @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(orderService.getOrderDetailByUser(userId, orderId));
    }

    @GetMapping("/list")
    public ApiResponse<List<UserOrderCardDTO>> getAllOrders(@RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(orderService.getOrdersByUserId(userId));
    }
}
