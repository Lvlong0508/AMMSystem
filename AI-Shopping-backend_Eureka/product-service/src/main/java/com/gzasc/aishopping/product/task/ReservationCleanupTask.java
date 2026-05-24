package com.gzasc.aishopping.product.task;

import com.gzasc.aishopping.product.service.ProductReservationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(ReservationCleanupTask.class);

    private final ProductReservationService reservationService;

    @Scheduled(fixedRate = 120000)
    public void releaseOrphanedReservations() {
        try {
            reservationService.releaseExpiredReservations();
        } catch (Exception e) {
            log.warn("清理孤儿预占失败", e);
        }
    }
}
