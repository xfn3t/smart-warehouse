package ru.rtc.warehouse.robot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.rtc.warehouse.config.RobotProperties;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(
    value = RobotStatusController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class}
)
@TestPropertySource(properties = {
    "server.port=8080",
    "spring.main.allow-bean-definition-overriding=true"
})
class RobotStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private RobotProperties robotProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void whenValidStatus_thenPublishesAndReturnsOk() throws Exception {

        when(robotProperties.getRedisChannel()).thenReturn("ws:robot_updates");

        var payload = new java.util.HashMap<String, Object>();
        payload.put("robotId", "RB-001");
        payload.put("timestamp", Instant.now().toString());
        payload.put("status", "CONNECTED");
        payload.put("batteryLevel", 90.0);
        payload.put("lastDataSent", null);

        String json = objectMapper.writeValueAsString(payload);

        ValueOperations<String, String> vo = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(vo);


        mockMvc.perform(post("/api/robots/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("received"));


        verify(redisTemplate, times(1)).convertAndSend(eq("ws:robot_updates"), anyString());
    }

    @Test
    void whenRedisThrows_thenReturns500() throws Exception {
        when(robotProperties.getRedisChannel()).thenReturn("ws:robot_updates");

        var payload = new java.util.HashMap<String, Object>();
        payload.put("robotId", "RB-002");
        payload.put("timestamp", Instant.now().toString());
        payload.put("status", "RECONNECTING");

        String json = objectMapper.writeValueAsString(payload);


        doThrow(new RuntimeException("redis down")).when(redisTemplate).convertAndSend(anyString(), anyString());

        mockMvc.perform(post("/api/robots/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void whenInvalidPayload_thenReturns400() throws Exception {
        String json = "{\"robotId\":\"\",\"status\":\"\"}";

        mockMvc.perform(post("/api/robots/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
