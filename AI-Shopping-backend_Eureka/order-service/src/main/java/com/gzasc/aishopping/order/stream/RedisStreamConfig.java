package com.gzasc.aishopping.order.stream;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisStreamConfig {

    private final RedisTemplate<String, String> redisTemplate;

    static final String STREAM_KEY = "order:events";
    static final String GROUP_NAME = "order:processors";

    @PostConstruct
    public void init() {
        try {
            redisTemplate.execute((RedisCallback<String>) conn -> {
                conn.xGroupCreate(
                        redisTemplate.getStringSerializer().serialize(STREAM_KEY),
                        GROUP_NAME,
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
