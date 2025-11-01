package ru.rtc.warehouse.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryPageRequest;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.service.InventoryHistoryQueryService;
import ru.rtc.warehouse.inventory.service.dto.HistoryPageDTO;

@RestController
@RequestMapping("/api/{warehouseCode}/inventory/history")
@Tag(name = "Inventory History")
@RequiredArgsConstructor
public class InventoryHistoryQueryController {

    private final InventoryHistoryQueryService queryService;

    @Operation(summary = "Поиск по историческим данным")
    @GetMapping
    public ResponseEntity<HistoryPageDTO> findHistory(
            @PathVariable String warehouseCode,
            @ParameterObject @Valid InventoryHistorySearchRequest searchRequest,
            @ParameterObject @Valid InventoryHistoryPageRequest pageRequest) {

        HistoryPageDTO result = queryService.search(warehouseCode, searchRequest, pageRequest.toPageable());
        return ResponseEntity.ok(result);
    }
}