package com.gzasc.aishopping.order.stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final FileFallbackDaemon fileFallbackDaemon;

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
