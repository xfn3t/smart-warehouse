package ru.rtc.warehouse.inventory.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.inventory.model.InventoryHistory;

import java.util.List;

public interface InventoryHistoryEntityService extends CrudEntityService<InventoryHistory, Long> {
	InventoryHistory findByProductSKU(String sku, String warehouseCode);
	List<InventoryHistory> findAllByWarehouseCodeAndProductCode(String warehouseCode, String productCode);
}
