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
import ru.rtc.warehouse.inventory.service.csv.CsvProcessingService;
import ru.rtc.warehouse.inventory.service.InventoryHistoryService;
import ru.rtc.warehouse.inventory.service.product.ProductLastInventoryService;

@Slf4j
@RestController
@RequestMapping("/api/{warehouseCode}/inventory")
@RequiredArgsConstructor
public class InventoryController {

	private final CsvProcessingService csvProcessingService;
	private final InventoryHistoryService ihs;
	private final ProductLastInventoryService productLastInventoryService;


	@Operation(summary = "Upload CSV file for inventory")
	@PostMapping(value = "/upload-csv", consumes = "multipart/form-data")
	public ResponseEntity<?> uploadCsvFile(
			@Parameter(description = "CSV file to upload",
					required = true,
					schema = @Schema(type = "string", format = "binary"))
			@RequestPart("file") MultipartFile file,
			@PathVariable String warehouseCode) {
		try {
			ihs.saveCsv(file, warehouseCode);
			return ResponseEntity.status(HttpStatus.CREATED).build();
		} catch (Exception e) {
			log.error("File upload failed", e);
			return ResponseEntity.internalServerError()
					.body("Error processing file: " + e.getMessage());
		}
	}


}