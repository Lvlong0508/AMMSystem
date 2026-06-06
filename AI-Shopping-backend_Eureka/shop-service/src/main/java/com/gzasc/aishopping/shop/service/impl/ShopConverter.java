package com.gzasc.aishopping.shop.service.impl;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.shop.vo.ShopInfoVO;
import com.gzasc.aishopping.shop.vo.ShopVO;
import com.gzasc.aishopping.shop.model.Shop;
import org.springframework.stereotype.Component;

@Component
public class ShopConverter {

    public ShopVO toShopVO(Shop shop) {
        return new ShopVO(
                String.valueOf(shop.getId()),
                String.valueOf(shop.getMerchantId()),
                String.valueOf(shop.getShopInfoId()),
                shop.getStatus(),
                shop.getCreatedAt(),
                shop.getUpdatedAt()
        );
    }

    public ShopInfoVO toShopInfoVO(ShopInfoDTO dto) {
        if (dto == null) return null;
        return new ShopInfoVO(
                String.valueOf(dto.getId()),
                dto.getName(),
                dto.getDescription(),
                dto.getLogoUrl()
        );
    }
}
