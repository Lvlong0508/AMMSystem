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

    // ==================== 补充覆盖 (sequence 为 null 边界) ====================

    @Test
    @DisplayName("OID-01 increment 返回 null 时抛 IllegalStateException 含 key 信息")
    void generate_sequenceNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(null);

        RedisOrderIdGenerator gen = new RedisOrderIdGenerator(redisTemplate);
        IllegalStateException ex = assertThrows(IllegalStateException.class, gen::generate);

        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String expectedKey = "order:seq:" + currentDate;
        assertTrue(ex.getMessage().contains(expectedKey),
                "异常信息应包含 Redis key，便于排障: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("INCR"),
                "异常信息应说明是 INCR 操作: " + ex.getMessage());
    }

    @Test
    @DisplayName("OID-02 大量递增 - sequence 为 99999 时仍正常格式化")
    void generate_largeSequence() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(99999L);

        RedisOrderIdGenerator gen = new RedisOrderIdGenerator(redisTemplate);
        String orderId = gen.generate();

        assertEquals("99999", orderId.substring(8, 13));
    }

    @Test
    @DisplayName("OID-03 随机后缀 - 5 位大写字母不出现数字或小写")
    void generate_randomSuffix() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);

        RedisOrderIdGenerator gen = new RedisOrderIdGenerator(redisTemplate);
        String orderId = gen.generate();

        String randomPart = orderId.substring(13, 18);
        assertTrue(randomPart.matches("[A-Z]{5}"),
                "随机后缀应为 5 位大写字母: " + randomPart);
    }

    @Test
    @DisplayName("OID-04 日期部分 - 与 yyyyMMdd 格式严格匹配")
    void generate_datePart() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);

        RedisOrderIdGenerator gen = new RedisOrderIdGenerator(redisTemplate);
        String orderId = gen.generate();

        String datePart = orderId.substring(0, 8);
        assertTrue(datePart.matches("\\d{8}"));
        assertEquals(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), datePart);
    }
}
