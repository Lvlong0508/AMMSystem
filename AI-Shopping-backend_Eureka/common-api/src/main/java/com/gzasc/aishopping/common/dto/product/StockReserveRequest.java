package com.gzasc.aishopping.common.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReserveRequest implements Serializable {
    private String orderId;
    private String productId;
    private int quantity;
}
