package ru.rtc.warehouse.robot.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.config.RobotProperties;
import ru.rtc.warehouse.robot.controller.dto.request.RobotStatusRequest;
import ru.rtc.warehouse.robot.service.RobotTelemetryService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/robots")
public class RobotStatusController {

    private final RobotTelemetryService telemetryService;

    @PostMapping("/status")
    @PreAuthorize("hasRole('ROBOT') and #request.robotId == authentication.name")
    public ResponseEntity<?> acceptRobotStatus(@Validated @RequestBody RobotStatusRequest request) {
        try {
            telemetryService.publishStatus(request);
            return ResponseEntity.ok(Map.of("status", "received"));
        } catch (Exception e) {
            log.error("Failed to publish robot status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}

