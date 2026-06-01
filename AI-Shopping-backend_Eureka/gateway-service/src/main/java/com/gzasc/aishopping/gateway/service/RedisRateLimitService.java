package com.gzasc.aishopping.gateway.service;

import com.gzasc.aishopping.gateway.config.IpRateLimitProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisRateLimitService {

    private static final String KEY_PREFIX = "rate_limit:ip:";

    private final StringRedisTemplate redisTemplate;
    private final IpRateLimitProperties ipRateLimitProperties;

    public RedisRateLimitService(StringRedisTemplate redisTemplate,
                                 IpRateLimitProperties ipRateLimitProperties) {
        this.redisTemplate = redisTemplate;
        this.ipRateLimitProperties = ipRateLimitProperties;
    }

    public boolean isAllowed(String ip) {
        String key = KEY_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, ipRateLimitProperties.getTimeWindowSeconds(), TimeUnit.SECONDS);
        }
        return count != null && count <= ipRateLimitProperties.getMaxRequests();
    }
}
