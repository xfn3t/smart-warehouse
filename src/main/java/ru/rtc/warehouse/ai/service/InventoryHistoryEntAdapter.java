package ru.rtc.warehouse.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.service.InventoryHistoryEntityService;
import ru.rtc.warehouse.product.model.ProductWarehouse;
import ru.rtc.warehouse.product.service.ProductWarehouseEntityService;

@Component
@RequiredArgsConstructor
public class InventoryHistoryEntAdapter {

	private final InventoryHistoryEntityService inventoryHistoryEntityService;
	private final ProductWarehouseEntityService productWarehouseEntityService;

	public InventoryHistory findLatestInventoryData(String sku, String warehouseCode) {
		return inventoryHistoryEntityService.findLatestBySkuAndWarehouseCode(sku, warehouseCode);
	}

	public ProductWarehouse findProductWarehouseData(String sku, String warehouseCode) {
		return productWarehouseEntityService.findBySkuAndWarehouseCode(sku, warehouseCode);
	}
}