package com.gzasc.aishopping.shop.service;

import com.gzasc.aishopping.shop.model.Shop;

import java.util.List;

public interface ShopService {
    Shop getShopById(String shopId);
    List<Shop> getShopsByMerchantId(String merchantId);
    List<Shop> getShopsByUserId(String userId);
    List<Shop> getAllShops(int page);
    int createShop(Shop shop);
    int updateShop(Shop shop);
    int closeShop(String shopId);
    int countActiveShops();
    List<Shop> getActiveShops(int page, int size);
}