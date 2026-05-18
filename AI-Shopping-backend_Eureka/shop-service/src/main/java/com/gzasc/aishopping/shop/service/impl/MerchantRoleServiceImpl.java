package com.gzasc.aishopping.shop.service.impl;

import com.gzasc.aishopping.shop.mapper.MerchantRoleMapper;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantRoleServiceImpl implements MerchantRoleService {

    private final MerchantRoleMapper merchantRoleMapper;

    @Override
    public MerchantRole selectById(String id) {
        return merchantRoleMapper.selectById(id);
    }

    @Override
    public List<MerchantRole> selectByMerchantId(String merchantId) {
        return merchantRoleMapper.selectByMerchantId(merchantId);
    }

    @Override
    public List<MerchantRole> selectByShopId(String shopId) {
        return merchantRoleMapper.selectByShopId(shopId);
    }

    @Override
    public int insert(MerchantRole merchantRole) {
        return merchantRoleMapper.insert(merchantRole);
    }

    @Override
    public int updateRole(MerchantRole merchantRole) {
        return merchantRoleMapper.updateRole(merchantRole);
    }

    @Override
    public int deleteById(String id) {
        return merchantRoleMapper.deleteById(id);
    }

    @Override
    public MerchantRole selectByMerchantAndShop(String merchantId, String shopId) {
        return merchantRoleMapper.selectByMerchantAndShop(merchantId, shopId);
    }

    @Override
    public MerchantRole selectByMerchantShopAndRole(String merchantId, String shopId, String role) {
        return merchantRoleMapper.selectByMerchantShopAndRole(merchantId, shopId, role);
    }
}