package com.gzasc.aishopping.shop.service;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.shop.dto.AddEmployeeRequest;
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

    // ===== 商品管理（Feign 调用 product-service） =====
    void createProduct(String shopId, ProductDTO productDTO, String userId);
    void updateProduct(String shopId, String productId, ProductDTO productDTO, String userId);
    void deleteProduct(String shopId, String productId, String userId);

    // ===== 员工管理（Feign 调用 auth-service） =====
    void addEmployee(String shopId, AddEmployeeRequest request, String userId);
    void removeEmployee(String shopId, String merchantId, String userId);
}