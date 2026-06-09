package com.gzasc.aishopping.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitReturnLogisticsRequest {
    @NotBlank(message = "快递单号不能为空")
    private String trackingNumber;

    @NotNull(message = "联系人ID不能为空")
    private Integer contactId;
}
