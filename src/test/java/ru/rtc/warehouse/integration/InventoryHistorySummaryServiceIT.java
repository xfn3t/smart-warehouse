// src/test/java/ru/rtc/warehouse/integration/InventoryHistorySummaryServiceIT.java
package ru.rtc.warehouse.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import ru.rtc.warehouse.inventory.common.QuickRange;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.service.InventoryHistorySummaryService;
import ru.rtc.warehouse.inventory.service.dto.HistorySummaryDTO;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("InventoryHistorySummaryService IT")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class InventoryHistorySummaryServiceIT {

    @Autowired InventoryHistorySummaryService service;
    @Autowired JdbcTemplate jdbc;

    // ───────────────────────────── утилиты логов ──────────────────────────────
    private static void step(String title) {
        System.out.println("\n" + "═".repeat(80));
        System.out.println("▶ " + title);
        System.out.println("═".repeat(80));
    }
    private static void ok(String s) { System.out.println("  ✓ " + s); }

    // ───────────────────────────── подготовка БД ──────────────────────────────
    @BeforeEach
    void seed() {
        step("DB SEED: TRUNCATE основных таблиц и вставка справочников статусов");

        jdbc.update("""
            TRUNCATE TABLE inventory_history, products, warehouses, inventory_status
            RESTART IDENTITY CASCADE
        """);
        ok("TRUNCATE + RESTART IDENTITY выполнен");

        jdbc.update("""
            INSERT INTO inventory_status(code, is_deleted)
            VALUES ('OK', false), ('LOW_STOCK', false), ('CRITICAL', false)
        """);
        ok("Статусы OK/LOW_STOCK/CRITICAL вставлены");
    }

    private void insertWarehouse(String code, String name) {
        jdbc.update("""
            INSERT INTO warehouses(code, name, zone_max_size, row_max_size, shelf_max_size, location, is_deleted)
            VALUES (?, ?, 1, 1, 1, 'IT', false)
        """, code, name);
    }

    // ───────────────────────────── assert helpers ─────────────────────────────
    private static void assertRse(Throwable t, int expectedStatus, String reasonSubstring) {
        assertThat(t).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException rse = (ResponseStatusException) t;
        assertThat(rse.getStatusCode().value()).isEqualTo(expectedStatus);
        if (reasonSubstring != null && !reasonSubstring.isBlank()) {
            assertThat(rse.getReason()).contains(reasonSubstring);
        }
    }

    // ───────────────────────────── тесты ──────────────────────────────────────

    @Test
    @DisplayName("Пустой склад: total/unique/discrepancies=0, avg=null")
    void summarize_emptyWarehouse_returnsZerosAndNullAvg() {
        String wh = "WH-SUM-0";
        insertWarehouse(wh, "Summary Zero");
        var rq = new InventoryHistorySearchRequest();

        step("Вызов summarize для пустого склада");
        HistorySummaryDTO dto = service.summarize(wh, rq);

        assertThat(dto.getTotal()).isZero();
        assertThat(dto.getUniqueProducts()).isZero();
        assertThat(dto.getDiscrepancies()).isZero();
        assertThat(dto.getAvgZoneScanMinutes()).isNull();
        ok("Метрики для пустого склада корректны");
    }

    @Test
    @DisplayName("Валидация: from >= to → ResponseStatusException(400) + reason")
    void summarize_invalidRange_throws400() {
        String wh = "WH-SUM-1";
        insertWarehouse(wh, "Summary InvalidRange");

        var rq = new InventoryHistorySearchRequest();
        rq.setFrom(Instant.parse("2025-10-31T11:00:00Z"));
        rq.setTo  (Instant.parse("2025-10-31T11:00:00Z"));

        step("Проверка from >= to");
        assertThatThrownBy(() -> service.summarize(wh, rq))
                .satisfies(t -> assertRse(t, BAD_REQUEST.value(), "'from' must be < 'to'"));
        ok("400 на неверный диапазон получен");
    }

    @Test
    @DisplayName("Конфликт параметров: quick и from/to одновременно → 400 + reason")
    void summarize_conflictingQuickAndRange_throws400() {
        String wh = "WH-SUM-2";
        insertWarehouse(wh, "Summary Conflicting");

        var rq = new InventoryHistorySearchRequest();
        rq.setQuick(QuickRange.WEEK);
        rq.setFrom(Instant.parse("2025-10-31T10:00:00Z"));
        rq.setTo  (Instant.parse("2025-10-31T11:00:00Z"));

        step("Проверка конфликта quick vs from/to");
        assertThatThrownBy(() -> service.summarize(wh, rq))
                .satisfies(t -> assertRse(t, BAD_REQUEST.value(), "Укажите либо quick, либо from/to"));
        ok("400 на конфликт параметров получен");
    }

    @Test
    @DisplayName("Неизвестный склад → ResponseStatusException(404) + reason")
    void summarize_unknownWarehouse_throws404() {
        insertWarehouse("WH-SUM-3", "Summary Known");

        var rq = new InventoryHistorySearchRequest();
        step("Вызов summarize с несуществующим warehouseCode");
        assertThatThrownBy(() -> service.summarize("UNKNOWN-CODE", rq))
                .satisfies(t -> assertRse(t, NOT_FOUND.value(), "Склад не найден"));
        ok("404 на неизвестный склад получен");
    }

    @Test
    @DisplayName("quick=WEEK без from/to — сводка по пустому складу не падает")
    void summarize_quickWeek_withoutExplicitRange_okOnEmpty() {
        String wh = "WH-SUM-4";
        insertWarehouse(wh, "Summary Quick");

        var rq = new InventoryHistorySearchRequest();
        rq.setQuick(QuickRange.WEEK);

        step("quick=WEEK → QuickRangeResolver установит from/to");
        HistorySummaryDTO dto = service.summarize(wh, rq);

        assertThat(dto.getTotal()).isZero();
        assertThat(dto.getUniqueProducts()).isZero();
        assertThat(dto.getDiscrepancies()).isZero();
        assertThat(dto.getAvgZoneScanMinutes()).isNull();
        ok("Сводка по пустому складу с quick=WEEK корректна");
    }
}
