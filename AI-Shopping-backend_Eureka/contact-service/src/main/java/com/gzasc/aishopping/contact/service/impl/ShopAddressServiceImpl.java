package com.gzasc.aishopping.contact.service.impl;

import com.gzasc.aishopping.contact.mapper.ShopAddressMapper;
import com.gzasc.aishopping.contact.model.ShopAddress;
import com.gzasc.aishopping.contact.service.ShopAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 店铺地址服务实现类
 * 提供店铺收货/发货地址的 CRUD 操作，支持店铺隔离
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopAddressServiceImpl implements ShopAddressService {

    private final ShopAddressMapper shopAddressMapper;

    @Override
    @Transactional
    public int createAddress(ShopAddress address, String shopId) {
        if (address.getIsDefault() == 1) {
            shopAddressMapper.clearDefaultByType(shopId, address.getAddressType());
        }
        int rows = shopAddressMapper.insertAddress(address);
        if (rows > 0) {
            shopAddressMapper.insertRel(shopId, address.getId());
        }
        return rows > 0 ? address.getId() : 0;
    }

    @Override
    @Transactional
    public int deleteAddress(int id, String shopId) {
        String checkShopId = shopAddressMapper.selectShopIdByAddressId(id);
        if (!shopId.equals(checkShopId)) {
            return 0;
        }
        shopAddressMapper.deleteRelByAddressId(id);
        return shopAddressMapper.deleteAddressById(id);
    }

    @Override
    @Transactional
    public int updateAddress(ShopAddress address, String shopId) {
        String checkShopId = shopAddressMapper.selectShopIdByAddressId(address.getId());
        if (!shopId.equals(checkShopId)) {
            return 0;
        }
        if (address.getIsDefault() == 1) {
            shopAddressMapper.clearDefaultByType(shopId, address.getAddressType());
        }
        return shopAddressMapper.updateAddress(address);
    }

    @Override
    public List<ShopAddress> getAddressesByShopId(String shopId) {
        return shopAddressMapper.selectAddressesByShopId(shopId);
    }

    @Override
    public ShopAddress getDefaultShipAddressByShopId(String shopId) {
        return shopAddressMapper.selectDefaultShipAddressByShopId(shopId);
    }

    @Override
    @Transactional
    public int setDefaultAddress(int id, String shopId) {
        ShopAddress address = getAddressById(id, shopId);
        if (address == null) {
            return 0;
        }
        shopAddressMapper.clearDefaultByType(shopId, address.getAddressType());
        return shopAddressMapper.setDefaultById(id);
    }

    // 只能内部使用，确保安全隔离
    private ShopAddress getAddressById(int id, String shopId) {
        String checkShopId = shopAddressMapper.selectShopIdByAddressId(id);
        if (!shopId.equals(checkShopId)) {
            return null;
        }
        return shopAddressMapper.selectAddressById(id);
    }
}