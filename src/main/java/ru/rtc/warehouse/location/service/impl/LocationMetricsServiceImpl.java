package ru.rtc.warehouse.location.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.location.dto.LocationMetricsDTO;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.location.model.LocationStatus;
import ru.rtc.warehouse.location.service.LocationMetricsService;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Service
@RequiredArgsConstructor
public class LocationMetricsServiceImpl implements LocationMetricsService {

    private final InventoryHistoryRepository historyRepo;

    // thresholds (minutes) - вынести в properties
    private final long recentThresholdMin = 15;
    private final long mediumThresholdMin = 120;

    public LocationMetricsDTO computeFor(Location loc) {
        Warehouse wh = loc.getWarehouse();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // last record
        Optional<InventoryHistory> lastOpt = historyRepo.findFirstByLocationAndWarehouseOrderByScannedAtDesc(loc, wh);
        if (lastOpt.isEmpty()) {
            // если вовсе нет данных — статус OLD и нулевые/пустые метрики
            return LocationMetricsDTO.builder()
                    .warehouseCode(wh.getCode())
                    .zone(loc.getZone())
                    .row(loc.getRow())
                    .shelf(loc.getShelf())
                    .lastScannedAt(null)
                    .scansCount24h(0)
                    .avgIntervalMinutes(null)
                    .minutesSinceLastScan(null)
                    .status(LocationStatus.LocationStatusCode.OLD.name())
                    .build();
        }

        InventoryHistory last = lastOpt.get();
        LocalDateTime lastAt = last.getScannedAt();
        long minutesSince = Duration.between(lastAt, now).toMinutes();

        // scans count in 24h
        LocalDateTime since24 = now.minusHours(24);
        long scans24 = historyRepo.countByLocationAndWarehouseAndScannedAtAfter(loc, wh, since24);

        // last N scans to compute avg interval (use N=5)
        int N = 5;
        List<InventoryHistory> lastN = historyRepo.findByLocationAndWarehouseOrderByScannedAtDesc(loc, wh, PageRequest.of(0, N));
        Double avgInterval = null;
        if (lastN.size() >= 2) {
            // compute avg diff between consecutive scans in minutes
            long totalMinutes = 0;
            for (int i = 0; i < lastN.size() - 1; i++) {
                LocalDateTime a = lastN.get(i).getScannedAt();
                LocalDateTime b = lastN.get(i+1).getScannedAt();
                totalMinutes += Math.abs(Duration.between(a, b).toMinutes());
            }
            avgInterval = totalMinutes / (double) (lastN.size() - 1);
        }

        String status = LocationStatus.LocationStatusCode.OLD.name();
        if (minutesSince <= recentThresholdMin) status = LocationStatus.LocationStatusCode.RECENT.name();
        else if (minutesSince <= mediumThresholdMin) status = LocationStatus.LocationStatusCode.MEDIUM.name();

        return LocationMetricsDTO.builder()
                .warehouseCode(wh.getCode())
                .zone(loc.getZone())
                .row(loc.getRow())
                .shelf(loc.getShelf())
                .lastScannedAt(lastAt)
                .scansCount24h((int) scans24)
                .avgIntervalMinutes(avgInterval)
                .minutesSinceLastScan(minutesSince)
                .status(status)
                .build();
    }
}
