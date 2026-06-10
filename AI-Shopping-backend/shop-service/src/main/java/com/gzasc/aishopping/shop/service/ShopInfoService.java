package com.gzasc.aishopping.shop.service;

import com.gzasc.aishopping.shop.model.ShopInfo;

import java.util.List;

public interface ShopInfoService {
    ShopInfo getById(Long id);
    List<ShopInfo> getByIds(List<Long> ids);
    int insert(ShopInfo shopInfo);
    int update(ShopInfo shopInfo);
}
