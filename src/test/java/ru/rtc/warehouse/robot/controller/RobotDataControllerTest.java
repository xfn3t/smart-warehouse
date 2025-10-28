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
import ru.rtc.warehouse.robot.controller.dto.response.RobotDataResponse;
import ru.rtc.warehouse.robot.service.RobotDataService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    value = RobotDataController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class}
)
@TestPropertySource(properties = {
    "server.port=8080",
    "spring.main.allow-bean-definition-overriding=true"
})
class RobotDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RobotDataService robotDataService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void whenValidData_thenCallsServiceAndReturnsResponse() throws Exception {
        Map<String, Object> request = Map.of(
            "code", "RB-0001",
            "timestamp", Instant.parse("2023-10-10T10:00:00Z"),
            "location", Map.of("zone", 1, "row", 1, "shelf", 1),
            "scanResults", List.of(
                Map.of(
                    "productCode", "TEL-4567",
                    "productName", "Роутер",
                    "quantity", 10,

                    "status", Map.of("code", "OK")
                )
            ),
            "batteryLevel", 85,
            "nextCheckpoint", "1-2-3"
        );

        UUID msgId = UUID.randomUUID();
        when(robotDataService.processRobotData(any())).thenReturn(new RobotDataResponse("received", msgId));

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/robots/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("received"))
                .andExpect(jsonPath("$.messageId").exists());

        verify(robotDataService, times(1)).processRobotData(any());
    }

    @Test
    void whenInvalidData_thenReturns400() throws Exception {

        Map<String, Object> request = Map.of(
                "code", "RB-001",
                "timestamp", Instant.now(),
                "location", Map.of("zone", 1, "row", 1, "shelf", 1),
                "batteryLevel", 85,
                "nextCheckpoint", "1-2-3"
        );

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/robots/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(robotDataService, never()).processRobotData(any());
    }
}
