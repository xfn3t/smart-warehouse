package ru.rtc.warehouse.dashboard.redis;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Генератор имён ключей для real-time метрик.
 */
public final class RealtimeRedisKeys {
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    private RealtimeRedisKeys() {}

    /** Счётчик "проверено сегодня": rt:checked:<yyyyMMdd> */
    public static String checkedToday(LocalDate day) {
        return "rt:checked:" + DAY.format(day);
    }

    /** Поминутная активность: rt:act:<epochMinute> */
    public static String activityMinute(long epochMinute) {
        return "rt:act:" + epochMinute;
    }

    /** Множество всех роботов (коды): rt:robots:all */
    public static String robotsAll() { return "rt:robots:all"; }

    /** Множество активных роботов (коды): rt:robots:active */
    public static String robotsActive() { return "rt:robots:active"; }

    /** Хэш {code -> battery%}: rt:robots:battery */
    public static String batteryHash() { return "rt:robots:battery"; }

    /** Сумма зарядов: rt:robots:battery:sum */
    public static String batterySum() { return "rt:robots:battery:sum"; }

    /** Кол-во роботов с известным зарядом: rt:robots:battery:cnt */
    public static String batteryCnt() { return "rt:robots:battery:cnt"; }

    /** Множество критических SKU: rt:sku:critical */
    public static String criticalSkuSet() { return "rt:sku:critical"; }

    /** Эпоха минуты (UTC) для метки времени. */
    public static long epochMinute(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC)
                .withSecond(0).withNano(0)
                .toEpochSecond();
    }
}