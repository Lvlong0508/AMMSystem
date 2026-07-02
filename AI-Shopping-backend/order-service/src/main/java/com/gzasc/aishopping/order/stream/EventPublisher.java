package com.gzasc.aishopping.order.stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

/**
 * 事件发布器，确保消息在数据库事务提交后才发送。
 * 如果当前存在事务，则注册 afterCommit 回调延后发送；
 * 如果没有事务，则立即发送。
 * 实际发送委托给 FileFallbackDaemon，发送失败会自动走文件兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final FileFallbackDaemon fileFallbackDaemon;

    /**
     * 在事务提交后（或无事务时立即）发布事件到 Redis Stream。
     * eventType 和 orderId 为必填参数，extra 用于携带附加字段。
     */
    public void publishAfterCommit(String eventType, String orderId, Map<String, String> extra) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            fileFallbackDaemon.sendOrFallback(eventType, orderId, extra);
                        }
                    }
            );
        } else {
            fileFallbackDaemon.sendOrFallback(eventType, orderId, extra);
        }
    }
}
