package com.gzasc.aishopping.product.task;

import com.gzasc.aishopping.product.service.ProductReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCleanupTask {

    private final ProductReservationService reservationService;

    @Value("${app.scheduler.reservation.enabled:true}")
    private boolean reservationCleanupEnabled;

    @Value("${app.scheduler.reservation.config-path:}")
    private String reservationSchedulerConfigPath;

    @Scheduled(fixedRateString = "${app.scheduler.reservation.fixed-rate:120000}")
    public void releaseOrphanedReservations() {
        if (!isReservationCleanupEnabled()) {
            return;
        }
        try {
            reservationService.releaseExpiredReservations();
        } catch (Exception e) {
            log.warn("清理孤儿预占失败", e);
        }
    }

    private boolean isReservationCleanupEnabled() {
        Path configPath = resolveReservationSchedulerConfigPath();
        if (configPath == null || !Files.isRegularFile(configPath)) {
            return reservationCleanupEnabled;
        }

        try {
            YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
            yaml.setResources(new FileSystemResource(configPath));
            Properties properties = yaml.getObject();
            String enabled = properties != null
                ? properties.getProperty("app.scheduler.reservation.enabled")
                : null;
            return enabled == null ? reservationCleanupEnabled : Boolean.parseBoolean(enabled);
        } catch (Exception e) {
            log.warn("读取预占清理定时器配置失败，使用启动时配置 enabled={}", reservationCleanupEnabled, e);
            return reservationCleanupEnabled;
        }
    }

    private Path resolveReservationSchedulerConfigPath() {
        if (reservationSchedulerConfigPath != null && !reservationSchedulerConfigPath.isBlank()) {
            return Paths.get(reservationSchedulerConfigPath).toAbsolutePath().normalize();
        }

        Path sourceConfig = Paths.get("src/main/resources/application.yml").toAbsolutePath().normalize();
        if (Files.isRegularFile(sourceConfig)) {
            return sourceConfig;
        }

        Path currentDirConfig = Paths.get("application.yml").toAbsolutePath().normalize();
        if (Files.isRegularFile(currentDirConfig)) {
            return currentDirConfig;
        }

        return null;
    }
}
