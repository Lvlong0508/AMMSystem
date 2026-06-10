package com.gzasc.aishopping.chat.dto;

import java.math.BigDecimal;

public record OrderItem(
    String orderId,
    String productId,
    Integer quantity,
    BigDecimal totalPrice,
    String orderStatus,
    String orderDate,
    String contactName,
    String contactPhone,
    String contactAddress
) {}
