package com.gzasc.aishopping.product.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReservation {
    public static final String RESERVED = "RESERVED";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String RELEASED = "RELEASED";

    private Long id;
    private String productId;
    private String orderId;
    private int quantity;
    private String status;
    private Date createdAt;
    private Date expiredAt;
}
