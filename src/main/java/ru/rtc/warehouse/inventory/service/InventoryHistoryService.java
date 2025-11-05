package ru.rtc.warehouse.inventory.service;

import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryCreateRequest;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryUpdateRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryGroupedDTO;
import ru.rtc.warehouse.inventory.service.product.dto.LowStockProductDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryHistoryService {
	void save(InventoryHistoryCreateRequest request);
	void update(InventoryHistoryUpdateRequest request, Long id);
	List<InventoryHistoryDTO> findAll();
	InventoryHistoryDTO findById(Long id);
	void delete(Long id);

	InventoryHistory findLatestByProductId(Long id);

	List<Object[]> findAggregatedDailyInventory(Long warehouseId, List<String> skuCodes, LocalDateTime localDateTime, LocalDateTime localDateTime1);

	List<InventoryHistoryDTO> findAllByWarehouseCodeAndProductCode(String warehouseCode, String productCode);

	List<InventoryHistoryGroupedDTO> findAllByWarehouseCodeAndProductCodes(String warehouseCode, List<String> productCodes);

	List<LowStockProductDTO> findLowStockProducts(String warehouseCode);
}
