package com.gzasc.aishopping.shop.service.impl;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.util.SafeIdGenerator;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.dto.SimpleShopDTO;
import com.gzasc.aishopping.shop.dto.UpdateShopRequest;
import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.mapper.ShopMapper;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.model.ShopInfo;
import com.gzasc.aishopping.shop.service.ImageStorageService;
import com.gzasc.aishopping.shop.service.ShopInfoService;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;
    private final ShopInfoService shopInfoService;
    private final ImageStorageService imageStorageService;

    @Override
    @Transactional
    public Shop createShop(CreateShopRequest request, Long userId) {
        return createShop(request, userId, null);
    }

    @Override
    @Transactional
    public Shop createShop(CreateShopRequest request, Long userId, MultipartFile logo) {
        Shop existing = shopMapper.selectShopByMerchantId(userId);
        if (existing != null) {
            throw new ShopException("您已拥有店铺，不可重复创建");
        }

        Long shopId = SafeIdGenerator.nextId();
        String logoUrl = request.getLogoId();
        if (logo != null && !logo.isEmpty()) {
            logoUrl = imageStorageService.saveImage(shopId, logo);
        }

        ShopInfo shopInfo = new ShopInfo();
        shopInfo.setName(request.getName());
        shopInfo.setDescription(request.getDescription());
        shopInfo.setLogoUrl(logoUrl);
        shopInfo.setAddress(request.getAddress());
        shopInfo.setPhone(request.getPhone());
        shopInfoService.insert(shopInfo);

        Shop shop = new Shop();
        shop.setId(shopId);
        shop.setMerchantId(userId);
        shop.setShopInfoId(shopInfo.getId());
        shop.setStatus(1);
        int result = shopMapper.insertShop(shop);
        if (result <= 0) {
            throw new ShopException("创建店铺失败");
        }
        return shop;
    }

    @Override
    @Transactional
    public void updateShop(Long shopId, UpdateShopRequest request, Long userId) {
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null) {
            throw new ShopException("店铺不存在");
        }
        if (!shop.getMerchantId().equals(userId)) {
            throw new ShopException("无权操作该店铺");
        }
        if (request.getName() != null && request.getName().trim().isEmpty()) {
            throw new ShopException("店铺名称不能为空");
        }
        if (shop.getShopInfoId() != null) {
            ShopInfo shopInfo = new ShopInfo();
            shopInfo.setId(shop.getShopInfoId());
            shopInfo.setName(request.getName());
            shopInfo.setDescription(request.getDescription());
            shopInfo.setLogoUrl(request.getLogoId());
            shopInfo.setAddress(request.getAddress());
            shopInfo.setPhone(request.getPhone());
            shopInfoService.update(shopInfo);
        }
    }

    @Override
    @Transactional
    public void closeShop(Long shopId, Long userId) {
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null || !shop.getMerchantId().equals(userId)) {
            throw new ShopException("无权操作该店铺");
        }
        int result = shopMapper.closeShop(shopId);
        if (result <= 0) {
            throw new ShopException("店铺已关闭或不存在");
        }
    }

    @Override
    @Transactional
    public void openShop(Long shopId, Long userId) {
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null || !shop.getMerchantId().equals(userId)) {
            throw new ShopException("无权操作该店铺");
        }
        int result = shopMapper.openShop(shopId);
        if (result <= 0) {
            throw new ShopException("店铺已开启或不存在");
        }
    }

    @Override
    public Shop getShopWithAccessCheck(Long shopId, Long userId) {
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null) {
            throw new ShopException("店铺不存在");
        }
        if (!shop.getMerchantId().equals(userId)) {
            throw new ShopException("无权限访问该店铺");
        }
        return shop;
    }

    @Override
    public Map<String, Object> getActiveShopById(Long shopId) {
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null || shop.getStatus() != 1) {
            throw new ShopException("店铺不存在或已关闭");
        }
        ShopInfoDTO shopInfoDTO = null;
        if (shop.getShopInfoId() != null) {
            ShopInfo shopInfo = shopInfoService.getById(shop.getShopInfoId());
            if (shopInfo != null) {
                shopInfoDTO = new ShopInfoDTO(shopInfo.getId(), shopInfo.getName(), shopInfo.getDescription(), shopInfo.getLogoUrl(), shopInfo.getAddress(), shopInfo.getPhone());
            }
        }
        return Map.of("shop", shop, "shopInfo", shopInfoDTO);
    }

    @Override
    public Map<String, Object> getUserShopList(int page, int size) {
        if (page < 1) throw new ShopException("分页参数错误: page 必须 >= 1");
        if (size < 1) throw new ShopException("分页参数错误: size 必须 >= 1");
        List<Shop> shops = getActiveShops(page, size);
        int total = shopMapper.countActiveShops();
        Map<String, Object> result = new HashMap<>();
        result.put("shops", shops);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    private List<Shop> getActiveShops(int page, int size) {
        int offset = (page - 1) * size;
        return shopMapper.selectActiveShops(offset, size);
    }

    @Override
    public SimpleShopDTO getMyShop(Long userId) {
        Shop shop = shopMapper.selectShopByMerchantId(userId);
        if (shop == null) {
            return null;
        }
        String name = null;
        if (shop.getShopInfoId() != null) {
            ShopInfo shopInfo = shopInfoService.getById(shop.getShopInfoId());
            if (shopInfo != null) {
                name = shopInfo.getName();
            }
        }
        return new SimpleShopDTO(shop.getId(), name, shop.getStatus());
    }

    @Override
    public ShopInfoDTO getShopInfoById(Long shopId) {
        if (shopId == null) return null;
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null || shop.getShopInfoId() == null) return null;
        ShopInfo shopInfo = shopInfoService.getById(shop.getShopInfoId());
        if (shopInfo == null) return null;
        return new ShopInfoDTO(shopInfo.getId(), shopInfo.getName(), shopInfo.getDescription(), shopInfo.getLogoUrl(), shopInfo.getAddress(), shopInfo.getPhone());
    }

    @Override
    public Map<Long, ShopInfoDTO> batchGetShopInfo(Set<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) return Collections.emptyMap();
        List<Shop> shops = shopMapper.selectShopsByIds(shopIds);
        List<Long> infoIds = shops.stream()
                .map(Shop::getShopInfoId)
                .filter(Objects::nonNull)
                .toList();
        List<ShopInfo> shopInfos = shopInfoService.getByIds(infoIds);
        Map<Long, ShopInfo> infoMap = new HashMap<>();
        for (ShopInfo si : shopInfos) infoMap.put(si.getId(), si);
        Map<Long, ShopInfoDTO> result = new HashMap<>();
        for (Shop shop : shops) {
            if (shop.getShopInfoId() == null) continue;
            ShopInfo si = infoMap.get(shop.getShopInfoId());
            if (si == null) continue;
            result.put(shop.getId(), new ShopInfoDTO(si.getId(), si.getName(), si.getDescription(), si.getLogoUrl(), si.getAddress(), si.getPhone()));
        }
        return result;
    }
}
