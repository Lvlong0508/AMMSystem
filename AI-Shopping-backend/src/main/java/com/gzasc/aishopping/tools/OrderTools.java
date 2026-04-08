package com.gzasc.aishopping.tools;

import com.gzasc.aishopping.mapper.ContactMapper;
import com.gzasc.aishopping.model.Contact;
import com.gzasc.aishopping.model.Order;
import com.gzasc.aishopping.service.OrderService;
import com.gzasc.aishopping.utils.AiToolFormatter;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderTools {

    private final OrderService orderService;
    private final ContactMapper contactMapper;

    @Tool("""
            主要作用:根据订单ID查询订单详情，包含订单ID、商品ID、数量、总价、订单状态、日期、收货人信息。
            特殊情况:如果能获取到订单信息，就按格式返回给用户；否则提示用户'订单不存在'。
            """)
    public String getOrderById(@P("订单ID，必须由用户提供方可使用该方法") String orderId) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return "订单不存在";
        }
        Contact contact = order.getContactId() != null ? contactMapper.selectContactById(order.getContactId()) : null;
        return AiToolFormatter.formatOrderInfo(order, contact);
    }

    @Tool("""
            主要作用:查询所有订单列表，包含订单ID、商品ID、数量、总价、订单状态、日期、收货人名称。
            特殊情况:如果能获取到订单列表，就按格式返回给用户；否则提示用户'暂无订单'。
            """)
    public String getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        List<Contact> contacts = getContactsForOrders(orders);
        return AiToolFormatter.formatOrders(orders, contacts, "暂无订单");
    }

    @Tool("""
            主要作用:根据客户名称查询订单列表。
            特殊情况:如果能获取到订单列表，就按格式返回给用户；否则提示用户'该客户暂无订单'。
            """)
    public String getOrdersByCustomerName(@P("客户名称，必须由用户提供方可使用该方法") String customerName) {
        List<Order> orders = orderService.getOrdersByCustomerName(customerName);
        List<Contact> contacts = getContactsForOrders(orders);
        return AiToolFormatter.formatOrders(orders, contacts, "该客户暂无订单");
    }

    @Tool("""
            主要作用:根据订单状态查询订单列表。
            特殊情况:如果能获取到订单列表，就按格式返回给用户；否则提示用户'该状态下暂无订单'。
            """)
    public String getOrdersByStatus(@P("订单状态，必须由用户提供方可使用该方法，可选值：待发货、已发货、已送达、已取消、已退货") String status) {
        List<Order> orders = orderService.getOrdersByStatus(status);
        List<Contact> contacts = getContactsForOrders(orders);
        return AiToolFormatter.formatOrders(orders, contacts, "该状态下暂无订单");
    }

    private List<Contact> getContactsForOrders(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        List<Integer> contactIds = orders.stream()
                .map(Order::getContactId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        return contactIds.stream()
                .map(contactMapper::selectContactById)
                .filter(c -> c != null)
                .collect(Collectors.toList());
    }
}
