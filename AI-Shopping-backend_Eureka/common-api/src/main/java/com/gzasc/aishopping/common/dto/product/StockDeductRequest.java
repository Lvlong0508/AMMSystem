package com.gzasc.aishopping.common.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 库存扣减请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDeductRequest implements Serializable {
    private String productId;
    private int quantity;
}
