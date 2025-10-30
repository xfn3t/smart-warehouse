package ru.rtc.warehouse.dashboard.redis;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class RealtimeRedisKeys {
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private RealtimeRedisKeys() {}

    public static String checkedToday(LocalDate day) { return "rt:checked:" + DAY.format(day); }
    public static String activityMinute(long epochMinute) { return "rt:act:" + epochMinute; }
    public static String robotsAll() { return "rt:robots:all"; }
    public static String robotsActive() { return "rt:robots:active"; }
    public static String batteryHash() { return "rt:robots:battery"; }
    public static String batterySum() { return "rt:robots:battery:sum"; }
    public static String batteryCnt() { return "rt:robots:battery:cnt"; }
    public static String criticalSkuSet() { return "rt:sku:critical"; }

    public static ZoneId zone() { return ZoneId.systemDefault(); }

    /** Эпоха минуты для локального времени сервера. */
    public static long epochMinute(LocalDateTime ldt) {
        return ldt.atZone(zone()).truncatedTo(ChronoUnit.MINUTES).toEpochSecond();
    }
}
