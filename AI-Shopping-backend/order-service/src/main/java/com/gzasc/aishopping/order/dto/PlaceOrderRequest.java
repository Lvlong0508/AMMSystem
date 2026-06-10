package com.gzasc.aishopping.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceOrderRequest {
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Min(value = 1, message = "购买数量必须大于0")
    private int quantity;

    @NotNull(message = "联系人ID不能为空")
    private Integer contactId;
}
