package ru.rtc.warehouse.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.service.InventoryHistorySummaryService;
import ru.rtc.warehouse.inventory.service.dto.HistorySummaryDTO;
import ru.rtc.warehouse.inventory.util.QuickRangeResolver;

import java.time.Instant;
import java.time.ZoneId;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/inventory/history")
@RequiredArgsConstructor
public class InventoryHistorySummaryController {

    private final InventoryHistorySummaryService summaryService;

    @Operation(summary = "Сводная статистика по историческим данным",
            description = "Фильтры как у /api/inventory/history. " +
                    "Если заданы from/to — quick игнорируется.",
            responses = @ApiResponse(responseCode = "200",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = HistorySummaryDTO.class))))
    @GetMapping("/summary")
    public ResponseEntity<HistorySummaryDTO> summary(@ParameterObject InventoryHistorySearchRequest rq) {

        // запрет смешивания quick и from/to
        if (rq != null && rq.getQuick() != null && (rq.getFrom() != null || rq.getTo() != null)) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Укажите либо quick, либо from/to — одновременно нельзя.");
        }

        // применяем quick -> from/to
        if (rq != null && rq.getFrom() == null && rq.getTo() == null && rq.getQuick() != null) {
            var zone = ZoneId.systemDefault(); // ← Исправлено: ZoneId вместо TimeZone
            var range = QuickRangeResolver.resolve(rq.getQuick(), zone);
            rq.setFrom(range[0]);
            rq.setTo(range[1]);
        }

        // валидация диапазона
        if (rq != null && rq.getFrom() != null && rq.getTo() != null) {
            Instant from = rq.getFrom(), to = rq.getTo();
            if (!from.isBefore(to)) {
                throw new ResponseStatusException(BAD_REQUEST, "'from' must be < 'to'");
            }
        }

        return ResponseEntity.ok(summaryService.summarize(rq));
    }
}