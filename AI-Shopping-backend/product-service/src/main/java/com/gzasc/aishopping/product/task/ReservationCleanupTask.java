package com.gzasc.aishopping.product.task;

import com.gzasc.aishopping.product.service.ProductReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
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

