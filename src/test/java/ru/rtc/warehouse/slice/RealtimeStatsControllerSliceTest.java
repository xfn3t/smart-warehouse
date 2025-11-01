package ru.rtc.warehouse.slice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.rtc.warehouse.dashboard.controller.RealtimeStatsController;
import ru.rtc.warehouse.dashboard.dto.RealtimeStatsDTO;
import ru.rtc.warehouse.dashboard.service.RealtimeStatsService;
import ru.rtc.warehouse.exception.GlobalExceptionHandler;
import ru.rtc.warehouse.exception.NotFoundException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RealtimeStatsController.class)
@AutoConfigureMockMvc(addFilters = false) // Security-фильтры нам здесь не нужны
@Import(GlobalExceptionHandler.class)     // чтобы 404/500 отдавались как в бою (JSON)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RealtimeStatsControllerSliceTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockitoBean
    RealtimeStatsService realtimeStatsService;

    // ───────────── утилиты логов ─────────────
    private static void step(String title) {
        System.out.println("\n" + "─".repeat(80));
        System.out.println("▶ " + title);
        System.out.println("─".repeat(80));
    }
    private static void note(String s) { System.out.println("  • " + s); }
    private static void ok(String s)   { System.out.println("  ✓ " + s); }

    // ───────────── фабрики данных ─────────────
    private static RealtimeStatsDTO sampleDto() {
        LocalDateTime t0 = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime t1 = t0.plusMinutes(1);

        return RealtimeStatsDTO.builder()
                .activeRobots(5L)
                .totalRobots(12L)
                .checkedToday(345L)
                .criticalSkus(2L)
                .avgBatteryPercent(83.5)
                .activitySeries(List.of(
                        RealtimeStatsDTO.ActivityPoint.builder().ts(t0).count(10L).build(),
                        RealtimeStatsDTO.ActivityPoint.builder().ts(t1).count(12L).build()
                ))
                .serverTime(LocalDateTime.of(2025, 1, 1, 12, 34))
                .build();
    }

    @Nested
    @DisplayName("GET /api/{warehouseCode}/realtime/stats")
    class Stats {

        @Test
        @DisplayName("200 OK — возвращает DTO и прокидывает warehouseCode в сервис")
        void returns200_and_passesWarehouseCode() throws Exception {
            String code = "WH-42";
            var dto = sampleDto();

            step("STUB: сервис возвращает собранные метрики для " + code);
            when(realtimeStatsService.getStats(eq(code))).thenReturn(dto);
            ok("Заглушка установлена");

            step("HTTP GET /api/{code}/realtime/stats");
            var res = mvc.perform(get("/api/{warehouseCode}/realtime/stats", code)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.activeRobots").value(5))
                    .andExpect(jsonPath("$.totalRobots").value(12))
                    .andExpect(jsonPath("$.avgBatteryPercent").value(83.5))
                    .andExpect(jsonPath("$.activitySeries", Matchers.hasSize(2)))
                    .andReturn();

            String raw = res.getResponse().getContentAsString(StandardCharsets.UTF_8);
            System.out.println("\n[Ответ /realtime/stats]\n" +
                    om.writerWithDefaultPrettyPrinter().writeValueAsString(om.readTree(raw)));

            step("VERIFY: сервис вызван с корректным warehouseCode");
            ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
            verify(realtimeStatsService, times(1)).getStats(cap.capture());
            assertThat(cap.getValue()).isEqualTo(code);
            ok("Прокинут именно " + code);
        }

        @Test
        @DisplayName("404 Not Found — если сервис кидает NotFoundException")
        void returns404_when_serviceThrowsNotFound() throws Exception {
            String code = "UNKNOWN-WH";

            step("STUB: сервис кидает NotFoundException");
            when(realtimeStatsService.getStats(eq(code)))
                    .thenThrow(new NotFoundException("Warehouse not found: " + code));
            ok("Заглушка установлена");

            step("HTTP GET /api/{code}/realtime/stats (неизвестный склад)");
            mvc.perform(get("/api/{warehouseCode}/realtime/stats", code)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            step("VERIFY: сервис дернулся один раз");
            verify(realtimeStatsService, times(1)).getStats(eq(code));
            ok("Вызов корректный");
        }

        @Test
        @DisplayName("500 Internal Server Error — если сервис кидает неожиданную RuntimeException")
        void returns500_when_serviceThrowsUnexpected() throws Exception {
            String code = "WH-ERR";

            step("STUB: сервис кидает RuntimeException");
            when(realtimeStatsService.getStats(eq(code)))
                    .thenThrow(new RuntimeException("boom"));
            ok("Заглушка установлена");

            step("HTTP GET /api/{code}/realtime/stats (авария)");
            mvc.perform(get("/api/{warehouseCode}/realtime/stats", code)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());

            verify(realtimeStatsService, times(1)).getStats(eq(code));
            ok("500 получен, вызов корректный");
        }
    }
}
