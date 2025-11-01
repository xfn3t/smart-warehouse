package ru.rtc.warehouse.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.dashboard.dto.RealtimeStatsDTO;
import ru.rtc.warehouse.dashboard.service.RealtimeStatsService;

/**
 * REST-контроллер блока "Статистика в реальном времени".
 */
@RestController
@RequestMapping("/api/{warehouseCode}/realtime")
@RequiredArgsConstructor
public class RealtimeStatsController {

    private final RealtimeStatsService service;

    @Operation(
            summary = "Метрики для блока 'Статистика в реальном времени'",
            description = "Карточки и line-chart за последний час. Обновлять каждые 5 секунд.",
            responses = @ApiResponse(responseCode = "200",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RealtimeStatsDTO.class)))
    )
    @GetMapping("/stats")
    public ResponseEntity<RealtimeStatsDTO> stats(@PathVariable String warehouseCode) {
        return ResponseEntity.ok(service.getStats(warehouseCode));
    }
}
