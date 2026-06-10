package com.gzasc.aishopping.product.service;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;

import java.util.Map;
import java.util.Set;

public interface ProductShopInfoService {

    ShopInfoDTO getCachedShopInfo(Long shopId);

    Map<Long, ShopInfoDTO> batchGetShopInfo(Set<Long> shopIds);
}
