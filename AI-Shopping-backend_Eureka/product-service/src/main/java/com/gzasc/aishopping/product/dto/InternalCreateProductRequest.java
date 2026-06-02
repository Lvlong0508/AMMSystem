package com.gzasc.aishopping.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class InternalCreateProductRequest {
    @NotBlank(message = "商品名称不能为空")
    private String name;

    @Positive(message = "商品价格必须大于0")
    private BigDecimal price;

    private String tags;
    private String description;

    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;
}
