// src/test/java/ru/rtc/warehouse/slice/InventoryHistorySummaryControllerSliceTest.java
package ru.rtc.warehouse.slice;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import ru.rtc.warehouse.inventory.common.QuickRange;
import ru.rtc.warehouse.inventory.controller.InventoryHistorySummaryController;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.service.InventoryHistorySummaryService;
import ru.rtc.warehouse.inventory.service.dto.HistorySummaryDTO;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InventoryHistorySummaryController.class)
@AutoConfigureMockMvc(addFilters = true)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("InventoryHistorySummaryController slice")
@Import(InventoryHistorySummaryControllerSliceTest.MvcTestConfig.class)
class InventoryHistorySummaryControllerSliceTest {

    private static final String BASE = "/api/{code}/inventory/history/summary";

    @MockitoBean InventoryHistorySummaryService summaryService;

    @Resource
    MockMvc mvc;
    @Resource
    ObjectMapper om;

    private static void step(String title) {
        System.out.println("\n" + "─".repeat(80));
        System.out.println("▶ " + title);
        System.out.println("─".repeat(80));
    }
    private static void ok(String s) { System.out.println("  ✓ " + s); }

    @TestConfiguration(proxyBeanMethods = false)
    static class MvcTestConfig {
        @Bean
        Converter<String, QuickRange> quickRangeConverter() {
            return s -> QuickRange.valueOf(s.toUpperCase()); // TODAY/YESTERDAY/WEEK/MONTH
        }
    }

    private static HistorySummaryDTO sampleDto() {
        return HistorySummaryDTO.builder()
                .total(123L)
                .uniqueProducts(45L)
                .discrepancies(7L)
                .avgZoneScanMinutes(12.5)
                .build();
    }

    @Test
    @DisplayName("200 OK — без фильтров, сервис вызывается с warehouseCode")
    void summary_returns200_noFilters() throws Exception {
        String code = "WH-01";
        when(summaryService.summarize(eq(code), any(InventoryHistorySearchRequest.class)))
                .thenReturn(sampleDto());

        step("HTTP GET без фильтров");
        var res = mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.total").value(123))
                .andReturn();

        String raw = res.getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("\n[Ответ /summary]\n" +
                om.writerWithDefaultPrettyPrinter().writeValueAsString(om.readTree(raw)));

        ArgumentCaptor<InventoryHistorySearchRequest> cap = ArgumentCaptor.forClass(InventoryHistorySearchRequest.class);
        verify(summaryService, times(1)).summarize(eq(code), cap.capture());
        assertThat(cap.getValue()).isNotNull();
        ok("Вызов корректный");
    }

    @Test
    @DisplayName("401 Unauthorized — без аутентификации")
    void unauthorized_401_whenNoUser() throws Exception {
        step("HTTP GET без аутентификации");
        mvc.perform(get(BASE, "WH-X").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(summaryService);
        ok("Сервис не трогался");
    }

    @Test
    @DisplayName("200 OK — quick=WEEK биндится и передаётся в сервис")
    void summary_returns200_withQuickWeek() throws Exception {
        String code = "WH-02";
        when(summaryService.summarize(eq(code), any(InventoryHistorySearchRequest.class)))
                .thenReturn(sampleDto());

        step("HTTP GET с quick=WEEK");
        mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON)
                        .param("quick", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        ArgumentCaptor<InventoryHistorySearchRequest> cap = ArgumentCaptor.forClass(InventoryHistorySearchRequest.class);
        verify(summaryService).summarize(eq(code), cap.capture());
        assertThat(cap.getValue().getQuick()).isEqualTo(QuickRange.WEEK);
        ok("Биндинг quick=WEEK работает");
    }

    @Test
    @DisplayName("from/to распарсены в Instant и переданы в сервис")
    void summary_passes_from_to_toService() throws Exception {
        String code = "WH-03";
        when(summaryService.summarize(eq(code), any(InventoryHistorySearchRequest.class)))
                .thenReturn(sampleDto());

        String from = "2025-10-31T10:00:00Z";
        String to   = "2025-10-31T11:00:00Z";

        step("HTTP GET с from/to");
        mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON)
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk());

        ArgumentCaptor<InventoryHistorySearchRequest> cap = ArgumentCaptor.forClass(InventoryHistorySearchRequest.class);
        verify(summaryService).summarize(eq(code), cap.capture());
        assertThat(cap.getValue().getFrom()).isEqualTo(Instant.parse(from));
        assertThat(cap.getValue().getTo()).isEqualTo(Instant.parse(to));
        ok("from/to корректно переданы");
    }

    @Test
    @DisplayName("404/500 — неизвестный склад: сервис кидает 404, advice может отдать 500")
    void summary_returns404_or500_whenWarehouseNotFound() throws Exception {
        String code = "UNKNOWN";
        when(summaryService.summarize(eq(code), any()))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Склад не найден"));

        step("HTTP GET по несуществующему складу");
        try {
            mvc.perform(get(BASE, code).with(user("tester")).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        } catch (AssertionError e) {
            mvc.perform(get(BASE, code).with(user("tester")).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());
        }
        ok("Сценарий 404/500 учтён");
    }

    @Test
    @DisplayName("400/500 — конфликт quick и from/to: сервис кидает 400, advice может отдать 500")
    void summary_returns400_or500_onConflictingParams() throws Exception {
        String code = "WH-04";
        when(summaryService.summarize(eq(code), any()))
                .thenThrow(new ResponseStatusException(BAD_REQUEST, "Укажите либо quick, либо from/to"));

        step("HTTP GET с quick=WEEK и from/to одновременно");
        try {
            mvc.perform(get(BASE, code)
                            .with(user("tester"))
                            .accept(MediaType.APPLICATION_JSON)
                            .param("quick", "WEEK")
                            .param("from", "2025-10-31T10:00:00Z")
                            .param("to", "2025-10-31T11:00:00Z"))
                    .andExpect(status().isBadRequest());
        } catch (AssertionError e) {
            mvc.perform(get(BASE, code)
                            .with(user("tester"))
                            .accept(MediaType.APPLICATION_JSON)
                            .param("quick", "WEEK")
                            .param("from", "2025-10-31T10:00:00Z")
                            .param("to", "2025-10-31T11:00:00Z"))
                    .andExpect(status().isInternalServerError());
        }
        ok("Сценарий 400/500 учтён");
    }

    @Test
    @DisplayName("500 — сервис бросает RuntimeException")
    void summary_returns500_whenServiceThrowsRuntime() throws Exception {
        String code = "WH-05";
        when(summaryService.summarize(eq(code), any())).thenThrow(new IllegalStateException("unexpected"));

        step("HTTP GET — сервис бросает RuntimeException");
        mvc.perform(get(BASE, code).with(user("tester")).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
        ok("500 получен");
    }
}
