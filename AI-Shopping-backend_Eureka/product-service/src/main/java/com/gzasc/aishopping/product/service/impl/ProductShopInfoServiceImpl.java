package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.feign.shop.ShopFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.product.service.ProductShopInfoService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductShopInfoServiceImpl implements ProductShopInfoService {

    private final ShopFeignClient shopFeignClient;

    private final Cache<Long, ShopInfoDTO> shopInfoCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    @Override
    public ShopInfoDTO getCachedShopInfo(Long shopId) {
        if (shopId == null) return null;
        try {
            return shopInfoCache.get(shopId, id -> {
                ApiResponse<ShopInfoDTO> response = shopFeignClient.getShopInfo(id);
                return response != null ? response.getData() : null;
            });
        } catch (Exception e) {
            log.warn("获取店铺信息失败, shopId={}", shopId, e);
            return null;
        }
    }

    @Override
    public Map<Long, ShopInfoDTO> batchGetShopInfo(Set<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) return Map.of();
        Set<Long> uncached = shopIds.stream()
            .filter(id -> shopInfoCache.getIfPresent(id) == null)
            .collect(Collectors.toSet());
        if (!uncached.isEmpty()) {
            try {
                ApiResponse<Map<Long, ShopInfoDTO>> response = shopFeignClient.batchGetShopInfo(uncached);
                if (response != null && response.getData() != null) {
                    response.getData().forEach(shopInfoCache::put);
                }
            } catch (Exception e) {
                log.warn("批量获取店铺信息失败, shopIds={}", uncached, e);
            }
        }
        return shopIds.stream()
            .filter(id -> shopInfoCache.getIfPresent(id) != null)
            .collect(Collectors.toMap(id -> id, shopInfoCache::getIfPresent));
    }
}
