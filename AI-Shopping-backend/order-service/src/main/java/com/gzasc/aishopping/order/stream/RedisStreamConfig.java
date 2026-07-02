package com.gzasc.aishopping.order.stream;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Redis Stream 基础配置，负责定义 Stream Key 和消费者组名，
 * 并在启动时自动创建消费者组（MKSTREAM 模式，Stream 不存在则自动创建）。
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisStreamConfig {

    private final RedisTemplate<String, String> redisTemplate;

    @Getter
    @Value("${order.stream.key:order:events}")
    private String streamKey;

    @Getter
    @Value("${order.stream.group:order:processors}")
    private String groupName;

    /**
     * 应用启动后尝试创建消费者组。
     * 如果组已存在则忽略异常，确保幂等。
     */
    @PostConstruct
    public void init() {
        try {
            final String sk = streamKey;
            final String gn = groupName;
            redisTemplate.execute((RedisCallback<String>) conn -> {
                conn.xGroupCreate(
                        redisTemplate.getStringSerializer().serialize(sk),
                        gn,
                        ReadOffset.latest(),
                        true
                );
                return null;
            });
        } catch (Exception e) {
            log.info("Redis Stream group already exists, skip creation");
        }
    }
}
