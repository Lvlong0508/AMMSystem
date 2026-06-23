package com.gzasc.aishopping.order.concurrency;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 下单接口并发限流配置。
 *
 * 对应 application.yml:
 *   order:
 *     concurrency:
 *       max-permits: 3
 *       wait-timeout-ms: 5000
 *       fair: true
 *
 * 通过 OrderServiceApplication 上的
 * @EnableConfigurationProperties(OrderConcurrencyProperties.class) 启用绑定,
 * 不在本类上加 @Component,避免重复注册。
 */
@Data
@ConfigurationProperties(prefix = "order.concurrency")
public class OrderConcurrencyProperties {

    /** 允许同时执行的下单请求数,默认 3 */
    private int maxPermits = 3;

    /** 排队等待许可的最长时间(毫秒),超过即抛 OrderException,默认 5000 */
    private long waitTimeoutMs = 5000L;

    /**
     * 是否使用公平模式 (FIFO)。
     * true: 等待最久的线程优先拿到许可,避免饥饿;吞吐略低。
     * false: 非公平,可能"插队",吞吐略高,有饥饿风险。
     * 默认 true 便于学习公平/非公平差异。
     */
    private boolean fair = true;
}
