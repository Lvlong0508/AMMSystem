package com.gzasc.aishopping.shop.service;

import com.gzasc.aishopping.shop.model.ProductShop;

import java.util.List;

public interface ProductShopService {
    List<ProductShop> selectByShopId(String shopId);
    List<ProductShop> selectByProductId(String productId);
    String selectShopIdByProductId(String productId);
    int insert(ProductShop productShop);
    int deleteById(String id);
    int deleteByShopAndProduct(String shopId, String productId);
}