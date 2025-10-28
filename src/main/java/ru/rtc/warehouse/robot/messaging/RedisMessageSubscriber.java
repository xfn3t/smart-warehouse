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
import ru.rtc.warehouse.robot.repository.RobotRepository;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final RobotProperties robotProperties;
    private final ObjectMapper objectMapper;
    private final RobotRepository robotRepository;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate; 


    private static final Duration ROBOT_WAREHOUSE_TTL = Duration.ofHours(1);

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


            String warehouseCode = data.path("warehouse").asText(null);


            if ((warehouseCode == null || warehouseCode.isEmpty()) && robotId != null && !robotId.isEmpty()) {
                String cacheKey = String.format("robot:%s:warehouse", robotId);
                try {
                    String cached = redisTemplate.opsForValue().get(cacheKey);
                    if (cached != null && !cached.isEmpty()) {
                        warehouseCode = cached;
                    } else {

                        Optional.ofNullable(robotRepository.findByCode(robotId))
                                .flatMap(rOpt -> rOpt) 
                                .ifPresent(r -> {
                                    try {
                                        if (r.getWarehouse() != null && r.getWarehouse().getCode() != null) {
                                            String wc = r.getWarehouse().getCode();
                                            try {
                                                redisTemplate.opsForValue().set(cacheKey, wc, ROBOT_WAREHOUSE_TTL);
                                            } catch (Exception ex) {
                                                log.warn("Failed to cache robot->warehouse in Redis: {}", ex.getMessage());
                                            }
                                            String perWarehouseTopic = robotProperties.getWsWarehouseTopicPrefix() + "/" + wc;
                                            messagingTemplate.convertAndSend(perWarehouseTopic, root);
                                        }
                                    } catch (Exception ex) {
                                        log.warn("Failed to publish per-warehouse for robot {}: {}", robotId, ex.getMessage());
                                    }
                                });
                       
                        return;
                    }
                } catch (Exception ex) {
                    log.warn("Failed to lookup/cache robot->warehouse for robot {}: {}", robotId, ex.getMessage());
                }
            }

            if (warehouseCode != null && !warehouseCode.isEmpty()) {
                String perWarehouseTopic = robotProperties.getWsWarehouseTopicPrefix() + "/" + warehouseCode;
                messagingTemplate.convertAndSend(perWarehouseTopic, root);
            }

        } catch (Exception e) {
            log.warn("Failed to parse redis message to JSON, sending raw string.", e);
            messagingTemplate.convertAndSend(robotProperties.getWsGlobalTopic(), payload);
        }
    }
}
