package ru.rtc.warehouse.location.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.location.dto.LocationMetricsDTO;
import ru.rtc.warehouse.location.service.LocationMetricsService;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Service
@RequiredArgsConstructor
public class LocationMetricsServiceImpl implements LocationMetricsService {

    private final InventoryHistoryRepository historyRepo;

    // thresholds (minutes)
    private final long recentThresholdMin = 15;
    private final long mediumThresholdMin = 120;

    @Override
    public LocationMetricsDTO computeFor(
        Warehouse warehouse,
        Integer zone,
        Integer row,
        Integer shelf
    ) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // last record
        Optional<InventoryHistory> lastOpt =
            historyRepo.findFirstByZoneAndRowAndShelfAndWarehouseOrderByScannedAtDesc(
                zone,
                row,
                shelf,
                warehouse
            );

        if (lastOpt.isEmpty()) {
            return LocationMetricsDTO.builder()
                .warehouseCode(warehouse.getCode())
                .zone(zone)
                .row(row)
                .shelf(shelf)
                .lastScannedAt(null)
                .scansCount24h(0)
                .avgIntervalMinutes(null)
                .minutesSinceLastScan(null)
                .status("OLD")
                .build();
        }

        InventoryHistory last = lastOpt.get();
        LocalDateTime lastAt = last.getScannedAt();
        long minutesSince = Duration.between(lastAt, now).toMinutes();

        // scans count in 24h
        LocalDateTime since24 = now.minusHours(24);
        long scans24 =
            historyRepo.countByZoneAndRowAndShelfAndWarehouseAndScannedAtAfter(
                zone,
                row,
                shelf,
                warehouse,
                since24
            );

        // last N scans to compute avg interval (use N=5)
        int N = 5;
        List<InventoryHistory> lastN =
            historyRepo.findByZoneAndRowAndShelfAndWarehouseOrderByScannedAtDesc(
                zone,
                row,
                shelf,
                warehouse,
                PageRequest.of(0, N)
            );
        Double avgInterval = null;
        if (lastN.size() >= 2) {
            long totalMinutes = 0;
            for (int i = 0; i < lastN.size() - 1; i++) {
                LocalDateTime a = lastN.get(i).getScannedAt();
                LocalDateTime b = lastN.get(i + 1).getScannedAt();
                totalMinutes += Math.abs(Duration.between(a, b).toMinutes());
            }
            avgInterval = totalMinutes / (double) (lastN.size() - 1);
        }

        String status = "OLD";
        if (minutesSince <= recentThresholdMin) {
            status = "RECENT";
        } else if (minutesSince <= mediumThresholdMin) {
            status = "MEDIUM";
        }

        return LocationMetricsDTO.builder()
            .warehouseCode(warehouse.getCode())
            .zone(zone)
            .row(row)
            .shelf(shelf)
            .lastScannedAt(lastAt)
            .scansCount24h((int) scans24)
            .avgIntervalMinutes(avgInterval)
            .minutesSinceLastScan(minutesSince)
            .status(status)
            .build();
    }
}
