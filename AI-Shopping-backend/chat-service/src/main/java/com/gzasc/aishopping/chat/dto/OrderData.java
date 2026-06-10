package com.gzasc.aishopping.chat.dto;

import java.util.List;

public record OrderData(List<OrderItem> orders) implements Data {}
