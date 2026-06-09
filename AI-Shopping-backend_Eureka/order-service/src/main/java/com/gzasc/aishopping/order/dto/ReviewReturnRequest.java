package com.gzasc.aishopping.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewReturnRequest {
    @NotBlank(message = "审核结果不能为空")
    private String status;
}
