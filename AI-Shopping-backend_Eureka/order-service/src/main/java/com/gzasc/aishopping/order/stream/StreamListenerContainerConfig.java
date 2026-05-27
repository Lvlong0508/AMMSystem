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

@Configuration
@Slf4j
public class StreamListenerContainerConfig {

    @Bean(destroyMethod = "stop")
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> container(
            RedisConnectionFactory cf,
            OrderEventConsumer consumer) {

        var opts = StreamMessageListenerContainerOptions
                .<String, MapRecord<String, String, String>>builder()
                .pollTimeout(Duration.ofMillis(100))
                .errorHandler(e -> log.error("Stream consumer error", e))
                .build();

        var container = StreamMessageListenerContainer.create(cf, opts);

        container.receive(
                Consumer.from(RedisStreamConfig.GROUP_NAME, "consumer-1"),
                StreamOffset.create(RedisStreamConfig.STREAM_KEY, ReadOffset.lastConsumed()),
                consumer
        );

        container.start();
        return container;
    }
}
