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
