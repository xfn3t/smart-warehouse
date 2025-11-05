package ru.rtc.warehouse.robot.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.config.RobotProperties;
import ru.rtc.warehouse.robot.dto.RobotUpdateDTO;
import ru.rtc.warehouse.robot.dto.RobotUpdateDataDTO;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.service.RobotEntityService;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardPublisher { 

    private final RobotEntityService robotService;
    private final StringRedisTemplate redisTemplate;
    private final RobotProperties robotProperties;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRateString = "${warehouse.robot.heartbeat-millis}")
    @Transactional(readOnly = true)
    public void publishHeartbeat() {
        try {
            List<Robot> robots = robotService.findAllWithWarehouseAndLocation();
            for (Robot robot : robots) {
                try {
                    if (robot.isDeleted()) continue;
                    if (robot.getWarehouse() == null) {
                        log.warn("Skipping robot {} without warehouse", robot.getCode());
                        continue;
                    }
                    publishForRobot(robot);
                } catch (Exception e) {
                    log.warn("Failed to publish heartbeat for robot {}: {}", robot.getCode(), e.getMessage());
                }
            }
        } catch (Exception ex) {
            log.error("DashboardPublisher failed: {}", ex.getMessage());
            throw new RuntimeException("DashboardPublisher failed: ", ex);
        }
    }

    private void publishForRobot(Robot robot) throws Exception {
        String redisKey = String.format(robotProperties.getRecentScansKeyTemplate(), robot.getCode());

        List<String> raw = redisTemplate.opsForList().range(redisKey, -robotProperties.getRecentScansLimit(), -1);
        List<Object> recentScans = new ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            for (String s : raw) {
                Map<String,Object> m = objectMapper.readValue(s, new TypeReference<Map<String,Object>>() {});
                recentScans.add(m);
            }
            Collections.reverse(recentScans);
        }

        RobotUpdateDataDTO data = new RobotUpdateDataDTO(
                robot.getCode(),
                robot.getBatteryLevel(),
                robot.getLocation() != null ? robot.getLocation().getZone() : null,
                robot.getLocation() != null ? robot.getLocation().getRow() : null,
                robot.getLocation() != null ? robot.getLocation().getShelf() : null,
                null,
                Objects.toString(robot.getLastUpdate(), ""),
                recentScans
        );

        RobotUpdateDTO payload = new RobotUpdateDTO("robot_update", data);

        String json = objectMapper.writeValueAsString(payload);
        redisTemplate.convertAndSend(robotProperties.getRedisChannel(), json);
    }
}
