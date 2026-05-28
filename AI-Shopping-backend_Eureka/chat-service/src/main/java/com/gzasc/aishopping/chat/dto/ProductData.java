package com.gzasc.aishopping.chat.dto;

import java.util.List;

public record ProductData(List<ProductItem> products) implements Data {}
