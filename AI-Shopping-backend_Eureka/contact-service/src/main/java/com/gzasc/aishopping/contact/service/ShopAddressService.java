package com.gzasc.aishopping.contact.service;

import com.gzasc.aishopping.contact.model.ShopAddress;

import java.util.List;

public interface ShopAddressService {
    int createAddress(ShopAddress address, String shopId);
    int deleteAddress(int id, String shopId);
    int updateAddress(ShopAddress address, String shopId);
    ShopAddress getAddressById(int id, String shopId);
    List<ShopAddress> getAddressesByShopId(String shopId);

    List<ShopAddress> getShipAddressesByShopId(String shopId);
    int setDefaultAddress(int id, String shopId);
}