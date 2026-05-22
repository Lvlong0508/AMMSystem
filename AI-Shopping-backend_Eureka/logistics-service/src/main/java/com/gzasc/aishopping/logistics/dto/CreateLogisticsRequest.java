package com.gzasc.aishopping.logistics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLogisticsRequest {
    @NotBlank(message = "订单号不能为空")
    private String orderId;

    private String type;

    @NotNull(message = "联系人ID不能为空")
    private Integer contactId;

    @NotBlank(message = "运单号不能为空")
    private String trackingNumber;
}
