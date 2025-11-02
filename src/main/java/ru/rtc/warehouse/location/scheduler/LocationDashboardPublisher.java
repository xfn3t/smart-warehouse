package ru.rtc.warehouse.location.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.location.repository.LocationRepository;
import ru.rtc.warehouse.location.service.LocationMetricsService;
import ru.rtc.warehouse.location.service.publisher.LocationTelemetryPublisher;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocationDashboardPublisher {

    private final LocationRepository locationRepository;
    private final LocationMetricsService metricsService;
    private final LocationTelemetryPublisher telemetryPublisher;
    private final WarehouseEntityService warehouseEntityService;

    @Scheduled(fixedRateString = "${warehouse.location.heartbeat-millis:600000}")
    @Transactional(readOnly = true)
    public void publishAll() {
        List<Location> all = locationRepository.findAll();
        for (Location loc : all) {
            try {
                var metrics = metricsService.computeFor(loc);
                telemetryPublisher.publish(metrics);
            } catch (Exception e) {
                log.warn("Failed to publish location {}-{}-{}: {}", loc.getZone(), loc.getRow(), loc.getShelf(), e.getMessage());
            }
        }
    }
}

