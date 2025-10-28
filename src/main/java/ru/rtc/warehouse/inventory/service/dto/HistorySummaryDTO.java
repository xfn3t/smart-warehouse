package ru.rtc.warehouse.inventory.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "Сводная статистика по истории")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistorySummaryDTO {

    @Schema(description = "Всего проверок за период")
    private long total;

    @Schema(description = "Уникальных товаров")
    private long uniqueProducts;

    @Schema(description = "Выявлено расхождений (status != OK)")
    private long discrepancies;

    @Schema(description = "Среднее время инвентаризации записи (мин), " +
            "как avg(created_at - scanned_at). Может быть null, если данных нет.")
    private Double avgZoneScanMinutes;
}
