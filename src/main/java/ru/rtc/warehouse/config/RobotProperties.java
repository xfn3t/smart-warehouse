package ru.rtc.warehouse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "warehouse.robot")
public class RobotProperties {
    private String recentScansKeyTemplate = "robot:%s:recent_scans";
    private String redisChannel = "ws:robot_updates";
    private String wsGlobalTopic = "/topic/dashboard";
    private String wsRobotTopicPrefix = "/topic/robot";
    private int recentScansLimit = 5;
    private long heartbeatMillis = 5000;
    private long recentScansTtlDays = 7L;
}
