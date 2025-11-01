package ru.rtc.warehouse.dashboard.redis;

import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@NoArgsConstructor
public final class RealtimeRedisKeys {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private static String prefix(String warehouseCode) {
        return "rt:" + warehouseCode + ":";
    }

    public static String checkedToday(String warehouseCode, LocalDate day) {
        return prefix(warehouseCode) + "checked:" + DAY.format(day);
    }

    public static String activityMinute(String warehouseCode, long epochMinute) {
        return prefix(warehouseCode) + "act:" + epochMinute;
    }

    public static String robotsAll(String warehouseCode) {
        return prefix(warehouseCode) + "robots:all";
    }

    public static String robotsActive(String warehouseCode) {
        return prefix(warehouseCode) + "robots:active";
    }

    public static String batteryHash(String warehouseCode) {
        return prefix(warehouseCode) + "robots:battery";
    }

    public static String batterySum(String warehouseCode) {
        return prefix(warehouseCode) + "robots:battery:sum";
    }

    public static String batteryCnt(String warehouseCode) {
        return prefix(warehouseCode) + "robots:battery:cnt";
    }

    public static String criticalSkuSet(String warehouseCode) {
        return prefix(warehouseCode) + "sku:critical";
    }

    public static ZoneId zone() {
        return ZoneId.systemDefault();
    }

    public static long epochMinute(LocalDateTime ldt) {
        return ldt.atZone(zone()).truncatedTo(ChronoUnit.MINUTES).toEpochSecond();
    }
}
