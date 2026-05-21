package com.gzasc.aishopping.product.exception;

public class ProductNotFoundException extends ProductServiceException {
    public ProductNotFoundException(String productId) {
        super("商品不存在: " + productId);
    }
}
