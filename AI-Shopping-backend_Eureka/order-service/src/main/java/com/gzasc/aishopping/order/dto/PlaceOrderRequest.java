package com.gzasc.aishopping.order.dto;

import lombok.Data;

@Data
public class PlaceOrderRequest {
    private String productId;
    private int quantity;
    private ContactDTO contact;

    @Data
    public static class ContactDTO {
        private Integer id;
        private String name;
        private String phone;
        private String address;
    }
}
