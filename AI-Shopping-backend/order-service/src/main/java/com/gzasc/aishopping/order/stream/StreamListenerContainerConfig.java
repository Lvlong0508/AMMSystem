package com.gzasc.aishopping.order.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

import java.time.Duration;
import java.util.UUID;

/**
 * Stream 消息监听容器配置，负责创建和启动 StreamMessageListenerContainer，
 * 将 OrderEventConsumer 注册为消费者组的订阅者。
 */
@Configuration
@Slf4j
public class StreamListenerContainerConfig {

    private final String consumerId = "consumer-" + UUID.randomUUID();

    /**
     * 创建 Stream 监听容器 Bean，应用关闭时自动停止。
     * 使用 lastConsumed 偏移量从上次消费位置继续读取。
     */
    @Bean(destroyMethod = "stop")
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> container(
            RedisConnectionFactory cf,
            OrderEventConsumer consumer,
            RedisStreamConfig redisStreamConfig) {

        var opts = StreamMessageListenerContainerOptions
                .<String, MapRecord<String, String, String>>builder()
                .pollTimeout(Duration.ofMillis(100))
                .errorHandler(e -> log.error("Stream consumer error", e))
                .build();

        var container = StreamMessageListenerContainer.create(cf, opts);

        container.receive(
                Consumer.from(redisStreamConfig.getGroupName(), consumerId),
                StreamOffset.create(redisStreamConfig.getStreamKey(), ReadOffset.lastConsumed()),
                consumer
        );

        container.start();
        return container;
    }
}
