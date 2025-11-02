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
import ru.rtc.warehouse.inventory.controller.dto.request.ProductLastInventorySearchRequest;
import ru.rtc.warehouse.inventory.service.InventoryHistoryQueryService;
import ru.rtc.warehouse.inventory.service.InventoryHistoryService;
import ru.rtc.warehouse.inventory.service.ProductLastInventoryService;
import ru.rtc.warehouse.inventory.service.dto.HistoryPageDTO;
import ru.rtc.warehouse.inventory.service.dto.LowStockProductDTO;
import ru.rtc.warehouse.inventory.service.dto.ProductLastInventoryPageDTO;

import java.util.List;

@RestController
@RequestMapping("/api/{warehouseCode}/inventory/history")
@Tag(name = "Inventory History")
@RequiredArgsConstructor
public class InventoryHistoryQueryController {

    private final InventoryHistoryQueryService queryService;
    private final InventoryHistoryService ihs;
    private final ProductLastInventoryService lastInventoryService;

    @Operation(summary = "Поиск по историческим данным (все записи)")
    @GetMapping
    public ResponseEntity<HistoryPageDTO> findHistory(
            @PathVariable String warehouseCode,
            @ParameterObject @Valid InventoryHistorySearchRequest searchRequest,
            @ParameterObject @Valid InventoryHistoryPageRequest pageRequest) {

        HistoryPageDTO result = queryService.search(warehouseCode, searchRequest, pageRequest.toPageable());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Получить последние записи инвентаризации по продуктам")
    @GetMapping("/last")
    public ResponseEntity<ProductLastInventoryPageDTO> getLastInventory(
            @PathVariable String warehouseCode,
            @ParameterObject @Valid ProductLastInventorySearchRequest searchRequest,
            @ParameterObject @Valid InventoryHistoryPageRequest pageRequest) {

        ProductLastInventoryPageDTO result = lastInventoryService.getLastInventoryByWarehouse(
                warehouseCode, searchRequest, pageRequest.toPageable());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/bySkus")
    public ResponseEntity<?> getByProductCodes(
            @PathVariable String warehouseCode,
            @RequestParam List<String> productCodes) {
        return ResponseEntity.ok(
                ihs.findAllByWarehouseCodeAndProductCodes(warehouseCode, productCodes)
        );
    }

    @Operation(summary = "Список товаров с количеством ниже минимального запаса (minStock)")
    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockProductDTO>> getLowStockProducts(
            @PathVariable String warehouseCode) {
        return ResponseEntity.ok(ihs.findLowStockProducts(warehouseCode));
    }

}