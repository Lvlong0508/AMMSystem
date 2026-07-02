package com.gzasc.aishopping.product.task;

import com.gzasc.aishopping.product.service.ProductReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 商品预占库存超时取消定时任务
 * 每2分钟扫描超时预占库存的孤儿预占记录，逐个调用 releaseExpiredReservations 自动取消
 * 通过配置 product.task.timeout.enabled 控制开关（默认关闭）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "product.task.timeout.enabled", havingValue = "False", matchIfMissing = true)
public class ReservationCleanupTask {

    private final ProductReservationService reservationService;

    @Value("${app.scheduler.reservation.enabled:true}")
    private boolean reservationCleanupEnabled;

    @Scheduled(fixedRateString = "${app.scheduler.reservation.fixed-rate:120000}")
    public void releaseOrphanedReservations() {
        if (!reservationCleanupEnabled) {
            return;
        }
        try {
            reservationService.releaseExpiredReservations();
        } catch (Exception e) {
            log.warn("清理孤儿预占失败", e);
        }
    }
}

