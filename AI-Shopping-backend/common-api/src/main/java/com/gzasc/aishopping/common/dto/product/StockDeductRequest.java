package com.gzasc.aishopping.common.dto.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Positive(message = "数量必须大于0")
    private int quantity;
}
