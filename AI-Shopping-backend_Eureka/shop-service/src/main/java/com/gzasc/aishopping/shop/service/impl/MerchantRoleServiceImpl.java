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
    public MerchantRole selectById(Long id) {
        return merchantRoleMapper.selectById(id);
    }

    @Override
    public List<MerchantRole> selectByMerchantId(Long merchantId) {
        return merchantRoleMapper.selectByMerchantId(merchantId);
    }

    @Override
    public List<MerchantRole> selectByShopId(Long shopId) {
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
    public int deleteById(Long id) {
        return merchantRoleMapper.deleteById(id);
    }

    @Override
    public MerchantRole selectByMerchantAndShop(Long merchantId, Long shopId) {
        return merchantRoleMapper.selectByMerchantAndShop(merchantId, shopId);
    }

    @Override
    public MerchantRole selectByMerchantShopAndRole(Long merchantId, Long shopId, Integer role) {
        return merchantRoleMapper.selectByMerchantShopAndRole(merchantId, shopId, role);
    }
}