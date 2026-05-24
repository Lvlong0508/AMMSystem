package com.gzasc.aishopping.order.controller.internal;

import com.gzasc.aishopping.common.dto.order.OrderAbstractSellerDTO;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
