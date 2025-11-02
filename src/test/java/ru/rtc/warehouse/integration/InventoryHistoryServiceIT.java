package ru.rtc.warehouse.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.service.InventoryHistoryQueryService;
import ru.rtc.warehouse.inventory.service.dto.HistoryPageDTO;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DisplayName("InventoryHistoryQueryService IT (реальная БД, профиль test, схема из дампа)")
class InventoryHistoryServiceIT {

    @Autowired JdbcTemplate jdbc;
    @Autowired InventoryHistoryQueryService service;

    private static void step(String t){ System.out.println("\n" + "═".repeat(80) + "\n▶ " + t + "\n" + "═".repeat(80)); }
    private static void ok(String s){ System.out.println("  ✓ " + s); }

    @BeforeEach
    void seed() {
        step("DB SEED: TRUNCATE + справочники + тестовые данные (схема из дампа)");

        // порядок не критичен из-за CASCADE, но явно перечислим
        jdbc.update("""
            TRUNCATE TABLE
              inventory_history,
              products,
              robots,
              location,
              inventory_status,
              location_status,
              warehouses
            RESTART IDENTITY CASCADE
        """);
        ok("TRUNCATE выполнен");

        // ── справочник статусов истории (имеет is_deleted)
        jdbc.update("INSERT INTO inventory_status(code, is_deleted) VALUES (?, false)", "OK");
        jdbc.update("INSERT INTO inventory_status(code, is_deleted) VALUES (?, false)", "LOW_STOCK");
        jdbc.update("INSERT INTO inventory_status(code, is_deleted) VALUES (?, false)", "CRITICAL");

        // ── справочник статусов локаций (ВАЖНО: без is_deleted в схеме)
        jdbc.update("INSERT INTO location_status(code) VALUES (?)", "AVAILABLE");
        jdbc.update("INSERT INTO location_status(code) VALUES (?)", "BLOCKED");
        ok("Справочники inventory_status и location_status вставлены");

        // ── склады
        jdbc.update("""
            INSERT INTO warehouses(code, name, zone_max_size, row_max_size, shelf_max_size, location, is_deleted)
            VALUES ('WH-Q-IT-1','Main 1',10,10,10,'',false)
        """);
        jdbc.update("""
            INSERT INTO warehouses(code, name, zone_max_size, row_max_size, shelf_max_size, location, is_deleted)
            VALUES ('WH-Q-IT-2','Main 2',10,10,10,'',false)
        """);
        Long w1 = id("SELECT id FROM warehouses WHERE code='WH-Q-IT-1'");
        Long w2 = id("SELECT id FROM warehouses WHERE code='WH-Q-IT-2'");

        Long stOk    = id("SELECT id FROM inventory_status WHERE code='OK'");
        Long stCrit  = id("SELECT id FROM inventory_status WHERE code='CRITICAL'");
        Long locOkId = id("SELECT id FROM location_status WHERE code='AVAILABLE'");

        // ── локации (таблица 'location', колонки zone, "row", shelf, location_status_id, warehouse_id)
        jdbc.update("""
            INSERT INTO location(warehouse_id, zone, "row", shelf, location_status_id)
            VALUES (?,?,?,?,?)
        """, w1, 1, 1, 1, locOkId);
        jdbc.update("""
            INSERT INTO location(warehouse_id, zone, "row", shelf, location_status_id)
            VALUES (?,?,?,?,?)
        """, w1, 1, 2, 3, locOkId);
        jdbc.update("""
            INSERT INTO location(warehouse_id, zone, "row", shelf, location_status_id)
            VALUES (?,?,?,?,?)
        """, w2, 2, 1, 1, locOkId);

        Long l11 = id("SELECT id FROM location WHERE warehouse_id=? AND zone=1 AND \"row\"=1 AND shelf=1", w1);
        Long l12 = id("SELECT id FROM location WHERE warehouse_id=? AND zone=1 AND \"row\"=2 AND shelf=3", w1);
        Long l21 = id("SELECT id FROM location WHERE warehouse_id=? AND zone=2 AND \"row\"=1 AND shelf=1", w2);

        // ── роботы
        jdbc.update("INSERT INTO robots(warehouse_id, robot_code, battery_level, is_deleted) VALUES (?,?,100,false)", w1, "RB-1");
        jdbc.update("INSERT INTO robots(warehouse_id, robot_code, battery_level, is_deleted) VALUES (?,?,100,false)", w1, "RB-2");
        jdbc.update("INSERT INTO robots(warehouse_id, robot_code, battery_level, is_deleted) VALUES (?,?,100,false)", w2, "RB-9");

        Long r11 = id("SELECT id FROM robots WHERE robot_code='RB-1'");
        Long r12 = id("SELECT id FROM robots WHERE robot_code='RB-2'");
        Long r21 = id("SELECT id FROM robots WHERE robot_code='RB-9'");

        // ── продукты
        jdbc.update("""
            INSERT INTO products(warehouse_id, sku_code, name, category, min_stock, optimal_stock, is_deleted)
            VALUES (?,?,?,?,10,100,false)
        """, w1, "SKU-1", "Router",  "network");
        jdbc.update("""
            INSERT INTO products(warehouse_id, sku_code, name, category, min_stock, optimal_stock, is_deleted)
            VALUES (?,?,?,?,10,100,false)
        """, w1, "SKU-2", "Switch",  "network");
        jdbc.update("""
            INSERT INTO products(warehouse_id, sku_code, name, category, min_stock, optimal_stock, is_deleted)
            VALUES (?,?,?,?,10,100,false)
        """, w2, "SKU-9", "Hammer",  "tools");

        Long p11 = id("SELECT id FROM products WHERE sku_code='SKU-1'");
        Long p12 = id("SELECT id FROM products WHERE sku_code='SKU-2'");
        Long p21 = id("SELECT id FROM products WHERE sku_code='SKU-9'");

        // ── история (обязательно: warehouse_id, quantity, scanned_at; остальное по необходимости)
        LocalDateTime now = LocalDateTime.now();

        insertHistory(w1, l11, r11, p11, stOk,   10, 12, now.minusDays(3), now.minusDays(3).plusMinutes(5));
        insertHistory(w1, l12, r12, p12, stCrit, 5,  20, now.minusDays(1), now.minusDays(1).plusMinutes(7));
        insertHistory(w2, l21, r21, p21, stOk,  100, 50, now.minusDays(1), now.minusDays(1).plusMinutes(1));

        ok("История вставлена");
    }

    // ───────────────────────────── ТЕСТЫ ──────────────────────────────────────

    @Test
    @DisplayName("Скоуп по складу + период + фильтр по статусам + дефолтная сортировка")
    void scopes_period_status_defaultSort() {
        var rq = new InventoryHistorySearchRequest();
        rq.setFrom(Instant.now().minusSeconds(7*24*3600));
        rq.setTo(Instant.now());
        rq.setStatuses(List.of(InventoryHistoryStatus.InventoryHistoryStatusCode.CRITICAL));

        Pageable pg = PageRequest.of(0, 20, Sort.unsorted()); // сервис подставит scannedAt DESC
        HistoryPageDTO page = service.search("WH-Q-IT-1", rq, pg);

        assertThat(page.getItems())
                .extracting(InventoryHistoryDTO::getSkuCode)
                .contains("SKU-2")
                .doesNotContain("SKU-9");
        assertThat(page.getItems()).isSortedAccordingTo((a, b) -> b.getScannedAt().compareTo(a.getScannedAt()));
        ok("Скоуп/период/статус/дефолтная сортировка работают");
    }

    @Test
    @DisplayName("Ручная пагинация: стабильный размер страницы и корректные номера")
    void pagination_manualSlice() {
        Long w1 = id("SELECT id FROM warehouses WHERE code='WH-Q-IT-1'");
        Long l  = id("SELECT id FROM location WHERE warehouse_id=? LIMIT 1", w1);
        Long r  = upsertRobot(w1, "RB-10");
        Long p  = upsertProduct(w1, "SKU-10", "Cable", "network");
        Long st = id("SELECT id FROM inventory_status WHERE code='OK'");
        LocalDateTime base = LocalDateTime.now().minusHours(30);

        for (int i = 0; i < 25; i++) {
            insertHistory(w1, l, r, p, st, 50+i, 60, base.plusHours(i), base.plusHours(i).plusMinutes(1));
        }

        var rq = new InventoryHistorySearchRequest();
        rq.setFrom(Instant.now().minusSeconds(60*24*3600));
        rq.setTo(Instant.now());

        var pageable = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("scannedAt"))); // страница #1 (вторая)
        HistoryPageDTO page = service.search("WH-Q-IT-1", rq, pageable);

        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(10);
        assertThat(page.getItems()).hasSize(10);
        assertThat(page.getTotal()).isGreaterThan(10);
        ok("Пагинация стабильная");
    }

    // ─────────────────────────── helpers ───────────────────────────

    private Long id(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private void insertHistory(Long wId, Long locId, Long robId, Long prodId, Long stId,
                               int qty, int expected,
                               LocalDateTime scannedAt, LocalDateTime createdAt) {
        jdbc.update("""
            INSERT INTO inventory_history(
                warehouse_id, location_id, robot_id, product_id, status_id,
                quantity, expected_quantity, difference,
                scanned_at, created_at, is_deleted
            ) VALUES (?,?,?,?,?, ?,?,?, ?,?, false)
        """,
                wId, locId, robId, prodId, stId,
                qty, expected, qty - expected,
                Timestamp.valueOf(scannedAt), Timestamp.valueOf(createdAt)
        );
    }

    private Long upsertRobot(Long wId, String code) {
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM robots WHERE robot_code=?", Integer.class, code);
        if (exists != null && exists > 0) return id("SELECT id FROM robots WHERE robot_code=?", code);
        jdbc.update("INSERT INTO robots(warehouse_id, robot_code, battery_level, is_deleted) VALUES (?,?,100,false)", wId, code);
        return id("SELECT id FROM robots WHERE robot_code=?", code);
    }

    private Long upsertProduct(Long wId, String sku, String name, String category) {
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE sku_code=?", Integer.class, sku);
        if (exists != null && exists > 0) return id("SELECT id FROM products WHERE sku_code=?", sku);
        jdbc.update("""
            INSERT INTO products(warehouse_id, sku_code, name, category, min_stock, optimal_stock, is_deleted)
            VALUES (?,?,?,?,10,100,false)
        """, wId, sku, name, category);
        return id("SELECT id FROM products WHERE sku_code=?", sku);
    }
}
