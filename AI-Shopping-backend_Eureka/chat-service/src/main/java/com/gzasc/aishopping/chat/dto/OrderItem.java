package com.gzasc.aishopping.chat.dto;

public record OrderItem(
    String orderId,
    String productId,
    Integer quantity,
    Double totalPrice,
    String orderStatus,
    String orderDate,
    String contactName,
    String contactPhone,
    String contactAddress
) {}
