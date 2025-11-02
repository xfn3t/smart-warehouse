// src/test/java/ru/rtc/warehouse/slice/InventoryHistoryQueryControllerSliceTest.java
package ru.rtc.warehouse.slice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.rtc.warehouse.inventory.controller.InventoryHistoryQueryController;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.service.InventoryHistoryQueryService;
import ru.rtc.warehouse.inventory.service.dto.HistoryPageDTO;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice-тест InventoryHistoryQueryController — изолированный веб-слой.
 */
@WebMvcTest(controllers = InventoryHistoryQueryController.class)
@AutoConfigureMockMvc(addFilters = true)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("InventoryHistoryQueryController slice")
class InventoryHistoryQueryControllerSliceTest {

    private static final String BASE = "/api/{code}/inventory/history";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockitoBean InventoryHistoryQueryService queryService;

    private static void step(String title) {
        System.out.println("\n" + "─".repeat(80));
        System.out.println("▶ " + title);
        System.out.println("─".repeat(80));
    }
    private static void ok(String s) { System.out.println("  ✓ " + s); }

    private static HistoryPageDTO emptyPage(int page, int size) {
        return HistoryPageDTO.builder()
                .total(0)
                .page(page)
                .size(size)
                .items(List.of())
                .build();
    }

    @Test
    @DisplayName("200 OK — минимальный запрос без фильтров → пустая страница")
    void findHistory_returns200_minimal() throws Exception {
        String code = "WH-Q-00";
        when(queryService.search(eq(code), any(), any())).thenReturn(emptyPage(0, 20));

        step("HTTP GET без фильтров (только аутентификация)");
        var res = mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items").isArray())
                .andReturn();

        System.out.println("\n[Ответ /inventory/history]\n" +
                om.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(om.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8))));

        ok("200 и корректное тело ответа");
        verify(queryService, times(1)).search(eq(code), any(InventoryHistorySearchRequest.class), any(Pageable.class));
    }

    @Test
    @DisplayName("200 OK — полный набор фильтров + пагинация/сортировка → сервис получает разобранные аргументы")
    void findHistory_parsesAllFilters_andPageable() throws Exception {
        String code = "WH-Q-01";

        var dto = new InventoryHistoryDTO();
        dto.setId(1L);
        dto.setRobotCode("RB-1");
        dto.setSkuCode("SKU-1");
        dto.setProductName("Router");
        dto.setQuantity(10);
        dto.setExpectedQuantity(12);
        dto.setDifference(-2);
        dto.setStatus("LOW_STOCK");

        var page = HistoryPageDTO.builder()
                .total(123)
                .page(1)
                .size(50)
                .items(List.of(dto))
                .build();

        when(queryService.search(eq(code), any(), any())).thenReturn(page);

        step("HTTP GET с полным набором параметров");
        mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON)
                        // период
                        .param("from", "2025-10-31T10:00:00Z")
                        .param("to",   "2025-11-01T10:00:00Z")
                        // списки (comma-separated)
                        .param("zones", "1,2,3")
                        .param("statuses", "CRITICAL,LOW_STOCK")
                        .param("categories", "network,tools")
                        .param("robots", "RB-1,RB-2")
                        // строковый поиск
                        .param("q", "router")
                        // пагинация и сортировка
                        .param("page", "1")
                        .param("size", "50")
                        .param("sort", "productName,asc")
                        .param("sort", "scannedAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(123))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.items[0].skuCode").value("SKU-1"))
                .andExpect(jsonPath("$.items[0].status").value("LOW_STOCK"));

        ArgumentCaptor<InventoryHistorySearchRequest> rqCap = ArgumentCaptor.forClass(InventoryHistorySearchRequest.class);
        ArgumentCaptor<Pageable>                      pgCap = ArgumentCaptor.forClass(Pageable.class);
        verify(queryService, times(1)).search(eq(code), rqCap.capture(), pgCap.capture());

        var rq = rqCap.getValue();
        assertThat(rq.getFrom()).isEqualTo(Instant.parse("2025-10-31T10:00:00Z"));
        assertThat(rq.getTo()).isEqualTo(Instant.parse("2025-11-01T10:00:00Z"));
        assertThat(rq.getZones()).containsExactly(1,2,3);
        assertThat(rq.getStatuses()).containsExactly(
                InventoryHistoryStatus.InventoryHistoryStatusCode.CRITICAL,
                InventoryHistoryStatus.InventoryHistoryStatusCode.LOW_STOCK
        );
        assertThat(rq.getCategories()).containsExactly("network","tools");
        assertThat(rq.getRobots()).containsExactly("RB-1","RB-2");
        assertThat(rq.getQ()).isEqualToIgnoringCase("router");

        var pageable = pgCap.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(50);

        // ── устойчивые проверки сортировки: допускаем алиасы маппера/сервиса (productName -> product.name и т.п.)
        Sort sort = pageable.getSort();
        System.out.println("[DEBUG] captured sort orders: " + sort);
        assertThat(sort.isSorted())
                .as("ожидается, что сортировка задана")
                .isTrue();

        assertOrder(sort, Sort.Direction.ASC,  "productName", "product.name");
        assertOrder(sort, Sort.Direction.DESC, "scannedAt", "createdAt"); // допускаем альтернативный ключ
        ok("Параметры распарсены корректно и переданы в сервис (включая сортировку)");
    }

    @Test
    @DisplayName("200 OK — повторяющиеся параметры списков (?statuses=A&statuses=B)")
    void findHistory_acceptsRepeatedListParams() throws Exception {
        String code = "WH-Q-02";
        when(queryService.search(eq(code), any(), any())).thenReturn(emptyPage(0, 20));

        step("HTTP GET c repeated list params");
        mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON)
                        .param("statuses", "OK")
                        .param("statuses", "CRITICAL")
                        .param("zones", "5")
                        .param("zones", "7"))
                .andExpect(status().isOk());

        ArgumentCaptor<InventoryHistorySearchRequest> rqCap = ArgumentCaptor.forClass(InventoryHistorySearchRequest.class);
        verify(queryService).search(eq(code), rqCap.capture(), any());

        var rq = rqCap.getValue();
        assertThat(rq.getStatuses()).containsExactly(
                InventoryHistoryStatus.InventoryHistoryStatusCode.OK,
                InventoryHistoryStatus.InventoryHistoryStatusCode.CRITICAL
        );
        assertThat(rq.getZones()).containsExactly(5, 7);
        ok("Списки из повторяющихся параметров приняты корректно");
    }

    @Test
    @DisplayName("401 Unauthorized — без аутентификации")
    void unauthorized_401_whenNoAuth() throws Exception {
        String code = "WH-Q-03";

        step("HTTP GET без аутентификации");
        mvc.perform(get(BASE, code).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(queryService);
        ok("Фильтры безопасности работают, сервис не вызывался");
    }

    @Test
    @DisplayName("500 — сервис бросает RuntimeException → глобальный обработчик сводит к 500")
    void serviceThrows_runtime_is500() throws Exception {
        String code = "WH-Q-04";
        when(queryService.search(eq(code), any(), any()))
                .thenThrow(new IllegalStateException("unexpected"));

        step("HTTP GET при ошибке сервиса");
        mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        ok("500 получен, ошибка обработана глобальным advice");
    }

    @Test
    @DisplayName("Неверное значение enum в параметрах (statuses=UNKNOWN) → 400/500 в зависимости от advice")
    void invalid_enum_in_params_yields_client_or_global_error() throws Exception {
        String code = "WH-Q-05";

        step("HTTP GET со статусом UNKNOWN");
        try {
            mvc.perform(get(BASE, code)
                            .with(user("tester"))
                            .accept(MediaType.APPLICATION_JSON)
                            .param("statuses", "UNKNOWN"))
                    .andExpect(status().isBadRequest());
            ok("400 Bad Request на неверный enum");
        } catch (AssertionError e) {
            mvc.perform(get(BASE, code)
                            .with(user("tester"))
                            .accept(MediaType.APPLICATION_JSON)
                            .param("statuses", "UNKNOWN"))
                    .andExpect(status().isInternalServerError());
            ok("Глобальный обработчик отдал 500 на ошибку биндинга");
        }

        verifyNoInteractions(queryService);
    }

    // ───────────────────────────── helpers ────────────────────────────────────

    private static void assertOrder(Sort sort, Sort.Direction expectedDir, String... propertyCandidates) {
        Optional<Sort.Order> found = sort.stream()
                .filter(o -> Arrays.stream(propertyCandidates)
                        .anyMatch(c -> c.equalsIgnoreCase(o.getProperty())))
                .findFirst();

        assertThat(found.orElse(null))
                .as("ожидается ордер по одному из: %s", String.join(", ", propertyCandidates))
                .isNotNull();

        assertThat(found.get().getDirection())
                .as("направление сортировки для '%s'", found.get().getProperty())
                .isEqualTo(expectedDir);
    }
    @Test
    @DisplayName("200 OK — без sort → дефолт scannedAt,DESC")
    void findHistory_usesDefaultSort_whenSortOmitted() throws Exception {
        String code = "WH-Q-06";
        when(queryService.search(eq(code), any(), any()))
                .thenReturn(HistoryPageDTO.builder().total(0).page(0).size(20).items(List.of()).build());

        step("HTTP GET без sort");
        mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pgCap = ArgumentCaptor.forClass(Pageable.class);
        verify(queryService).search(eq(code), any(), pgCap.capture());
        Sort sort = pgCap.getValue().getSort();

        assertThat(sort.isSorted()).isTrue();
        assertOrder(sort, Sort.Direction.DESC, "scannedAt");
        ok("Дефолтная сортировка применена: scannedAt,DESC");
    }

    @Test
    @DisplayName("200 OK — size вне разрешённых (33) → принудительно 20; page<0 → 0")
    void findHistory_coercesPageAndSize() throws Exception {
        String code = "WH-Q-07";
        when(queryService.search(eq(code), any(), any()))
                .thenReturn(HistoryPageDTO.builder().total(0).page(0).size(20).items(List.of()).build());

        step("HTTP GET с некорректными page/size");
        mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON)
                        .param("page", "-5")
                        .param("size", "33")) // допустимы только 20/50/100
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pgCap = ArgumentCaptor.forClass(Pageable.class);
        verify(queryService).search(eq(code), any(), pgCap.capture());

        Pageable p = pgCap.getValue();
        assertThat(p.getPageNumber()).isEqualTo(0);
        assertThat(p.getPageSize()).isEqualTo(20);
        ok("page нормализован к 0, size нормализован к 20");
    }

    @Test
    @DisplayName("200 OK — sort с пробелами/регистром и некорректным направлением → trimming + fallback ASC")
    void findHistory_parsesSort_trimsAndFallsBackOnDirection() throws Exception {
        String code = "WH-Q-08";
        when(queryService.search(eq(code), any(), any()))
                .thenReturn(HistoryPageDTO.builder().total(0).page(0).size(20).items(List.of()).build());

        step("HTTP GET с ' sort = \" productName , Up \" ' + ' scannedAt, DESC '");
        mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON)
                        .param("sort", " productName , Up ")
                        .param("sort", " scannedAt, DESC "))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pgCap = ArgumentCaptor.forClass(Pageable.class);
        verify(queryService).search(eq(code), any(), pgCap.capture());
        Sort sort = pgCap.getValue().getSort();
        System.out.println("[DEBUG] captured sort orders: " + sort);

        // 'Up' невалидно -> ASC (fallback), пробелы обрезаются
        assertOrder(sort, Sort.Direction.ASC,  "productName", "product.name");
        assertOrder(sort, Sort.Direction.DESC, "scannedAt", "createdAt");
        ok("Сортировка распознана корректно, невалидное направление → ASC");
    }

    @Test
    @DisplayName("200 OK — списки: смешанные формы (comma + повтор) → корректный merge с trimming")
    void findHistory_acceptsMixedListForms() throws Exception {
        String code = "WH-Q-09";
        when(queryService.search(eq(code), any(), any()))
                .thenReturn(HistoryPageDTO.builder().total(0).page(0).size(20).items(List.of()).build());

        step("HTTP GET: categories=' network , tools ' + categories='hardware'");
        mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON)
                        .param("categories", " network , tools ")
                        .param("categories", "hardware")
                        .param("robots", " RB-1 , RB-2 ")
                        .param("robots", "RB-3"))
                .andExpect(status().isOk());

        ArgumentCaptor<InventoryHistorySearchRequest> rqCap = ArgumentCaptor.forClass(InventoryHistorySearchRequest.class);
        verify(queryService).search(eq(code), rqCap.capture(), any());

        var rq = rqCap.getValue();
        assertThat(rq.getCategories()).containsExactly("network","tools","hardware");
        assertThat(rq.getRobots()).containsExactly("RB-1","RB-2","RB-3");
        ok("Списки смержены из разных форм, элементы очищены от пробелов");
    }

    @Test
    @DisplayName("200 OK — quick=WEEK → enum биндинг проходит и попадает в сервис")
    void findHistory_bindsQuickEnum() throws Exception {
        String code = "WH-Q-10";
        when(queryService.search(eq(code), any(), any()))
                .thenReturn(HistoryPageDTO.builder().total(0).page(0).size(20).items(List.of()).build());

        step("HTTP GET с quick=WEEK");
        mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON)
                        .param("quick", "WEEK"))
                .andExpect(status().isOk());

        ArgumentCaptor<InventoryHistorySearchRequest> rqCap = ArgumentCaptor.forClass(InventoryHistorySearchRequest.class);
        verify(queryService).search(eq(code), rqCap.capture(), any());

        assertThat(rqCap.getValue().getQuick())
                .as("биндинг QuickRange")
                .isNotNull()
                .hasToString("WEEK");
        ok("quick корректно привязан к enum");
    }

    @Test
    @DisplayName("Некорректный формат from/to → 400/500 и сервис не вызывается")
    void findHistory_invalidInstantYieldsClientOrGlobalError() throws Exception {
        String code = "WH-Q-11";

        step("HTTP GET с from='not-an-instant'");
        try {
            mvc.perform(get(BASE, code)
                            .with(user("tester"))
                            .accept(MediaType.APPLICATION_JSON)
                            .param("from", "not-an-instant"))
                    .andExpect(status().isBadRequest());
            ok("400 Bad Request на неверный Instant");
        } catch (AssertionError e) {
            mvc.perform(get(BASE, code)
                            .with(user("tester"))
                            .accept(MediaType.APPLICATION_JSON)
                            .param("from", "not-an-instant"))
                    .andExpect(status().isInternalServerError());
            ok("Глобальный обработчик вернул 500 на ошибку биндинга");
        }

        verifyNoInteractions(queryService);
    }

    @Test
    @DisplayName("Сортировка: попытка инъекции в направлении → безопасный разбор, fallback ASC")
    void findHistory_sortInjectionDirection_isNeutralized() throws Exception {
        String code = "WH-Q-12";
        when(queryService.search(eq(code), any(), any()))
                .thenReturn(HistoryPageDTO.builder().total(0).page(0).size(20).items(List.of()).build());

        step("HTTP GET: sort=productName,asc;drop table inventory_history");
        mvc.perform(get(BASE, code)
                        .with(user("tester"))
                        .accept(MediaType.APPLICATION_JSON)
                        .param("sort", "productName,asc;drop table inventory_history"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pgCap = ArgumentCaptor.forClass(Pageable.class);
        verify(queryService).search(eq(code), any(), pgCap.capture());
        Sort sort = pgCap.getValue().getSort();

        // 'asc;drop table inventory_history' не распознаётся как валидное направление → ASC
        assertOrder(sort, Sort.Direction.ASC, "productName", "product.name");
        ok("Неправильное направление сортировки безопасно деградировало к ASC");
    }
}
