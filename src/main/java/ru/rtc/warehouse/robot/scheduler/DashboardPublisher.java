package ru.rtc.warehouse.robot.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.config.RobotProperties;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.repository.RobotRepository;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardPublisher {

    private final RobotRepository robotRepository;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final RobotProperties robotProperties;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper; 

    private final DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Scheduled(fixedRateString = "${warehouse.robot.heartbeat-millis}")
    @Transactional(readOnly = true)
    public void publishHeartbeat() {
        try {
            List<Robot> robots = robotRepository.findAll();
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
        List<Map<String,Object>> recentScans = new ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            for (String s : raw) {
                Map<String,Object> m = objectMapper.readValue(s, new TypeReference<Map<String,Object>>() {});
                recentScans.add(m);
            }
            Collections.reverse(recentScans);
        }

        Map<String,Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "robot_update");
        Map<String,Object> data = new HashMap<>();
        data.put("robot_id", robot.getCode());
        data.put("battery_level", robot.getBatteryLevel());
        data.put("zone", robot.getCurrentZone());
        data.put("row", robot.getCurrentRow());
        data.put("shelf", robot.getCurrentShelf());
        data.put("next_checkpoint", null);
        data.put("timestamp", Objects.toString(robot.getLastUpdate(), ""));
        data.put("recent_scans", recentScans);

        wsPayload.put("data", data);

        String json = objectMapper.writeValueAsString(wsPayload);
        redisTemplate.convertAndSend(robotProperties.getRedisChannel(), json);
    }
}
