package ru.rtc.warehouse.config.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.config.RobotProperties;
import ru.rtc.warehouse.config.service.adapter.RobotEntityAdapter;
import ru.rtc.warehouse.robot.model.Robot;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final RobotProperties robotProperties;
    private final ObjectMapper objectMapper;
    private final RobotEntityAdapter robotEntityAdapter;
    private final StringRedisTemplate redisTemplate;

    private static final Duration ROBOT_WAREHOUSE_TTL = Duration.ofHours(1);

    @Override
    public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("Failed to parse redis message to JSON, sending raw string.", e);
            try {
                messagingTemplate.convertAndSend(robotProperties.getWsGlobalTopic(), payload);
            } catch (Exception ex) {
                log.warn("Failed to publish raw payload to global topic: {}", ex.getMessage());
            }
            return;
        }

        try {
            String type = root.path("type").asText("");
            if ("location_update".equals(type)) {
                handleLocationUpdate(root);
                // also forward to global (optional) so dashboard which listens on global sees it
                try {
                    messagingTemplate.convertAndSend(robotProperties.getWsGlobalTopic(), root);
                } catch (Exception ex) {
                    log.warn("Failed to publish location_update to global topic: {}", ex.getMessage());
                }
                return;
            }

            // generic publish
            messagingTemplate.convertAndSend(robotProperties.getWsGlobalTopic(), root);

            JsonNode data = root.path("data");
            String robotId = textOrNull(data, "robot_id", "robotId", "robot");
            if (robotId != null && !robotId.isEmpty()) {
                String perRobotTopic = buildTopic(robotProperties.getWsRobotTopicPrefix(), robotId);
                messagingTemplate.convertAndSend(perRobotTopic, root);
            }

            String warehouseCode = textOrNull(data, "warehouseCode", "warehouse_code", "warehouse");
            if ((warehouseCode == null || warehouseCode.isEmpty()) && robotId != null && !robotId.isEmpty()) {
                // try cache -> db
                String cacheKey = String.format("robot:%s:warehouse", robotId);
                try {
                    String cached = redisTemplate.opsForValue().get(cacheKey);
                    if (cached != null && !cached.isEmpty()) {
                        warehouseCode = cached;
                    } else {
                        Robot robot = robotEntityAdapter.findByCode(robotId);
                        if (robot != null) {
                            try {
                                if (robot.getWarehouse() != null && robot.getWarehouse().getCode() != null) {
                                    String wc = robot.getWarehouse().getCode();
                                    try {
                                        redisTemplate.opsForValue().set(cacheKey, wc, ROBOT_WAREHOUSE_TTL);
                                    } catch (Exception ex) {
                                        log.warn("Failed to cache robot->warehouse in Redis: {}", ex.getMessage());
                                    }
                                    String perWarehouseTopic = buildTopic(robotProperties.getWsWarehouseTopicPrefix(), wc);
                                    messagingTemplate.convertAndSend(perWarehouseTopic, root);
                                }
                            } catch (Exception ex) {
                                log.warn("Failed to publish per-warehouse for robot {}: {}", robotId, ex.getMessage());
                            }
                        }
                        return;
                    }
                } catch (Exception ex) {
                    log.warn("Failed to lookup/cache robot->warehouse for robot {}: {}", robotId, ex.getMessage());
                }
            }

            if (warehouseCode != null && !warehouseCode.isEmpty()) {
                String perWarehouseTopic = buildTopic(robotProperties.getWsWarehouseTopicPrefix(), warehouseCode);
                messagingTemplate.convertAndSend(perWarehouseTopic, root);
            }

        } catch (Exception e) {
            log.warn("Unexpected error in onMessage handler", e);
        }
    }

    private void handleLocationUpdate(JsonNode root) {
        try {
            JsonNode data = root.path("data");
            String warehouseCode = textOrNull(data, "warehouseCode", "warehouse_code", "warehouse");

            String globalLocationsTopic = robotProperties.getWsGlobalLocationsTopic();
            if (globalLocationsTopic == null || globalLocationsTopic.isEmpty()) {
                globalLocationsTopic = buildTopic(robotProperties.getWsGlobalTopic(), "locations");
            }
            messagingTemplate.convertAndSend(globalLocationsTopic, root);

            String warehouseLocationsPrefix;
            String tempWarehouseLocationsPrefix = robotProperties.getWsWarehouseLocationsTopicPrefix();
            if (tempWarehouseLocationsPrefix == null || tempWarehouseLocationsPrefix.isEmpty()) {
                warehouseLocationsPrefix = robotProperties.getWsWarehouseTopicPrefix();
            } else {
                warehouseLocationsPrefix = tempWarehouseLocationsPrefix;
            }

            if (warehouseCode != null && !warehouseCode.isEmpty()) {
                String perWarehouseTopic = buildTopic(warehouseLocationsPrefix, warehouseCode, "locations");
                messagingTemplate.convertAndSend(perWarehouseTopic, root);
                return;
            }

            String robotId = textOrNull(data, "robot_id", "robotId", "robot");
            if (robotId != null && !robotId.isEmpty()) {
                String cacheKey = String.format("robot:%s:warehouse", robotId);
                try {
                    String cached = redisTemplate.opsForValue().get(cacheKey);
                    if (cached != null && !cached.isEmpty()) {
                        String perWarehouseTopic = buildTopic(warehouseLocationsPrefix, cached, "locations");
                        messagingTemplate.convertAndSend(perWarehouseTopic, root);
                        return;
                    } else {
                        Robot robot = robotEntityAdapter.findByCode(robotId);
                        if (robot != null) {
                            try {
                                if (robot.getWarehouse() != null && robot.getWarehouse().getCode() != null) {
                                    String wc = robot.getWarehouse().getCode();
                                    try {
                                        redisTemplate.opsForValue().set(cacheKey, wc, ROBOT_WAREHOUSE_TTL);
                                    } catch (Exception ex) {
                                        log.warn("Failed to cache robot->warehouse in Redis: {}", ex.getMessage());
                                    }
                                    String perWarehouseTopic = buildTopic(warehouseLocationsPrefix, wc, "locations");
                                    messagingTemplate.convertAndSend(perWarehouseTopic, root);
                                }
                            } catch (Exception ex) {
                                log.warn("Failed to publish per-warehouse locations for robot {}: {}", robotId, ex.getMessage());
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Failed to lookup/cache robot->warehouse for robot {}: {}", robotId, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to handle location_update payload", ex);
        }
    }

    private String textOrNull(JsonNode node, String... names) {
        for (String n : names) {
            if (node.has(n)) {
                String t = node.path(n).asText(null);
                if (t != null && !t.isEmpty()) return t;
            }
        }
        return null;
    }

    // helper: join parts with single slash, avoid double slashes
    private String buildTopic(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            String s = p;
            if (sb.length() == 0) {
                sb.append(s.replaceAll("/+$", "")); // leading piece: keep its leading slash if present
            } else {
                sb.append("/");
                sb.append(s.replaceAll("^/+", "").replaceAll("/+$", ""));
            }
        }
        return sb.toString();
    }
}
