package com.gzasc.aishopping.shop.service.impl;

import com.gzasc.aishopping.shop.mapper.ShopInfoMapper;
import com.gzasc.aishopping.shop.model.ShopInfo;
import com.gzasc.aishopping.shop.service.ShopInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopInfoServiceImpl implements ShopInfoService {

    private final ShopInfoMapper shopInfoMapper;

    @Override
    public ShopInfo getById(Long id) {
        return shopInfoMapper.selectById(id);
    }

    @Override
    public List<ShopInfo> getByIds(List<Long> ids) {
        return shopInfoMapper.selectBatch(ids);
    }

    @Override
    public int insert(ShopInfo shopInfo) {
        return shopInfoMapper.insert(shopInfo);
    }

    @Override
    public int update(ShopInfo shopInfo) {
        return shopInfoMapper.update(shopInfo);
    }
}
