package ru.rtc.warehouse.robot.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.rtc.warehouse.config.RobotProperties;
import ru.rtc.warehouse.robot.controller.dto.request.RobotStatusRequest;
import ru.rtc.warehouse.robot.service.RobotTelemetryService;

@Service
@RequiredArgsConstructor
@Slf4j
public class RobotTelemetryServiceImpl implements RobotTelemetryService {

    private final StringRedisTemplate redisTemplate;
    private final RobotProperties robotProperties;
    private final ObjectMapper objectMapper;

    public void publishStatus(RobotStatusRequest req) throws JsonProcessingException {

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

    }
    
}
