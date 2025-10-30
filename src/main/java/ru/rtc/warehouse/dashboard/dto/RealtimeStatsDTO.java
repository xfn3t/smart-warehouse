package ru.rtc.warehouse.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Набор метрик для блока «Статистика в реальном времени».
 * Используется в ответе /api/realtime/stats.
 */
@Schema(description = "Метрики реального времени для дашборда")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealtimeStatsDTO {

    @Schema(description = "Активных роботов")
    private long activeRobots;

    @Schema(description = "Всего роботов")
    private long totalRobots;

    @Schema(description = "Проверено сегодня (количество записей истории)", example = "1234")
    private long checkedToday;

    @Schema(description = "Количество SKU в критическом состоянии (по последнему статусу каждого SKU)", example = "7")
    private long criticalSkus;

    @Schema(description = "Средний заряд батареи роботов, % (null, если данных нет)", example = "76.4")
    private Double avgBatteryPercent;

    @Schema(description = "Серия активности за последний час (по минутам)")
    private List<ActivityPoint> activitySeries;

    @Schema(description = "Момент формирования ответа")
    private LocalDateTime serverTime;

    /**
     * Точка временного ряда активности.
     * Одна точка = одна минута, значение — число сканов в эту минуту.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityPoint {

        @Schema(description = "Начало минуты (локальное)", example = "2025-10-25T15:34:00")
        private LocalDateTime ts;

        @Schema(description = "Количество сканов за минуту", example = "5")
        private long count;
    }
}
