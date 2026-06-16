package ru.rtc.warehouse.location.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.common.aspect.RequiresOwnership;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.location.dto.LocationMetricsDTO;
import ru.rtc.warehouse.location.service.LocationMetricsService;
import ru.rtc.warehouse.location.service.publisher.LocationTelemetryPublisher;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/warehouses")
public class LocationController {

    private final WarehouseEntityService warehouseService;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final LocationMetricsService metricsService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @GetMapping("/{warehouseCode}/locations")
    @RequiresOwnership(
        codeParam = "warehouseCode",
        entityType = RequiresOwnership.EntityType.WAREHOUSE
    )
    public ResponseEntity<List<LocationMetricsDTO>> list(
        @PathVariable String warehouseCode
    ) {
        String snapshotKey = LocationTelemetryPublisher.snapshotKey(
            warehouseCode
        );
        try {
            String cached = redisTemplate.opsForValue().get(snapshotKey);
            if (cached != null) {
                List<LocationMetricsDTO> list = objectMapper.readValue(
                    cached,
                    objectMapper
                        .getTypeFactory()
                        .constructCollectionType(
                            List.class,
                            LocationMetricsDTO.class
                        )
                );
                return ResponseEntity.ok(list);
            }
        } catch (Exception e) {
            // ignore cache miss
        }

        Warehouse wh = warehouseService.findByCode(warehouseCode);
        List<Object[]> distinctLocations =
            inventoryHistoryRepository.findDistinctZoneRowShelfByWarehouse(wh);
        List<LocationMetricsDTO> dtos = distinctLocations
            .stream()
            .map(row -> {
                Integer zone = (Integer) row[0];
                Integer r = (Integer) row[1];
                Integer shelf = (Integer) row[2];
                return metricsService.computeFor(wh, zone, r, shelf);
            })
            .collect(Collectors.toList());

        try {
            redisTemplate
                .opsForValue()
                .set(
                    snapshotKey,
                    objectMapper.writeValueAsString(dtos),
                    Duration.ofMinutes(30)
                );
        } catch (Exception ignore) {}

        return ResponseEntity.ok(dtos);
    }
}
