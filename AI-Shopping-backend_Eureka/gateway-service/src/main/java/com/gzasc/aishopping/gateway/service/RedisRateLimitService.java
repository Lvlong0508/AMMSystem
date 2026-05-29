package com.gzasc.aishopping.gateway.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisRateLimitService {

    private static final String KEY_PREFIX = "rate_limit:ip:";
    private static final long MAX_REQUESTS = 300;
    private static final long WINDOW_SECONDS = 60;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean isAllowed(String ip) {
        String key = KEY_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        return count != null && count <= MAX_REQUESTS;
    }
}
