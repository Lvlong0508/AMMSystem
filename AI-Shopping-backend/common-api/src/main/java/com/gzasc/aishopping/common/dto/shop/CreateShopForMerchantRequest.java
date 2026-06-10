package com.gzasc.aishopping.common.dto.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateShopForMerchantRequest {

    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @NotBlank(message = "店铺名称不能为空")
    private String name;

    private String description;

    private String logoUrl;
}