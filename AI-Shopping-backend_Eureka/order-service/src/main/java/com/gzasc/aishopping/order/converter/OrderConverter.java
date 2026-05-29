package com.gzasc.aishopping.order.converter;

import com.gzasc.aishopping.common.dto.contact.ContactDTO;
import com.gzasc.aishopping.order.dto.OrderAbstractSellerDTO;
import com.gzasc.aishopping.order.dto.OrderAbstractUserDTO;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.model.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderConverter {

    public OrderAbstractUserDTO toUserAbstractDTO(Order order) {
        OrderAbstractUserDTO dto = new OrderAbstractUserDTO();
        dto.setOrderId(order.getOrderId());
        dto.setProductId(order.getProductId());
        dto.setShopId(order.getShopId());
        dto.setTotalPrice(BigDecimal.valueOf(order.getTotalPrice()));
        dto.setQuantity(order.getQuantity());
        dto.setOrderStatus(order.getOrderStatus());
        return dto;
    }

    public List<OrderAbstractUserDTO> toUserAbstractDTOList(List<Order> orders) {
        return orders.stream().map(this::toUserAbstractDTO).collect(Collectors.toList());
    }

    public OrderAbstractSellerDTO toSellerAbstractDTO(Order order) {
        OrderAbstractSellerDTO dto = new OrderAbstractSellerDTO();
        dto.setOrderId(order.getOrderId());
        dto.setProductId(order.getProductId());
        dto.setContactId(order.getContactId());
        dto.setQuantity(order.getQuantity());
        dto.setOrderStatus(order.getOrderStatus());
        return dto;
    }

    public List<OrderAbstractSellerDTO> toSellerAbstractDTOList(List<Order> orders) {
        return orders.stream().map(this::toSellerAbstractDTO).collect(Collectors.toList());
    }

    public OrderDetailDTO toDetailDTO(Order order) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setOrderId(order.getOrderId());
        dto.setUserId(order.getUserId());
        dto.setShopId(order.getShopId());
        dto.setProductId(order.getProductId());
        dto.setQuantity(order.getQuantity());
        dto.setTotalPrice(BigDecimal.valueOf(order.getTotalPrice()));
        dto.setOrderStatus(order.getOrderStatus());
        dto.setOrderDate(order.getOrderDate());
        dto.setContactId(order.getContactId());
        return dto;
    }

    public OrderDetailDTO enrichDetailDTO(OrderDetailDTO dto, ContactDTO contactInfo,
                                          Map<String, Object> logisticsInfo) {
        if (contactInfo != null) {
            dto.setContactName(contactInfo.getName());
            dto.setContactPhone(contactInfo.getPhone());
            dto.setContactAddress(contactInfo.getAddress());
        }
        if (logisticsInfo != null && logisticsInfo.get("data") instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) logisticsInfo.get("data");
            dto.setTrackingNumber((String) data.get("trackingNumber"));
        }
        return dto;
    }
}
