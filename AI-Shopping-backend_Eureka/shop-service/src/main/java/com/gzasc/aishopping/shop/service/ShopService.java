package com.gzasc.aishopping.shop.service;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.shop.dto.AddEmployeeRequest;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.dto.UpdateShopRequest;
import com.gzasc.aishopping.shop.model.Shop;

import java.util.List;
import java.util.Map;

public interface ShopService {
    Shop getShopById(Long shopId);
    List<Shop> getShopsByMerchantId(Long merchantId);
    List<Shop> getShopsByUserId(Long userId);
    List<Shop> getAllShops(int page);
    int createShop(Shop shop);

    /**
     * 使用 DTO 创建店铺
     */
    Shop createShop(CreateShopRequest request, Long userId);
    int updateShop(Shop shop);
    int closeShop(Long shopId);
    int countActiveShops();
    List<Shop> getActiveShops(int page, int size);

    // ===== 带权限检查的店铺操作 =====
    void updateShop(Long shopId, UpdateShopRequest request, Long userId);
    void closeShop(Long shopId, Long userId);

    // ===== 商品查询（分页 + 详情） =====
    Map<String, Object> getShopProductsWithPagination(Long shopId, int page, int size);
    ProductDTO getProductDetailByShop(Long shopId, Long productId);

    // ===== 商品管理（Feign 调用 product-service） =====
    void createProduct(Long shopId, ProductDTO productDTO, Long userId);
    void updateProduct(Long shopId, Long productId, ProductDTO productDTO, Long userId);
    void deleteProduct(Long shopId, Long productId, Long userId);

    // ===== 员工管理（Feign 调用 auth-service） =====
    void addEmployee(Long shopId, AddEmployeeRequest request, Long userId);
    void removeEmployee(Long shopId, Long merchantId, Long userId);

    // ===== 查询（Feign 调用 + 权限检查） =====
    Shop getShopWithAccessCheck(Long shopId, Long userId);
    Map<String, Object> getShopProductsWithDetails(Long shopId, Long userId, int page, int size);
    Map<String, Object> getShopEmployees(Long shopId, Long userId);

    // ===== C端用户查询（ShopUserController 业务下沉） =====
    Map<String, Object> getActiveShopById(Long shopId);
    Map<String, Object> getUserShopList(int page, int size);
    Map<String, Object> getUserShopProducts(Long shopId, int page, int size);
    Map<String, Object> getUserShopProductDetail(Long shopId, Long productId);
}