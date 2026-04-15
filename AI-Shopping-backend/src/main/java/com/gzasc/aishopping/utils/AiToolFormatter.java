package com.gzasc.aishopping.chat.utils;

import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.order.model.Order;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单信息格式化工具类
 */
public class AiToolFormatter {

    /**
     * 格式化订单详细信息（完整信息）
     */
    public static String formatOrderInfo(Order order, Contact contact) {
        String name = contact != null ? contact.getName() : "";
        String phone = contact != null ? contact.getPhone() : "";
        String address = contact != null ? contact.getAddress() : "";
        return String.format("订单ID: %s, 商品ID: %s, 数量: %d, 总价: %.2f, 状态: %s, 日期: %s, 收货人: %s, 电话: %s, 地址: %s",
                order.getOrderId(), order.getProductId(), order.getQuantity(), order.getTotalPrice(),
                order.getOrderStatus(), order.getOrderDate(), name, phone, address);
    }

    /**
     * 格式化订单概要信息（列表展示用）
     */
    public static String formatOrderSummary(Order order, Contact contact) {
        String name = contact != null ? contact.getName() : "";
        return String.format("订单ID: %s, 商品ID: %s, 数量: %d, 总价: %.2f, 状态: %s, 日期: %s, 收货人: %s",
                order.getOrderId(), order.getProductId(), order.getQuantity(), order.getTotalPrice(),
                order.getOrderStatus(), order.getOrderDate(), name);
    }

    /**
     * 格式化订单列表
     */
    public static String formatOrders(List<Order> orders, List<Contact> contacts, String emptyMsg) {
        if (orders == null || orders.isEmpty()) {
            return emptyMsg;
        }
        return orders.stream()
                .map(order -> {
                    Contact contact = findContactById(contacts, order.getContactId());
                    return formatOrderSummary(order, contact);
                })
                .collect(Collectors.joining("\n"));
    }

    private static Contact findContactById(List<Contact> contacts, Integer contactId) {
        if (contacts == null || contactId == null) return null;
        return contacts.stream()
                .filter(c -> c.getId() == contactId)
                .findFirst()
                .orElse(null);
    }
}
