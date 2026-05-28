package com.gzasc.aishopping.order.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisOrderIdGeneratorTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    @DisplayName("OR-060 订单号格式验证 - yyyyMMdd+5位INCR+5位字母")
    void generate_format() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);

        RedisOrderIdGenerator gen = new RedisOrderIdGenerator(redisTemplate);
        String orderId = gen.generate();

        assertNotNull(orderId);
        assertEquals(18, orderId.length());

        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertTrue(orderId.startsWith(currentDate));

        String seqPart = orderId.substring(8, 13);
        assertTrue(seqPart.matches("\\d{5}"));

        String randomPart = orderId.substring(13, 18);
        assertTrue(randomPart.matches("[A-Z]{5}"));
    }

    @Test
    @DisplayName("OR-061 订单号递增 - 连续生成两个订单号INCR部分递增")
    void generate_increment() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString()))
                .thenReturn(1L)
                .thenReturn(2L);
        when(redisTemplate.expire(anyString(), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);

        RedisOrderIdGenerator gen = new RedisOrderIdGenerator(redisTemplate);
        String id1 = gen.generate();
        String id2 = gen.generate();

        String seq1 = id1.substring(8, 13);
        String seq2 = id2.substring(8, 13);
        assertEquals("00001", seq1);
        assertEquals("00002", seq2);
    }

    @Test
    @DisplayName("OR-062 新key从1开始，设置24h过期")
    void generate_newKeyFirstSequence() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);

        RedisOrderIdGenerator gen = new RedisOrderIdGenerator(redisTemplate);
        String orderId = gen.generate();

        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String expectedKey = "order:seq:" + currentDate;
        verify(valueOps).increment(expectedKey);
        verify(redisTemplate).expire(expectedKey, 24L, TimeUnit.HOURS);
        assertEquals("00001", orderId.substring(8, 13));
    }

    @Test
    @DisplayName("OR-062 非首次调用不设置过期")
    void generate_subsequentNoExpire() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(5L);

        RedisOrderIdGenerator gen = new RedisOrderIdGenerator(redisTemplate);
        gen.generate();

        verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
    }
}
