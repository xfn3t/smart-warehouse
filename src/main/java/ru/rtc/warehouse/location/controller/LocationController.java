package ru.rtc.warehouse.location.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.location.service.LocationMetricsService;
import ru.rtc.warehouse.location.service.publisher.LocationTelemetryPublisher;
import ru.rtc.warehouse.location.dto.LocationMetricsDTO;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.location.repository.LocationRepository;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/warehouses")
public class LocationController {

    private final WarehouseEntityService warehouseService;
    private final LocationRepository locationRepository;
    private final LocationMetricsService metricsService;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @GetMapping("/{code}/locations")
    public ResponseEntity<List<LocationMetricsDTO>> list(@PathVariable String code) {

        String snapshotKey = LocationTelemetryPublisher.snapshotKey(code);
        try {
            String cached = redisTemplate.opsForValue().get(snapshotKey);
            if (cached != null) {
                List<LocationMetricsDTO> list = objectMapper.readValue(cached, objectMapper.getTypeFactory().constructCollectionType(List.class, LocationMetricsDTO.class));
                return ResponseEntity.ok(list);
            }
        } catch (Exception e) {

        }


        var wh = warehouseService.findByCode(code);
        List<Location> locs = locationRepository.findByWarehouse(wh);
        List<LocationMetricsDTO> dtos = locs.stream()
                .map(metricsService::computeFor)
                .collect(Collectors.toList());


        try {
            redisTemplate.opsForValue().set(snapshotKey, objectMapper.writeValueAsString(dtos), java.time.Duration.ofMinutes(30));
        } catch (Exception ignore) {}

        return ResponseEntity.ok(dtos);
    }


}
