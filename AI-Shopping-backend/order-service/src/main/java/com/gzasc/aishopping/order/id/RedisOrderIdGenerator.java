package com.gzasc.aishopping.order.id;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisOrderIdGenerator implements OrderIdGenerator {

    private final StringRedisTemplate redisTemplate;

    @Override
    public String generate() {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = "order:seq:" + currentDate;
        Long sequence = redisTemplate.opsForValue().increment(key);
        if (sequence == null) {
            throw new IllegalStateException(
                    "Redis INCR 返回 null，无法生成订单号。请检查 Redis 连接状态。key=" + key);
        }
        if (sequence == 1) {
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        }
        String seqStr = String.format("%05d", sequence);
        String randomChars = generateRandomLetters();
        return currentDate + seqStr + randomChars;
    }

    private String generateRandomLetters() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            char c = (char) ('A' + random.nextInt(26));
            sb.append(c);
        }
        return sb.toString();
    }
}
