package com.gzasc.aishopping.shop.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateShopRequest {

    @Size(max = 100, message = "店铺名称最长100个字符")
    private String name;

    @Size(max = 500, message = "店铺描述最长500个字符")
    private String description;

    private String logoId;
}
