package com.gzasc.aishopping.common.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShipOrderRequest {
    @NotBlank(message = "物流单号不能为空")
    private String trackingNumber;

    @NotNull(message = "联系人ID不能为空")
    private Integer contactId;

    private String shippingDate;
}