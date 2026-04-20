package com.gzasc.aishopping.chat.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.gzasc.aishopping.common.dto.contact.ContactDTO;
import com.gzasc.aishopping.common.dto.order.OrderDTO;
import com.gzasc.aishopping.common.feign.contact.ContactFeignClient;
import com.gzasc.aishopping.common.feign.order.OrderFeignClient;
import com.gzasc.aishopping.chat.utils.AiToolFormatter;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderTools {

    private final OrderFeignClient orderFeignClient;
    private final ContactFeignClient contactFeignClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool("""
            主要作用:根据订单ID查询订单详情，包含订单ID、商品ID、数量、总价、订单状态、日期、收货人信息。
            特殊情况:如果能获取到订单信息，就按格式返回给用户；否则提示用户'订单不存在'。
            """)
    public String getOrderById(@P("订单ID，必须由用户提供方可使用该方法") String orderId) {
        OrderDTO order = getOrderFromFeign(orderId);
        if (order == null) {
            return "订单不存在";
        }
        ContactDTO contact = order.getContactId() != null ? getContactFromFeign(order.getContactId()) : null;
        return AiToolFormatter.formatOrderInfo(order, contact);
    }

    @Tool("""
            主要作用:查询所有订单列表，包含订单ID、商品ID、数量、总价、订单状态、日期、收货人名称。
            特殊情况:如果能获取到订单列表，就按格式返回给用户；否则提示用户'暂无订单'。
            """)
    public String getAllOrders() {
        List<OrderDTO> orders = getAllOrdersFromFeign();
        List<ContactDTO> contacts = getContactsForOrders(orders);
        return AiToolFormatter.formatOrders(orders, contacts, "暂无订单");
    }

    @Tool("""
            主要作用:根据客户名称查询订单列表。
            特殊情况:如果能获取到订单列表，就按格式返回给用户；否则提示用户'该客户暂无订单'。
            """)
    public String getOrdersByCustomerName(@P("客户名称，必须由用户提供方可使用该方法") String customerName) {
        List<OrderDTO> orders = getAllOrdersFromFeign().stream()
                .filter(o -> customerName.equals(o.getContactName()))
                .collect(Collectors.toList());
        List<ContactDTO> contacts = getContactsForOrders(orders);
        return AiToolFormatter.formatOrders(orders, contacts, "该客户暂无订单");
    }

    @Tool("""
            主要作用:根据订单状态查询订单列表。
            特殊情况:如果能获取到订单列表，就按格式返回给用户；否则提示用户'该状态下暂无订单'。
            """)
    public String getOrdersByStatus(@P("订单状态，必须由用户提供方可使用该方法，可选值：待发货、已发货、已送达、已取消、已退货") String status) {
        List<OrderDTO> orders = getOrdersByStatusFromFeign(status);
        List<ContactDTO> contacts = getContactsForOrders(orders);
        return AiToolFormatter.formatOrders(orders, contacts, "该状态下暂无订单");
    }

    private List<ContactDTO> getContactsForOrders(List<OrderDTO> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        List<Integer> contactIds = orders.stream()
                .map(OrderDTO::getContactId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        return contactIds.stream()
                .map(this::getContactFromFeign)
                .filter(c -> c != null)
                .collect(Collectors.toList());
    }

    private ContactDTO getContactFromFeign(Integer contactId) {
        try {
            Map<String, Object> response = contactFeignClient.getContactById(contactId);
            if (response != null && "查询成功".equals(response.get("message"))) {
                Object data = response.get("data");
                return objectMapper.convertValue(data, ContactDTO.class);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private OrderDTO getOrderFromFeign(String orderId) {
        try {
            Map<String, Object> response = orderFeignClient.getOrderById(orderId);
            if (response != null && "查询成功".equals(response.get("message"))) {
                Object order = response.get("order");
                return objectMapper.convertValue(order, OrderDTO.class);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private List<OrderDTO> getAllOrdersFromFeign() {
        try {
            Map<String, Object> response = orderFeignClient.getAllOrders();
            if (response != null && "查询成功".equals(response.get("message"))) {
                Object orders = response.get("orders");
                return objectMapper.convertValue(orders, new TypeReference<List<OrderDTO>>() {});
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<OrderDTO> getOrdersByStatusFromFeign(String status) {
        try {
            Map<String, Object> response = orderFeignClient.getOrdersByStatus(status);
            if (response != null && "查询成功".equals(response.get("message"))) {
                Object orders = response.get("orders");
                return objectMapper.convertValue(orders, new TypeReference<List<OrderDTO>>() {});
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
}
