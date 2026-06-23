package com.gzasc.aishopping.gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的 IP 级别请求限流服务。
 *
 * 利用 Redis 自增计数 + TTL 实现滑动窗口限流:
 *   - 每请求将 key("rate_limit:ip:{IP}") 增 1
 *   - 首次请求(计数值=1)时设置 TTL=时间窗口
 *   - 计数值超过阈值时拒绝
 *
 * 默认配置: 每个 IP 60 秒内最多 300 次请求(由 yml/Nacos 的 ip-rate-limit.* 控制)。
 */
@Service
public class RedisRateLimitService {

    /** Redis key 前缀,每个 IP 有独立的计数器 */
    private static final String KEY_PREFIX = "rate_limit:ip:";

    private final StringRedisTemplate redisTemplate;

    /** 时间窗口内允许的最大请求数 */
    @Value("${ip-rate-limit.max-requests}")
    private int maxRequests;

    /** 限流时间窗口(秒) */
    @Value("${ip-rate-limit.time-window-seconds}")
    private int timeWindowSeconds;

    public RedisRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 判断指定 IP 是否允许通过。
     * 原子自增 Redis 计数,首次访问时设置过期时间。
     *
     * @param ip 客户端 IP
     * @return true=通过, false=超限拒绝
     */
    public boolean isAllowed(String ip) {
        String key = KEY_PREFIX + ip;
        // INCR 命令原子递增,返回值是递增后的值
        Long count = redisTemplate.opsForValue().increment(key);
        // 第一个请求: 设置 TTL,后续请求的 TTL 不变(Redis 策略:不重置过期时间)
        if (count != null && count == 1) {
            redisTemplate.expire(key, timeWindowSeconds, TimeUnit.SECONDS);
        }
        return count != null && count <= maxRequests;
    }
}
