package com.gzasc.aishopping.common.dto.order;

import lombok.Data;
import java.io.Serializable;

@Data
public class OrderAbstractSellerDTO implements Serializable {
    private String orderId;
    private String productId;
    private Integer contactId;
    private int quantity;
    private String orderStatus;
}
