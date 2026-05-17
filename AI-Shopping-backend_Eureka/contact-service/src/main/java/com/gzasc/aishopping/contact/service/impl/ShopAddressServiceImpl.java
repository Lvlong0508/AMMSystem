package com.gzasc.aishopping.contact.service.impl;

import com.gzasc.aishopping.contact.mapper.ShopAddressMapper;
import com.gzasc.aishopping.contact.mapper.ShopAddressRelMapper;
import com.gzasc.aishopping.contact.model.ShopAddress;
import com.gzasc.aishopping.contact.service.ShopAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopAddressServiceImpl implements ShopAddressService {

    private final ShopAddressMapper shopAddressMapper;
    private final ShopAddressRelMapper shopAddressRelMapper;

    @Override
    @Transactional
    public int createAddress(ShopAddress address, String shopId) {
        if (address.getIsDefault() == 1) {
            shopAddressMapper.clearDefaultByType(shopId, address.getAddressType());
        }
        int rows = shopAddressMapper.insertAddress(address);
        if (rows > 0) {
            shopAddressRelMapper.insertRel(shopId, address.getId());
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteAddress(int id, String shopId) {
        String checkShopId = shopAddressRelMapper.selectShopIdByAddressId(id);
        if (!shopId.equals(checkShopId)) {
            return 0;
        }
        shopAddressRelMapper.deleteRelByAddressId(id);
        return shopAddressMapper.deleteAddressById(id);
    }

    @Override
    @Transactional
    public int updateAddress(ShopAddress address, String shopId) {
        String checkShopId = shopAddressRelMapper.selectShopIdByAddressId(address.getId());
        if (!shopId.equals(checkShopId)) {
            return 0;
        }
        if (address.getIsDefault() == 1) {
            shopAddressMapper.clearDefaultByType(shopId, address.getAddressType());
        }
        return shopAddressMapper.updateAddress(address);
    }

    @Override
    public ShopAddress getAddressById(int id, String shopId) {
        String checkShopId = shopAddressRelMapper.selectShopIdByAddressId(id);
        if (!shopId.equals(checkShopId)) {
            return null;
        }
        return shopAddressMapper.selectAddressById(id);
    }

    @Override
    public List<ShopAddress> getAddressesByShopId(String shopId) {
        return shopAddressMapper.selectAddressesByShopId(shopId);
    }

    @Override
    public List<ShopAddress> getShipAddressesByShopId(String shopId) {
        return shopAddressMapper.selectShipAddressesByShopId(shopId);
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
}