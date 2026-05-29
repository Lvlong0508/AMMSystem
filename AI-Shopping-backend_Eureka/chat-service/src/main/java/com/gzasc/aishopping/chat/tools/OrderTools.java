package com.gzasc.aishopping.chat.tools;

import com.gzasc.aishopping.chat.exception.AiToolException;
import dev.langchain4j.service.MemoryId;
import com.gzasc.aishopping.common.feign.order.OrderFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderTools {

    private final OrderFeignClient orderFeignClient;

    @Tool("获取指定订单的详细信息，包含订单ID、商品、总价、状态、收货人信息")
    public Map<String, Object> getOrderById(@P("订单ID") String orderId, @MemoryId Long userId) {
        ApiResponse<Map<String, Object>> response = orderFeignClient.getOrderById(orderId, userId);
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            throw new AiToolException("订单不存在");
        }
        return new HashMap<>(response.getData());
    }

    @Tool("查询当前用户的所有订单列表，包含订单ID、商品ID、总价、数量、状态")
    public List<Map<String, Object>> getAllOrders(@MemoryId Long userId) {
        ApiResponse<List<Map<String, Object>>> response = orderFeignClient.getAllOrders(userId);
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            return Collections.emptyList();
        }
        return response.getData();
    }

    @Tool("根据订单状态查询订单列表")
    public List<Map<String, Object>> getOrdersByStatus(@P("订单状态，可选值：PENDING PAID SHIPPED DELIVERED CANCELLED RETURNED") String status, @MemoryId Long userId) {
        return getAllOrders(userId).stream()
                .filter(order -> java.util.Objects.equals(status, order.get("orderStatus")))
                .collect(Collectors.toList());
    }
}
