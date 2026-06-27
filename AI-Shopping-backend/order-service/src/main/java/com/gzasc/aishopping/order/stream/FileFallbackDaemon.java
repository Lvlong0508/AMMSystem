package com.gzasc.aishopping.order.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class FileFallbackDaemon {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisStreamConfig redisStreamConfig;
    private final ObjectMapper objectMapper;

    @Value("${order.fallback.dir:data/failover}")
    private String fallbackDirPath;

    private Path fallbackDir;

    @PostConstruct
    public void init() throws IOException {
        this.fallbackDir = Paths.get(fallbackDirPath).toAbsolutePath();
        Files.createDirectories(fallbackDir);
        retryFailed();
    }

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

    @Scheduled(fixedRate = 60000)
    public void retryFailed() throws IOException {
        List<Path> files;
        try (var stream = Files.list(fallbackDir)) {
            files = stream.filter(p -> p.getFileName().toString().startsWith("failover-")).toList();
        }
        for (Path file : files) {
            try {
                String content = Files.readString(file);
                @SuppressWarnings("unchecked")
                Map<String, String> msg = objectMapper.readValue(content, Map.class);
                redisTemplate.opsForStream().add(
                        StreamRecords.newRecord().ofMap(msg).withStreamKey(redisStreamConfig.getStreamKey()));
                Files.delete(file);
                log.info("补发成功, 文件已清理, file={}", file);
            } catch (Exception e) {
                log.warn("补发失败, 文件保留等待下次重试, file={}", file);
            }
        }
    }
}
