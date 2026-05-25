package com.gzasc.aishopping.shop.service;

import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.model.Shop;

import java.util.List;

public interface ShopService {
    Shop getShopById(String shopId);
    List<Shop> getShopsByMerchantId(String merchantId);
    List<Shop> getShopsByUserId(String userId);
    List<Shop> getAllShops(int page);
    int createShop(Shop shop);

    /**
     * 使用 DTO 创建店铺
     */
    Shop createShop(CreateShopRequest request, String userId);
    int updateShop(Shop shop);
    int closeShop(String shopId);
    int countActiveShops();
    List<Shop> getActiveShops(int page, int size);
}