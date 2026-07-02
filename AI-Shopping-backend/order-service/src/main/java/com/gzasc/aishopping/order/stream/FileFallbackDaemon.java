package com.gzasc.aishopping.order.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Redis 不可用时的本地文件兜底发送守护任务。
 * 包含三级保障：
 * 1. 发送失败时写入本地文件作为兜底
 * 2. 定时任务从 Nacos 配置开关控制，开启后对每个文件做 while 循环重试
 * 3. 配置开关热更新，无需重启即可启停定时补发任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
@RefreshScope
public class FileFallbackDaemon {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisStreamConfig redisStreamConfig;
    private final ObjectMapper objectMapper;

    @Value("${order.fallback.dir:data/failover}")
    private String fallbackDirPath;

    /** 定时补发任务开关，默认关闭，从 Nacos 配置 order.fallback.retry-enabled 热更新 */
    @Value("${order.fallback.retry-enabled:false}")
    private boolean retryEnabled;

    private Path fallbackDir;

    /** 每次 while 重试的间隔毫秒数（可修改，测试时可设小值避免等待） */
    private long retryIntervalMs = 5000;

    /** 单个文件最大重试次数，防止极端情况下死循环 */
    private static final int RETRY_MAX_ATTEMPTS = 3;

    /**
     * 初始化兜底目录，确保目录存在，并在启动时立即尝试补发已有文件。
     */
    @PostConstruct
    public void init() throws IOException {
        this.fallbackDir = Paths.get(fallbackDirPath).toAbsolutePath();
        Files.createDirectories(fallbackDir);
        retryFailed();
    }

    /**
     * 发送消息到 Redis Stream，发送失败时自动切换到本地文件兜底。
     * 这是外部调用的统一入口，调用方无需关心 Redis 是否可用。
     */
    public void sendOrFallback(String eventType, String orderId, Map<String, String> extra) {
        try {
            Map<String, String> msg = new LinkedHashMap<>();
            msg.put("eventType", eventType);
            msg.put("orderId", orderId);
            if (extra != null) {
                msg.putAll(extra);
            }
            redisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .ofMap(msg)
                            .withStreamKey(redisStreamConfig.getStreamKey())
            );
            log.debug("消息发送成功 eventType={}, orderId={}", eventType, orderId);
        } catch (Exception e) {
            log.warn("Redis不可用，写入本地文件兜底 eventType={}, orderId={}", eventType, orderId);
            writeFallback(eventType, orderId, extra);
        }
    }

    /**
     * 将消息序列化为 JSON，写入以 "failover-" 前缀命名的本地文件，
     * 文件名带时间戳以防重名。
     */
    private void writeFallback(String eventType, String orderId, Map<String, String> extra) {
        try {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("eventType", eventType);
            data.put("orderId", orderId);
            if (extra != null) {
                data.putAll(extra);
            }
            String json = objectMapper.writeValueAsString(data);
            String filename = "failover-" + System.currentTimeMillis() + "-" + orderId + ".txt";
            Files.writeString(fallbackDir.resolve(filename), json);
        } catch (IOException e) {
            log.error("写入本地兜底文件失败 eventType={}, orderId={}", eventType, orderId, e);
        }
    }

    /**
     * 定时任务，每分钟触发一次。
     * 受 Nacos 配置 order.fallback.retry-enabled 控制（默认关闭），
     * 开启后扫描兜底目录并对每个文件做 while 循环重试直到发送成功或开关关闭。
     */
    @Scheduled(fixedRate = 60000)
    public void retryFailed() {
        if (!retryEnabled) {
            log.debug("文件补发定时任务已关闭，可设置 order.fallback.retry-enabled=true 开启");
            return;
        }
        log.info("文件补发定时任务启动，开始扫描兜底目录");
        retryLoop();
    }

    /**
     * 单轮补发：扫描兜底文件并对每个文件做 while 循环重试。
     * 外层由 @Scheduled 每分钟触发，内层 retryFileWithLoop 负责单文件持续重试。
     */
    private void retryLoop() {
        List<Path> files = scanFailoverFiles();
        if (files.isEmpty()) {
            log.debug("无待补发文件，等待下次定时触发");
            return;
        }
        log.info("发现 {} 个待补发文件", files.size());
        for (Path file : files) {
            if (!retryEnabled) {
                log.info("补发开关已关闭，停止重试");
                return;
            }
            retryFileWithLoop(file);
        }
    }

    /**
     * 对单个文件做 while 循环重试，直到发送成功、开关关闭或达到最大重试次数。
     */
    private void retryFileWithLoop(Path file) {
        int retryCount = 0;
        while (retryEnabled) {
            retryCount++;
            try {
                String content = Files.readString(file);
                @SuppressWarnings("unchecked")
                Map<String, String> msg = objectMapper.readValue(content, Map.class);
                redisTemplate.opsForStream().add(
                        StreamRecords.newRecord().ofMap(msg).withStreamKey(redisStreamConfig.getStreamKey()));
                Files.delete(file);
                log.info("补发成功, 文件已清理, file={}", file);
                return;
            } catch (Exception e) {
                if (retryCount >= RETRY_MAX_ATTEMPTS) {
                    log.error("重试{}次均失败, 文件保留等待下次定时触发, file={}", RETRY_MAX_ATTEMPTS, file);
                    return;
                }
                log.warn("补发失败(第{}次), {}ms后重试, file={}", retryCount, retryIntervalMs, file, e);
                sleepQuietly(retryIntervalMs);
            }
        }
    }

    /**
     * 扫描兜底目录，返回所有 failover- 开头的文件列表。
     */
    private List<Path> scanFailoverFiles() {
        try (Stream<Path> stream = Files.list(fallbackDir)) {
            return stream.filter(p -> p.getFileName().toString().startsWith("failover-")).toList();
        } catch (IOException e) {
            log.error("扫描兜底目录失败", e);
            return List.of();
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
