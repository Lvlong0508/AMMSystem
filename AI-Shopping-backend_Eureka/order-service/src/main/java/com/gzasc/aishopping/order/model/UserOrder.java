package com.gzasc.aishopping.order.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class UserOrder {
    private Integer id;
    private Integer userId;
    private String orderId;
    private Timestamp createdAt;
}