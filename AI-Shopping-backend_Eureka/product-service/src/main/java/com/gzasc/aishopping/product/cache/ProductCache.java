package com.gzasc.aishopping.product.cache;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 商品缓存层 - 预留接口
 * TODO: 后续可替换为 Redis 缓存
 */
@Component
public class ProductCache {

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    public Object get(String key) {
        return cache.get(key);
    }

    public void put(String key, Object value) {
        cache.put(key, value);
    }

    public void evict(String key) {
        cache.remove(key);
    }

    public void evictAll(List<String> keys) {
        keys.forEach(cache::remove);
    }
}
