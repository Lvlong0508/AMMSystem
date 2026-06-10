package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.product.mapper.ProductStockMapper;
import com.gzasc.aishopping.product.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductStockMapper productStockMapper;

    @Override
    @Transactional
    public boolean restoreStock(Long productId, int quantity) {
        int rows = productStockMapper.restoreStock(productId, quantity);
        if (rows > 0) {
            log.info("归还库存成功 productId={}, quantity={}", productId, quantity);
        } else {
            log.warn("归还库存失败 productId={}, quantity={}", productId, quantity);
        }
        return rows > 0;
    }
}
