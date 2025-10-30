package ru.rtc.warehouse.inventory.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;


/**
 * Ответ страницы истории: total/page/size + элементы.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryPageDTO {
    @Schema(description = "Общее количество записей по фильтрам")
    private long total;
    @Schema(description = "Номер страницы (0-based)")
    private int page;
    @Schema(description = "Размер страницы")
    private int size;
    private java.util.List<InventoryHistoryDTO> items;
}
