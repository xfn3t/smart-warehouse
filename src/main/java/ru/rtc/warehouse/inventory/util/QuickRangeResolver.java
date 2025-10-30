package ru.rtc.warehouse.inventory.util;


import ru.rtc.warehouse.inventory.common.QuickRange;


import java.time.*;


/**
 * Утилита для вычисления интервалов по быстрым пресетам с учётом таймзоны.
 * Интервалы возвращаются как [from; to), в UTC (Instant).
 */
public final class QuickRangeResolver {
    private QuickRangeResolver() {}


    /** Возвращает интервал [from; to) для указанного пресета в заданной таймзоне. */
    public static Instant[] resolve(QuickRange quick, ZoneId zone) {
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime startToday = now.toLocalDate().atStartOfDay(zone);
        return switch (quick) {
            case TODAY -> new Instant[]{ startToday.toInstant(), startToday.plusDays(1).toInstant() };
            case YESTERDAY -> new Instant[]{ startToday.minusDays(1).toInstant(), startToday.toInstant() };
            case WEEK -> new Instant[]{ startToday.minusDays(6).toInstant(), startToday.plusDays(1).toInstant() }; // последние 7 дней включая сегодня
            case MONTH -> {
                LocalDate firstOfMonth = LocalDate.of(now.getYear(), now.getMonth(), 1);
                ZonedDateTime startMonth = firstOfMonth.atStartOfDay(zone);
                ZonedDateTime startNextMonth = startMonth.plusMonths(1);
                yield new Instant[]{ startMonth.toInstant(), startNextMonth.toInstant() };
            }
        };
    }
}