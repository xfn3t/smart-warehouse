package ru.rtc.warehouse.inventory.service.csv;

import org.springframework.web.multipart.MultipartFile;

public interface InventoryImportService {
	void importInventoryFromCsv(MultipartFile file, String warehouseCode);
}