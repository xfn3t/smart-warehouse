package ru.rtc.warehouse.location.service.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import ru.rtc.warehouse.config.RobotProperties;
import ru.rtc.warehouse.location.dto.LocationMetricsDTO;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LocationTelemetryPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RobotProperties robotProperties;

    // TTL для кеша — настроить в свойствах при желании
    private final Duration CACHE_TTL = Duration.ofHours(6);

    public void publish(LocationMetricsDTO dto) {
        try {
            Map<String,Object> payload = Map.of("type", "location_update", "data", dto);
            String json = objectMapper.writeValueAsString(payload);
            // publish to redis channel (existing channel)
            redisTemplate.convertAndSend(robotProperties.getRedisChannel(), json);

            // write per-location cache for REST snapshot / fast read
            String key = buildCacheKey(dto.getWarehouseCode(), dto.getZone(), dto.getRow(), dto.getShelf());
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(dto), CACHE_TTL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String buildCacheKey(String whCode, Integer zone, Integer row, Integer shelf) {
        return String.format("warehouse:%s:location:%d:%d:%d:metrics", whCode, zone, row, shelf);
    }

    // optionally: helper to build aggregated snapshot key
    public static String snapshotKey(String whCode) {
        return String.format("warehouse:%s:locations:snapshot", whCode);
    }
}
