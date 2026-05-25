package com.gzasc.aishopping.shop.service;

import com.gzasc.aishopping.shop.model.ProductShop;

import java.util.List;

public interface ProductShopService {
    List<ProductShop> selectByShopId(Long shopId);
    List<ProductShop> selectByProductId(Long productId);
    Long selectShopIdByProductId(Long productId);
    int insert(ProductShop productShop);
    int deleteById(Long id);
    int deleteByShopAndProduct(Long shopId, Long productId);
}