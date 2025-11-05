package ru.rtc.warehouse.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rtc.warehouse.common.aspect.RequiresOwnership;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.service.InventoryHistorySummaryService;
import ru.rtc.warehouse.inventory.service.dto.HistorySummaryDTO;

@RestController
@RequestMapping("/api/{warehouseCode}/inventory/history")
@RequiredArgsConstructor
@RequiresOwnership(codeParam = "warehouseCode", entityType = RequiresOwnership.EntityType.WAREHOUSE)
public class InventoryHistorySummaryController {

    private final InventoryHistorySummaryService summaryService;

    @Operation(summary = "Сводная статистика по историческим данным",
            description = "Фильтры как у /api/inventory/history. " +
                    "Если заданы from/to — quick игнорируется.",
            responses = @ApiResponse(responseCode = "200",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = HistorySummaryDTO.class))))
    @GetMapping("/summary")
    public ResponseEntity<HistorySummaryDTO> summary(
            @PathVariable String warehouseCode,
            @ParameterObject @Valid InventoryHistorySearchRequest rq) {

        HistorySummaryDTO result = summaryService.summarize(warehouseCode, rq);
        return ResponseEntity.ok(result);
    }
}