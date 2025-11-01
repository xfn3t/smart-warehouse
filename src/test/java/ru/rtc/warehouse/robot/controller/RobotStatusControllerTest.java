package ru.rtc.warehouse.robot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.rtc.warehouse.config.RobotProperties;
import ru.rtc.warehouse.robot.service.RobotTelemetryService;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
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
    private RobotTelemetryService telemetryService;

    @MockBean
    private RobotProperties robotProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser(username = "RB-001", roles = {"ROBOT"})
    void whenValidStatus_thenPublishesAndReturnsOk() throws Exception {
        // given
        var payload = new java.util.HashMap<String, Object>();
        payload.put("robotId", "RB-001");
        payload.put("timestamp", Instant.now().toString());
        payload.put("status", "CONNECTED");
        payload.put("batteryLevel", 90.0);
        payload.put("lastDataSent", null);

        String json = objectMapper.writeValueAsString(payload);

        // when & then
        mockMvc.perform(post("/api/robots/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("received"));

        // Проверяем, что сервис был вызван
        verify(telemetryService, times(1)).publishStatus(any());
    }

    @Test
    @WithMockUser(username = "RB-002", roles = {"ROBOT"})
    void whenRedisThrows_thenReturns500() throws Exception {
        // given
        var payload = new java.util.HashMap<String, Object>();
        payload.put("robotId", "RB-002");
        payload.put("timestamp", Instant.now().toString());
        payload.put("status", "RECONNECTING");

        String json = objectMapper.writeValueAsString(payload);

        // Настраиваем мок сервиса на выброс исключения
        doThrow(new RuntimeException("redis down")).when(telemetryService).publishStatus(any());

        // when & then
        mockMvc.perform(post("/api/robots/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(username = "RB-003", roles = {"ROBOT"})
    void whenInvalidPayload_thenReturns400() throws Exception {
        // given
        String json = "{\"robotId\":\"\",\"status\":\"\"}";

        // when & then
        mockMvc.perform(post("/api/robots/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        // Проверяем, что сервис НЕ был вызван при невалидных данных
        verify(telemetryService, never()).publishStatus(any());
    }
}