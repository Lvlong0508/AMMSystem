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
        when(redisTemplate.opsForStream()).thenReturn(streamOps);
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
