package ru.rtc.warehouse.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.rtc.warehouse.inventory.service.csv.InventoryImportService;

@Slf4j
@RestController
@RequestMapping("/api/{warehouseCode}/inventory")
@RequiredArgsConstructor
public class InventoryImportController {

	private final InventoryImportService inventoryImportService;

	@Operation(summary = "Импорт инвентаря из CSV файла")
	@PostMapping(value = "/import/csv", consumes = "multipart/form-data")
	public ResponseEntity<?> importInventoryFromCsv(
			@Parameter(description = "CSV файл с данными инвентаря", required = true, schema = @Schema(type = "string", format = "binary"))
			@RequestPart("file") MultipartFile file,
			@PathVariable String warehouseCode) {

		try {
			inventoryImportService.importInventoryFromCsv(file, warehouseCode);
			return ResponseEntity.status(HttpStatus.CREATED).build();
		} catch (Exception e) {
			log.error("Ошибка импорта инвентаря для склада: {}", warehouseCode, e);
			return ResponseEntity.internalServerError().build();
		}
	}
}