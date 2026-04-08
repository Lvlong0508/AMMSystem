package com.gzasc.aishopping.controller.seller;

import com.gzasc.aishopping.mapper.LogisticsMapper;
import com.gzasc.aishopping.model.Order;
import com.gzasc.aishopping.model.Logistics;
import com.gzasc.aishopping.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderSellerController {

    private final OrderService orderService;
    private final LogisticsMapper logisticsMapper;

    // 发货：创建物流信息并更新订单
    @PutMapping("/{orderId}/ship")
    public Map<String, String> shipOrder(
            @PathVariable("orderId") String orderId,
            @RequestParam("trackingNumber") String trackingNumber,
            @RequestParam("contactId") Integer contactId,
            @RequestParam(value = "shippingDate", required = false) String shippingDate) {
        try {
            // 1. 创建物流记录
            Logistics logistics = new Logistics();
            logistics.setContactId(contactId);
            logistics.setTrackingNumber(trackingNumber);
            // 解析发货日期
            if (shippingDate != null && !shippingDate.isEmpty()) {
                logistics.setShippingDate(java.sql.Timestamp.valueOf(shippingDate.replace("T", " ") + ":00"));
            } else {
                logistics.setShippingDate(new java.sql.Timestamp(System.currentTimeMillis()));
            }
            logisticsMapper.insertLogistics(logistics);

            // 2. 更新订单的logistics_id
            Order order = new Order();
            order.setOrderId(orderId);
            order.setLogisticsId(logistics.getId());
            // 注意：不更新contactId，因为订单的contactId是收货人，而发货时选的contactId是发货人
            int result = orderService.updateOrder(order);

            if (result > 0) {
                return Map.of("message", "发货成功", "logisticsId", String.valueOf(logistics.getId()));
            } else {
                return Map.of("message", "发货失败：订单不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "发货错误：" + e.getMessage());
        }
    }
}
