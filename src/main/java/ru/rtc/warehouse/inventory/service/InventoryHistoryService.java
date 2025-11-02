package ru.rtc.warehouse.inventory.service;

import org.springframework.web.multipart.MultipartFile;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryCreateRequest;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryUpdateRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryGroupedDTO;
import ru.rtc.warehouse.inventory.service.dto.LowStockProductDTO;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryHistoryService {
	void save(InventoryHistoryCreateRequest request);
	void saveCsv(MultipartFile multipartFile, String warehouseCode);
	void update(InventoryHistoryUpdateRequest request, Long id);
	List<InventoryHistoryDTO> findAll();
	InventoryHistoryDTO findById(Long id);
	void delete(Long id);

	InventoryHistory findLatestByProductId(Long id);

	List<Object[]> findAggregatedDailyInventory(Long warehouseId, List<String> skuCodes, LocalDateTime localDateTime, LocalDateTime localDateTime1);

	int countProductsInLocation(Location location, Warehouse warehouse);

	List<InventoryHistoryDTO> findAllByWarehouseCodeAndProductCode(String warehouseCode, String productCode);

	List<InventoryHistoryGroupedDTO> findAllByWarehouseCodeAndProductCodes(String warehouseCode, List<String> productCodes);

	List<LowStockProductDTO> findLowStockProducts(String warehouseCode);
}
