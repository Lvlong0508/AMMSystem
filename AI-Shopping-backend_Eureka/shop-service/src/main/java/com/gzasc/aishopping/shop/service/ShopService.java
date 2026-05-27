package com.gzasc.aishopping.shop.service;

import com.gzasc.aishopping.shop.dto.AddEmployeeRequest;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.dto.UpdateShopRequest;
import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.shop.model.Shop;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ShopService {

    // ===== 店铺管理 =====
    Shop createShop(CreateShopRequest request, Long userId);
    void updateShop(Long shopId, UpdateShopRequest request, Long userId);
    void closeShop(Long shopId, Long userId);
    void openShop(Long shopId, Long userId);

    // ===== 员工管理 =====
    void addEmployee(Long shopId, AddEmployeeRequest request, Long userId);
    void removeEmployee(Long shopId, Long merchantId, Long userId);

    // ===== 查询（含权限检查） =====
    Shop getShopWithAccessCheck(Long shopId, Long userId);
    Map<String, Object> getShopEmployees(Long shopId, Long userId);

    // ===== C端用户查询 =====
    Map<String, Object> getActiveShopById(Long shopId);
    Map<String, Object> getUserShopList(int page, int size);

    // ===== 查询商户关联店铺 =====
    List<Long> getShopIdsByMerchantId(Long merchantId);

    // ===== 内部接口查询（Feign 调用） =====
    ShopInfoDTO getShopInfoById(Long shopId);
    Map<Long, ShopInfoDTO> batchGetShopInfo(Set<Long> shopIds);
}