package com.gzasc.aishopping.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReturnRequest {
    @NotBlank(message = "退货原因不能为空")
    @Size(max = 500, message = "退货原因不能超过500字")
    private String returnReason;
}
