package com.gzasc.aishopping.chat.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ProductData.class, name = "product"),
    @JsonSubTypes.Type(value = OrderData.class, name = "order")
})
public sealed interface Data permits ProductData, OrderData {}
