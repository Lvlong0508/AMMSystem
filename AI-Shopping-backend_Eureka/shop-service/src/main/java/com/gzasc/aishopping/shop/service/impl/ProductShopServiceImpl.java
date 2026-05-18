package com.gzasc.aishopping.shop.service.impl;

import com.gzasc.aishopping.shop.mapper.ProductShopMapper;
import com.gzasc.aishopping.shop.model.ProductShop;
import com.gzasc.aishopping.shop.service.ProductShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductShopServiceImpl implements ProductShopService {

    private final ProductShopMapper productShopMapper;

    @Override
    public List<ProductShop> selectByShopId(String shopId) {
        return productShopMapper.selectByShopId(shopId);
    }

    @Override
    public List<ProductShop> selectByProductId(String productId) {
        return productShopMapper.selectByProductId(productId);
    }

    @Override
    public String selectShopIdByProductId(String productId) {
        return productShopMapper.selectShopIdByProductId(productId);
    }

    @Override
    public int insert(ProductShop productShop) {
        return productShopMapper.insert(productShop);
    }

    @Override
    public int deleteById(String id) {
        return productShopMapper.deleteById(id);
    }

    @Override
    public int deleteByShopAndProduct(String shopId, String productId) {
        return productShopMapper.deleteByShopAndProduct(shopId, productId);
    }
}