package com.gzasc.aishopping.common.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReserveRequest implements Serializable {
    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Positive(message = "数量必须大于0")
    private int quantity;
}
