package com.gzasc.aishopping.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class StockRequest {

    @NotBlank(message = "商品ID不能为空")
    private String productId;

    @NotNull(message = "库存数量不能为空")
    @Positive(message = "库存数量必须大于0")
    private Integer quantity;

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}