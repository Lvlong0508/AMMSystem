package com.gzasc.aishopping.product.task;

import com.gzasc.aishopping.product.service.ProductReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationCleanupTaskTest {

    @Mock
    private ProductReservationService reservationService;

    @InjectMocks
    private ReservationCleanupTask cleanupTask;

    @Test
    @DisplayName("PR-069 - 定时任务触发清理过期预占")
    void testReleaseOrphanedReservations() {
        cleanupTask.releaseOrphanedReservations();
        verify(reservationService).releaseExpiredReservations();
    }

    @Test
    @DisplayName("PR-069 - 清理异常不影响定时任务继续运行")
    void testReleaseOrphanedReservationsExceptionHandling() {
        doThrow(new RuntimeException("数据库连接失败")).when(reservationService).releaseExpiredReservations();

        cleanupTask.releaseOrphanedReservations();

        verify(reservationService).releaseExpiredReservations();
    }
}
