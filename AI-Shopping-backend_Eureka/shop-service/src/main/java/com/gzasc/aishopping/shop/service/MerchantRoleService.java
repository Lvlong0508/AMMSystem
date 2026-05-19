package com.gzasc.aishopping.shop.service;

import com.gzasc.aishopping.shop.model.MerchantRole;

import java.util.List;

public interface MerchantRoleService {
    MerchantRole selectById(Long id);
    List<MerchantRole> selectByMerchantId(String merchantId);
    List<MerchantRole> selectByShopId(String shopId);
    int insert(MerchantRole merchantRole);
    int updateRole(MerchantRole merchantRole);
    int deleteById(Long id);
    MerchantRole selectByMerchantAndShop(String merchantId, String shopId);
    MerchantRole selectByMerchantShopAndRole(String merchantId, String shopId, String role);
}