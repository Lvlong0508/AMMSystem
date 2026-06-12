package com.gzasc.aishopping.order.converter;

import com.gzasc.aishopping.common.dto.contact.ContactDTO;
import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.order.dto.*;
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
        dto.setTotalPrice(order.getTotalPrice());
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
        dto.setTotalPrice(order.getTotalPrice());
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

    // ==================== 用户端订单卡片 ====================

    public UserOrderCardDTO toUserCardDTO(Order order, ProductDTO product, ShopInfoDTO shop) {
        UserOrderCardDTO dto = new UserOrderCardDTO();
        dto.setOrderId(order.getOrderId());
        if (shop != null) {
            dto.setShopLogoUrl(shop.getLogoUrl());
            dto.setShopName(shop.getName());
        }
        if (product != null) {
            dto.setProductImageUrl(product.getImageUrl());
            dto.setProductName(product.getName());
            dto.setProductType(product.getTags());
        }
        dto.setQuantity(order.getQuantity());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setTotalPrice(order.getTotalPrice());
        return dto;
    }

    public List<UserOrderCardDTO> toUserCardDTOList(List<Order> orders, Map<String, ProductDTO> productMap, Map<Long, ShopInfoDTO> shopMap) {
        return orders.stream()
            .map(o -> {
                ProductDTO product = o.getProductId() != null ? productMap.get(o.getProductId()) : null;
                ShopInfoDTO shop = null;
                if (o.getShopId() != null) {
                    try {
                        shop = shopMap.get(Long.valueOf(o.getShopId()));
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
                return toUserCardDTO(o, product, shop);
            })
            .collect(Collectors.toList());
    }

    // ==================== 商家端订单卡片 ====================

    public SellerOrderCardDTO toSellerCardDTO(Order order, ProductDTO product, ContactDTO contact) {
        SellerOrderCardDTO dto = new SellerOrderCardDTO();
        dto.setOrderId(order.getOrderId());
        if (product != null) {
            dto.setProductImageUrl(product.getImageUrl());
            dto.setProductName(product.getName());
        }
        dto.setQuantity(order.getQuantity());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setOrderDate(order.getOrderDate());
        if (contact != null) {
            dto.setContactName(contact.getName());
            dto.setContactPhone(contact.getPhone());
            dto.setContactAddress(contact.getAddress());
        }
        return dto;
    }

    public List<SellerOrderCardDTO> toSellerCardDTOList(List<Order> orders, Map<String, ProductDTO> productMap, Map<Integer, ContactDTO> contactMap) {
        return orders.stream()
            .map(o -> {
                ProductDTO product = o.getProductId() != null ? productMap.get(o.getProductId()) : null;
                ContactDTO contact = o.getContactId() != null ? contactMap.get(o.getContactId()) : null;
                return toSellerCardDTO(o, product, contact);
            })
            .collect(Collectors.toList());
    }

    // ==================== 发货页订单卡片 ====================

    public ShipmentOrderCardDTO toShipmentCardDTO(Order order, ProductDTO product, ContactDTO contact) {
        ShipmentOrderCardDTO dto = new ShipmentOrderCardDTO();
        dto.setOrderId(order.getOrderId());
        if (product != null) {
            dto.setProductImageUrl(product.getImageUrl());
            dto.setProductName(product.getName());
            dto.setProductType(product.getTags());
        }
        dto.setQuantity(order.getQuantity());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setOrderDate(order.getOrderDate());
        if (contact != null) {
            dto.setContactName(contact.getName());
            dto.setContactPhone(contact.getPhone());
            dto.setContactAddress(contact.getAddress());
        }
        return dto;
    }

    public List<ShipmentOrderCardDTO> toShipmentCardDTOList(List<Order> orders, Map<String, ProductDTO> productMap, Map<Integer, ContactDTO> contactMap) {
        return orders.stream()
            .map(o -> {
                ProductDTO product = o.getProductId() != null ? productMap.get(o.getProductId()) : null;
                ContactDTO contact = o.getContactId() != null ? contactMap.get(o.getContactId()) : null;
                return toShipmentCardDTO(o, product, contact);
            })
            .collect(Collectors.toList());
    }

    // ==================== 订单详情扩展（含商品/店铺信息） ====================

    public OrderDetailDTO enrichDetailDTO(OrderDetailDTO dto, ProductDTO product, ShopInfoDTO shop, ContactDTO contactInfo, Map<String, Object> logisticsInfo) {
        if (product != null) {
            dto.setProductImageUrl(product.getImageUrl());
            dto.setProductName(product.getName());
            dto.setProductType(product.getTags());
        }
        if (shop != null) {
            dto.setShopLogoUrl(shop.getLogoUrl());
            dto.setShopName(shop.getName());
        }
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
