package com.gzasc.aishopping.gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisRateLimitService {

    private static final String KEY_PREFIX = "rate_limit:ip:";

    private final StringRedisTemplate redisTemplate;

    @Value("${ip-rate-limit.max-requests:300}")
    private int maxRequests;

    @Value("${ip-rate-limit.time-window-seconds:60}")
    private int timeWindowSeconds;

    public RedisRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String ip) {
        String key = KEY_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, timeWindowSeconds, TimeUnit.SECONDS);
        }
        return count != null && count <= maxRequests;
    }
}
