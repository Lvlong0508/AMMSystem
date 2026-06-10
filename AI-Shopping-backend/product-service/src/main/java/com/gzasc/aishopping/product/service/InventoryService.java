package com.gzasc.aishopping.product.service;

public interface InventoryService {

    /**
     * 归还库存
     *
     * @param productId 商品ID
     * @param quantity  归还数量
     * @return true 如果归还成功
     */
    boolean restoreStock(Long productId, int quantity);
}
