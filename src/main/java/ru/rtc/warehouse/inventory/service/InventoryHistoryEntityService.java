package ru.rtc.warehouse.inventory.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryHistoryEntityService extends CrudEntityService<InventoryHistory, Long> {
	InventoryHistory findByProductSKU(String sku, String warehouseCode);
	List<InventoryHistory> findAllByWarehouseCodeAndProductCode(String warehouseCode, String productCode);

	long countByWarehouseAndScannedAtBetween(Warehouse warehouse, LocalDateTime todayStart, LocalDateTime todayEnd);

	long countByWarehouseAndStatusAndScannedAtAfter(Warehouse warehouse, InventoryHistoryStatus.InventoryHistoryStatusCode inventoryHistoryStatusCode, LocalDateTime last24Hours);

	boolean existsByLocationAndScannedAtAfter(Location location, LocalDateTime since);
}
