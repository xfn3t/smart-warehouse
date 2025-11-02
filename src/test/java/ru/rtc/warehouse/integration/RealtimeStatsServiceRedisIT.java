package ru.rtc.warehouse.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import ru.rtc.warehouse.configuration.TestRedisContainerConfig;
import ru.rtc.warehouse.dashboard.dto.RealtimeStatsDTO;
import ru.rtc.warehouse.dashboard.redis.RealtimeRedisKeys;
import ru.rtc.warehouse.dashboard.service.RealtimeStatsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Интеграционный тест сервиса real-time статистики.
 * Реальный Redis через Testcontainers, автоматическое подключение через @ServiceConnection.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisContainerConfig.class)
@DisplayName("RealtimeStatsServiceRedisImpl IT (реальный Redis через Testcontainers)")
class RealtimeStatsServiceRedisIT {

    private static final String CODE = "WH-42";

    @Autowired RealtimeStatsService service;
    @Autowired StringRedisTemplate rt;

    // ───────────────────────────── утилиты логов ──────────────────────────────
    private static void step(String title) {
        System.out.println("\n" + "═".repeat(80));
        System.out.println("▶ " + title);
        System.out.println("═".repeat(80));
    }
    private static void note(String s) { System.out.println("  • " + s); }
    private static void ok(String s)   { System.out.println("  ✓ " + s); }

    // ───────────────────────────── подготовка Redis ───────────────────────────
    @BeforeEach
    void flushRedis() {
        step("REDIS: FLUSHDB перед тестом");
        rt.getConnectionFactory().getConnection().serverCommands().flushDb();
        ok("База Redis очищена");
    }

    // ───────────────────────────── тесты ──────────────────────────────────────

    @Test
    @DisplayName("Пустой Redis → нули/пустые и avgBattery=null, серия = 60 точек")
    void emptyRedis_returnsZeros() {
        step("ACT: вызов getStats для пустого Redis");
        RealtimeStatsDTO dto = service.getStats(CODE);

        step("ASSERT: карточки");
        assertThat(dto.getActiveRobots()).isZero();
        assertThat(dto.getTotalRobots()).isZero();
        assertThat(dto.getCriticalSkus()).isZero();
        assertThat(dto.getCheckedToday()).isZero();
        assertThat(dto.getAvgBatteryPercent()).isNull();
        ok("Все агрегаты равны 0/null, как ожидается");

        step("ASSERT: поминутная серия");
        assertThat(dto.getActivitySeries()).hasSize(60);
        ok("Серия содержит ровно 60 точек (последний час)");
    }

    @Test
    @DisplayName("Заполненный Redis → корректные агрегаты и значения серии по последним минутам (без завязки на индексы)")
    void seededRedis_ok() {
        ZoneId zone = RealtimeRedisKeys.zone();

        // фиксируем «окно» минут до сидирования и вызова сервиса — избегаем гонки на границе минуты
        LocalDateTime toMinute = LocalDateTime.now(zone).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime m0 = toMinute.minusMinutes(1);
        LocalDateTime m1 = toMinute;

        step("SEED: множества активных/всех роботов и критических SKU");
        rt.opsForSet().add(RealtimeRedisKeys.robotsActive(CODE), "r1","r2","r3","r4","r5"); // 5 активных
        rt.opsForSet().add(RealtimeRedisKeys.robotsAll(CODE),
                "r1","r2","r3","r4","r5","r6","r7","r8","r9","r10","r11","r12"); // 12 всего
        rt.opsForSet().add(RealtimeRedisKeys.criticalSkuSet(CODE), "skuA","skuB"); // 2 критических
        ok("sets посеяны");

        step("SEED: аккумуляторы батареи (sum/cnt) и checkedToday");
        rt.opsForValue().set(RealtimeRedisKeys.batterySum(CODE), "835");
        rt.opsForValue().set(RealtimeRedisKeys.batteryCnt(CODE), "10");  // avg = 83.5
        rt.opsForValue().set(RealtimeRedisKeys.checkedToday(CODE, LocalDate.now(zone)), "345");
        ok("battery/checkedToday посеяны");

        step("SEED: поминутная активность для m0 и m1");
        long e0 = RealtimeRedisKeys.epochMinute(m0);
        long e1 = RealtimeRedisKeys.epochMinute(m1);
        rt.opsForValue().set(RealtimeRedisKeys.activityMinute(CODE, e0), "10");
        rt.opsForValue().set(RealtimeRedisKeys.activityMinute(CODE, e1), "12");
        note("m0=" + m0 + " → 10;  m1=" + m1 + " → 12");
        ok("minute-series посеяны");

        step("ACT: вызов getStats");
        RealtimeStatsDTO dto = service.getStats(CODE);

        step("ASSERT: карточки");
        assertThat(dto.getActiveRobots()).isEqualTo(5);
        assertThat(dto.getTotalRobots()).isEqualTo(12);
        assertThat(dto.getCriticalSkus()).isEqualTo(2);
        assertThat(dto.getCheckedToday()).isEqualTo(345);
        assertThat(dto.getAvgBatteryPercent()).isCloseTo(83.5, within(1e-9));
        ok("Агрегаты соответствуют сиду");

        step("ASSERT: серия = 60 точек и корректные значения на m0/m1 (по ts)");
        assertThat(dto.getActivitySeries()).hasSize(60);

        var atM0 = dto.getActivitySeries().stream()
                .filter(p -> m0.equals(p.getTs()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Точка за m0 не найдена в серии"));
        var atM1 = dto.getActivitySeries().stream()
                .filter(p -> m1.equals(p.getTs()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Точка за m1 не найдена в серии"));

        assertThat(atM0.getCount()).isEqualTo(10);
        assertThat(atM1.getCount()).isEqualTo(12);
        ok("Значения на последних минутах считываются корректно (без хрупких индексов)");
    }

    @Test
    @DisplayName("Нечисловые/пустые значения в Redis → безопасно интерпретируются как 0")
    void nonNumericValues_areTreatedAsZero() {
        step("SEED: кладём мусорные строки в числовые ключи");
        rt.opsForValue().set(RealtimeRedisKeys.batterySum(CODE), "NaN");
        rt.opsForValue().set(RealtimeRedisKeys.batteryCnt(CODE), "");
        rt.opsForValue().set(RealtimeRedisKeys.checkedToday(CODE, LocalDate.now(RealtimeRedisKeys.zone())), "oops");

        step("ACT: вызов getStats");
        RealtimeStatsDTO dto = service.getStats(CODE);

        step("ASSERT: все численные агрегаты → 0/null без исключений");
        assertThat(dto.getAvgBatteryPercent()).isNull(); // cnt=0 → null
        assertThat(dto.getCheckedToday()).isZero();
        ok("Мусорные значения корректно «глотаются» и приводятся к 0/null");
    }
}
