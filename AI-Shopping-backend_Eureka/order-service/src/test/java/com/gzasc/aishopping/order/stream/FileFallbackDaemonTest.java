package com.gzasc.aishopping.order.stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileFallbackDaemonTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private StreamOperations<String, Object, Object> streamOps;

    private FileFallbackDaemon daemon;

    private static final Path TEST_FALLBACK_DIR = Paths.get("data/failover");

    @BeforeEach
    void setUp() throws IOException {
        lenient().when(redisTemplate.opsForStream()).thenReturn(streamOps);
        daemon = new FileFallbackDaemon(redisTemplate);
        Files.createDirectories(TEST_FALLBACK_DIR);
        cleanup();
    }

    @AfterEach
    void cleanup() throws IOException {
        if (Files.exists(TEST_FALLBACK_DIR)) {
            try (var files = Files.list(TEST_FALLBACK_DIR)) {
                files.filter(p -> p.getFileName().toString().startsWith("failover-"))
                        .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
            }
        }
    }

    @Test
    @DisplayName("OR-045 sendOrFallback - Redis正常发送")
    void sendOrFallback_redisSuccess() {
        when(streamOps.add(any())).thenReturn(RecordId.of("12345-0"));

        daemon.sendOrFallback("STOCK_CONFIRM", "ORDER001",
                Map.of("productId", "1", "quantity", "2"));

        verify(streamOps).add(any());

    }

    @Test
    @DisplayName("OR-045 sendOrFallback - Redis异常写入本地文件")
    void sendOrFallback_redisFailure() {
        when(streamOps.add(any()))
                .thenThrow(new RuntimeException("Redis不可用"));

        daemon.sendOrFallback("STOCK_CONFIRM", "ORDER001",
                Map.of("productId", "1", "quantity", "2"));

        verify(streamOps).add(any());
        assertTrue(hasFailoverFiles());
    }

    @Test
    @DisplayName("OR-045 sendOrFallback - extra为null时正常处理")
    void sendOrFallback_nullExtra() {
        when(streamOps.add(any()))
                .thenThrow(new RuntimeException("Redis不可用"));

        daemon.sendOrFallback("STOCK_RESTORE", "ORDER002", null);

        assertTrue(hasFailoverFiles());
    }

    @Test
    @DisplayName("OR-045 retryFailed - 补发成功并删除文件")
    void retryFailed_success() throws IOException {
        when(streamOps.add(any()))
                .thenThrow(new RuntimeException("Redis不可用"))
                .thenReturn(RecordId.of("12345-0"));

        daemon.sendOrFallback("STOCK_CONFIRM", "ORDER001",
                Map.of("productId", "1"));

        long beforeCount = countFailoverFiles();
        assertTrue(beforeCount > 0);

        daemon.retryFailed();

        long afterCount = countFailoverFiles();
        assertEquals(0, afterCount);
    }

    @Test
    @DisplayName("OR-045 retryFailed - 补发失败保留文件")
    void retryFailed_retryFailure() throws IOException {
        when(streamOps.add(any()))
                .thenThrow(new RuntimeException("Redis仍不可用"));

        daemon.sendOrFallback("STOCK_CONFIRM", "ORDER001",
                Map.of("productId", "1"));

        long beforeCount = countFailoverFiles();
        assertTrue(beforeCount > 0);

        daemon.retryFailed();

        long afterCount = countFailoverFiles();
        assertEquals(beforeCount, afterCount);
    }

    // ==================== 补充覆盖 (目录不存在、JSON 损坏、空目录) ====================

    @Test
    @DisplayName("OR-FF-01 retryFailed - 目录为空时直接返回不抛异常")
    void retryFailed_emptyDir() throws IOException {
        assertDoesNotThrow(() -> daemon.retryFailed());
        verifyNoInteractions(streamOps);
    }

    @Test
    @DisplayName("OR-FF-02 retryFailed - 目录中有非法 JSON 文件时跳过该文件")
    void retryFailed_invalidJsonFile() throws IOException {
        Path badFile = TEST_FALLBACK_DIR.resolve("failover-bad-ORDER999.txt");
        Files.writeString(badFile, "not a json {{{");

        assertDoesNotThrow(() -> daemon.retryFailed());

        assertTrue(Files.exists(badFile), "非法 JSON 文件应保留（不会被处理）");
    }

    @Test
    @DisplayName("OR-FF-03 sendOrFallback - extra 为空 Map 时正常序列化（不写入任何额外字段）")
    void sendOrFallback_emptyExtra() {
        lenient().when(streamOps.add(any())).thenReturn(RecordId.of("12345-0"));

        daemon.sendOrFallback("STOCK_CONFIRM", "ORDER001", Map.of());

        verify(streamOps).add(any());
    }

    @Test
    @DisplayName("OR-FF-04 init - 创建目录幂等（目录已存在时不抛异常）")
    void init_idempotent() throws IOException {
        assertDoesNotThrow(() -> daemon.init());
        assertTrue(Files.exists(TEST_FALLBACK_DIR));
        assertDoesNotThrow(() -> daemon.init());
    }

    @Test
    @DisplayName("OR-FF-05 retryFailed - 多个文件全部成功补发后全部删除")
    void retryFailed_multipleSuccess() throws Exception {
        when(streamOps.add(any()))
                .thenThrow(new RuntimeException("Redis不可用"))   // sendOrFallback 1
                .thenThrow(new RuntimeException("Redis不可用"))   // sendOrFallback 2
                .thenThrow(new RuntimeException("Redis不可用"))   // sendOrFallback 3
                .thenReturn(RecordId.of("12345-0"))               // retry file 1
                .thenReturn(RecordId.of("12345-1"))               // retry file 2
                .thenReturn(RecordId.of("12345-2"));              // retry file 3

        daemon.sendOrFallback("STOCK_CONFIRM", "ORDER001", Map.of());
        Thread.sleep(5);
        daemon.sendOrFallback("STOCK_CONFIRM", "ORDER002", Map.of());
        Thread.sleep(5);
        daemon.sendOrFallback("STOCK_CONFIRM", "ORDER003", Map.of());

        long before = countFailoverFiles();
        assertTrue(before >= 3, "应当写入 3 个兜底文件，实际 " + before);

        daemon.retryFailed();

        assertEquals(0, countFailoverFiles());
    }

    private boolean hasFailoverFiles() {
        try (var files = Files.list(TEST_FALLBACK_DIR)) {
            return files.anyMatch(p -> p.getFileName().toString().startsWith("failover-"));
        } catch (IOException e) {
            return false;
        }
    }

    private long countFailoverFiles() throws IOException {
        try (var files = Files.list(TEST_FALLBACK_DIR)) {
            return files.filter(p -> p.getFileName().toString().startsWith("failover-")).count();
        }
    }
}
