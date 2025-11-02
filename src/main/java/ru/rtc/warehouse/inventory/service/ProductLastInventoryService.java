package ru.rtc.warehouse.inventory.service;

import org.springframework.data.domain.Pageable;
import ru.rtc.warehouse.inventory.controller.dto.request.ProductLastInventorySearchRequest;
import ru.rtc.warehouse.inventory.service.dto.ProductLastInventoryPageDTO;

public interface ProductLastInventoryService {
	ProductLastInventoryPageDTO getLastInventoryByWarehouse(String warehouseCode,
															ProductLastInventorySearchRequest searchRequest,
															Pageable pageable);
}