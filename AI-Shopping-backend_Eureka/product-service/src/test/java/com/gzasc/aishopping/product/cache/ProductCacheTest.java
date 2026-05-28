package com.gzasc.aishopping.product.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductCacheTest {

    private ProductCache productCache;

    @BeforeEach
    void setUp() {
        productCache = new ProductCache();
    }

    @Test
    @DisplayName("PR-077 - Caffeine 缓存 - 店铺信息缓存命中")
    void testCacheHit() {
        productCache.put("shop:1", "testShop");
        Object cached = productCache.get("shop:1");
        assertEquals("testShop", cached);
    }

    @Test
    @DisplayName("PR-077 - Caffeine 缓存 - 未命中返回null")
    void testCacheMiss() {
        Object cached = productCache.get("nonExistent");
        assertNull(cached);
    }

    @Test
    @DisplayName("PR-078 - Caffeine 缓存 - 手动淘汰后重新加载")
    void testCacheEvict() {
        productCache.put("shop:1", "testShop");
        productCache.evict("shop:1");
        assertNull(productCache.get("shop:1"));
    }

    @Test
    @DisplayName("PR-078 - Caffeine 缓存 - 批量淘汰")
    void testCacheEvictAll() {
        productCache.put("shop:1", "shop1");
        productCache.put("shop:2", "shop2");
        productCache.evictAll(List.of("shop:1", "shop:2"));
        assertNull(productCache.get("shop:1"));
        assertNull(productCache.get("shop:2"));
    }

    @Test
    @DisplayName("PR-079 - Caffeine 缓存 - 大量数据put和get")
    void testCacheLargeCapacity() {
        for (int i = 0; i < 2000; i++) {
            productCache.put("key:" + i, "value:" + i);
        }
        assertEquals("value:1999", productCache.get("key:1999"));
        assertEquals("value:0", productCache.get("key:0"));
    }

    @Test
    @DisplayName("PR-079 - Caffeine 缓存 - 覆盖已有key")
    void testCacheOverwrite() {
        productCache.put("shop:1", "oldValue");
        productCache.put("shop:1", "newValue");
        assertEquals("newValue", productCache.get("shop:1"));
    }
}
