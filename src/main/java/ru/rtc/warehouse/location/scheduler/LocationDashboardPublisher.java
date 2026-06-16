package ru.rtc.warehouse.location.scheduler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.location.service.LocationMetricsService;
import ru.rtc.warehouse.location.service.publisher.LocationTelemetryPublisher;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocationDashboardPublisher {

    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final LocationMetricsService metricsService;
    private final LocationTelemetryPublisher telemetryPublisher;

    @Scheduled(
        fixedRateString = "${warehouse.location.heartbeat-millis:600000}"
    )
    @Transactional(readOnly = true)
    public void publishAll() {
        List<Object[]> distinctLocations =
            inventoryHistoryRepository.findDistinctZoneRowShelfWithWarehouse();
        for (Object[] row : distinctLocations) {
            try {
                Integer zone = (Integer) row[0];
                Integer r = (Integer) row[1];
                Integer shelf = (Integer) row[2];
                Warehouse warehouse = (Warehouse) row[3];
                var metrics = metricsService.computeFor(
                    warehouse,
                    zone,
                    r,
                    shelf
                );
                telemetryPublisher.publish(metrics);
            } catch (Exception e) {
                log.warn(
                    "Failed to publish location {}-{}-{}: {}",
                    row[0],
                    row[1],
                    row[2],
                    e.getMessage()
                );
            }
        }
    }
}
