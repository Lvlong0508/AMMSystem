package com.gzasc.aishopping.chat.tools;

import com.gzasc.aishopping.chat.context.UserContext;
import com.gzasc.aishopping.chat.exception.AiToolException;
import com.gzasc.aishopping.common.feign.order.OrderFeignClient;
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
    public Map<String, Object> getOrderById(@P("订单ID") String orderId) {
        Long userId = UserContext.getUserId();
        Object response = orderFeignClient.getOrderById(orderId, userId);
        if (response == null) {
            throw new AiToolException("订单不存在");
        }
        Map<String, Object> result = new HashMap<>();
        if (response instanceof Map) {
            result.putAll((Map<String, Object>) response);
        }
        return result;
    }

    @Tool("查询当前用户的所有订单列表，包含订单ID、商品ID、总价、数量、状态")
    public List<Map<String, Object>> getAllOrders() {
        Long userId = UserContext.getUserId();
        Object response = orderFeignClient.getAllOrders(userId);
        if (response instanceof List) {
            return (List<Map<String, Object>>) response;
        }
        return Collections.emptyList();
    }

    @Tool("根据订单状态查询订单列表，可选值：待发货、已发货、已送达、已取消、已退货")
    public List<Map<String, Object>> getOrdersByStatus(@P("订单状态，可选值：待发货、已发货、已送达、已取消、已退货") String status) {
        return getAllOrders().stream()
                .filter(order -> status.equals(order.get("orderStatus")))
                .collect(Collectors.toList());
    }
}
