package com.gzasc.aishopping.chat.dto;

public record ProductItem(
    Long id,
    String name,
    Double price,
    String tags,
    String description,
    Integer stock,
    String imageUrl,
    String shopName
) {}
