package ru.rtc.warehouse.robot.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ru.rtc.warehouse.config.RobotProperties;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.robot.repository.RobotRepository;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisMessageSubscriberTest {

    private SimpMessagingTemplate messagingTemplate;
    private RobotProperties robotProperties;
    private ObjectMapper objectMapper;
    private RobotRepository robotRepository;
    private StringRedisTemplate redisTemplate;
    private RedisMessageSubscriber subscriber;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        robotProperties = new RobotProperties();
        robotProperties.setWsGlobalTopic("/topic/dashboard");
        robotProperties.setWsRobotTopicPrefix("/topic/dashboard/robot");
        robotProperties.setWsWarehouseTopicPrefix("/topic/dashboard/warehouse");
        objectMapper = new ObjectMapper();
        robotRepository = mock(RobotRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);

        subscriber = new RedisMessageSubscriber(messagingTemplate, robotProperties, objectMapper, robotRepository, redisTemplate);
    }

    @Test
    void whenPayloadHasNoWarehouse_thenLookupAndCacheAndPublishPerWarehouse() throws Exception {
        String json = "{\"type\":\"robot_update\",\"data\":{\"robot_id\":\"RB-001\",\"battery_level\":80}}";
        

        Message msg = mock(Message.class);
        when(msg.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));
        when(msg.getChannel()).thenReturn("test-channel".getBytes(StandardCharsets.UTF_8));

        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("robot:RB-001:warehouse")).thenReturn(null);

        Warehouse wh = new Warehouse();
        wh.setCode("WH-01");
        Robot r = new Robot();
        r.setCode("RB-001");
        r.setWarehouse(wh);
        when(robotRepository.findByCode("RB-001")).thenReturn(Optional.of(r));


        subscriber.onMessage(msg, null);

        // Verify global published
        verify(messagingTemplate).convertAndSend(eq("/topic/dashboard"), any(Object.class));
        // Verify per-robot published
        verify(messagingTemplate).convertAndSend(eq("/topic/dashboard/robot/RB-001"), any(Object.class));
        // Verify caching set called
        verify(valueOperations).set(eq("robot:RB-001:warehouse"), eq("WH-01"), any());
        // Verify per-warehouse published
        verify(messagingTemplate).convertAndSend(eq("/topic/dashboard/warehouse/WH-01"), any(Object.class));
    }

    @Test
    void whenCacheHasWarehouse_thenNoDbLookup_andPublishPerWarehouseFromCache() throws Exception {
        String json = "{\"type\":\"robot_update\",\"data\":{\"robot_id\":\"RB-002\",\"battery_level\":50}}";
        
        Message msg = mock(Message.class);
        when(msg.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));
        when(msg.getChannel()).thenReturn("test-channel".getBytes(StandardCharsets.UTF_8));


        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("robot:RB-002:warehouse")).thenReturn("WH-02");

        subscriber.onMessage(msg, null);

        verify(messagingTemplate).convertAndSend(eq("/topic/dashboard"), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/dashboard/robot/RB-002"), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/dashboard/warehouse/WH-02"), any(Object.class));
        verify(robotRepository, never()).findByCode(any());// no DB lookup
    }
}