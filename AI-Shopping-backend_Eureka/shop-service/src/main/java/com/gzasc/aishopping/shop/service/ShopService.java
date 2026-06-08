package com.gzasc.aishopping.shop.service;

import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.dto.SimpleShopDTO;
import com.gzasc.aishopping.shop.dto.UpdateShopRequest;
import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.shop.model.Shop;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

public interface ShopService {

    Shop createShop(CreateShopRequest request, Long userId);
    Shop createShop(CreateShopRequest request, Long userId, MultipartFile logo);
    void updateShop(Long shopId, UpdateShopRequest request, Long userId);
    void closeShop(Long shopId, Long userId);
    void openShop(Long shopId, Long userId);
    Shop getShopWithAccessCheck(Long shopId, Long userId);
    Map<String, Object> getActiveShopById(Long shopId);
    Map<String, Object> getUserShopList(int page, int size);
    SimpleShopDTO getMyShop(Long userId);
    ShopInfoDTO getShopInfoById(Long shopId);
    Map<Long, ShopInfoDTO> batchGetShopInfo(Set<Long> shopIds);
}
