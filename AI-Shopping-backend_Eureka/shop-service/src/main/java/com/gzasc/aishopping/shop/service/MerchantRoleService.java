package com.gzasc.aishopping.shop.service;

import com.gzasc.aishopping.shop.model.MerchantRole;

import java.util.List;

public interface MerchantRoleService {
    List<MerchantRole> selectByMerchantId(Long merchantId);
    List<MerchantRole> selectByShopId(Long shopId);
    int insert(MerchantRole merchantRole);
    int deleteById(Long id);
    int deleteByMerchantAndShop(Long merchantId, Long shopId);
    MerchantRole selectByMerchantAndShop(Long merchantId, Long shopId);
    MerchantRole selectByMerchantShopAndRole(Long merchantId, Long shopId, Integer role);
}