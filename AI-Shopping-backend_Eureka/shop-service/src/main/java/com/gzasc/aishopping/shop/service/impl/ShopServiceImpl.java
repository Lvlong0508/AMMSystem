package com.gzasc.aishopping.shop.service.impl;

import com.gzasc.aishopping.shop.mapper.MerchantRoleMapper;
import com.gzasc.aishopping.shop.mapper.ShopMapper;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;
    private final MerchantRoleMapper merchantRoleMapper;

    @Override
    public Shop getShopById(String shopId) {
        return shopMapper.selectShopById(shopId);
    }

    @Override
    public List<Shop> getShopsByMerchantId(String merchantId) {
        return shopMapper.selectShopsByMerchantId(merchantId);
    }

    @Override
    public List<Shop> getAllShops(int page) {
        int offset = (page - 1) * 20;
        return shopMapper.selectShopsByPage(offset);
    }

    @Override
    @Transactional
    public int createShop(Shop shop) {
        try {
            int result = shopMapper.insertShop(shop);
            if (result > 0) {
                MerchantRole merchantRole = new MerchantRole();
                merchantRole.setId(UUID.randomUUID().toString());
                merchantRole.setMerchantId(shop.getMerchantId());
                merchantRole.setShopId(shop.getId());
                merchantRole.setRole("1");
                merchantRole.setAssignedBy(shop.getMerchantId());
                merchantRoleMapper.insert(merchantRole);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("创建店铺失败", e);
        }
    }

    @Override
    public int updateShop(Shop shop) {
        return shopMapper.updateShop(shop);
    }

    @Override
    public int closeShop(String shopId) {
        return shopMapper.closeShop(shopId);
    }

    @Override
    public int countActiveShops() {
        return shopMapper.countActiveShops();
    }

    @Override
    public List<Shop> getActiveShops(int page, int size) {
        int offset = (page - 1) * size;
        return shopMapper.selectActiveShops(offset, size);
    }
}