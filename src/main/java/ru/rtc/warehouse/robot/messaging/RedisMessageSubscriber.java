package ru.rtc.warehouse.robot.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.config.RobotProperties;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final RobotProperties robotProperties;
    private final ObjectMapper objectMapper; // инжект бин

    @Override
    public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            JsonNode root = objectMapper.readTree(payload);

            messagingTemplate.convertAndSend(robotProperties.getWsGlobalTopic(), root);

            JsonNode data = root.path("data");
            String robotId = data.path("robot_id").asText(null);
            if (robotId != null && !robotId.isEmpty()) {
                String perRobotTopic = robotProperties.getWsRobotTopicPrefix() + "/" + robotId;
                messagingTemplate.convertAndSend(perRobotTopic, root);
            }
        } catch (Exception e) {
            log.warn("Failed to parse redis message to JSON, sending raw string.", e);
            messagingTemplate.convertAndSend(robotProperties.getWsGlobalTopic(), payload);
        }
    }
}

