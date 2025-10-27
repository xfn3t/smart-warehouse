package ru.rtc.warehouse.robot.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.config.RobotProperties;
import ru.rtc.warehouse.robot.controller.dto.request.RobotStatusRequest;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/robots")
public class RobotStatusController {

    private final StringRedisTemplate redisTemplate;
    private final RobotProperties robotProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/status")
    public ResponseEntity<?> acceptRobotStatus(@Validated @RequestBody RobotStatusRequest req,
                                               @RequestHeader(value = "Authorization", required = false) String auth) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "robot_status");
            Map<String, Object> data = new HashMap<>();
            data.put("robot_id", req.getRobotId());
            data.put("status", req.getStatus());
            data.put("timestamp", req.getTimestamp() != null ? req.getTimestamp().toString() : null);
            data.put("battery_level", req.getBatteryLevel());
            data.put("last_data_sent", req.getLastDataSent() != null ? req.getLastDataSent().toString() : null);
            payload.put("data", data);

            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.convertAndSend(robotProperties.getRedisChannel(), json);

            return ResponseEntity.ok(Map.of("status", "received"));
        } catch (Exception e) {
            log.error("Failed to publish robot status", e);
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}

