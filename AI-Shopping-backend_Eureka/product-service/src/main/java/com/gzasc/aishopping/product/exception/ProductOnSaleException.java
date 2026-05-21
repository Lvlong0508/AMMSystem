package com.gzasc.aishopping.product.exception;

public class ProductOnSaleException extends ProductServiceException {
    public ProductOnSaleException(String productId) {
        super("商品在上架中，请先下架: " + productId);
    }
}
