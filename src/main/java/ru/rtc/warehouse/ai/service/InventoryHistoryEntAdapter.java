package ru.rtc.warehouse.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.service.InventoryHistoryEntityService;
import ru.rtc.warehouse.product.model.ProductWarehouse;
import ru.rtc.warehouse.product.service.ProductWarehouseEntityService;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InventoryHistoryEntAdapter {

	private final InventoryHistoryEntityService inventoryHistoryEntityService;
	private final ProductWarehouseEntityService productWarehouseEntityService;
	private final WarehouseEntityService warehouseEntityService;

	public InventoryHistory findByProductSKU(String sku, String warehouseCode) {
		return inventoryHistoryEntityService.findByProductSKU(sku, warehouseCode);
	}

	public Optional<Integer> getMinStockForProduct(String sku, String warehouseCode) {
		try {
			InventoryHistory inventoryHistory = findByProductSKU(sku, warehouseCode);
			Long warehouseId = warehouseEntityService.findByCode(warehouseCode).getId();
			Long productId = inventoryHistory.getProduct().getId();

			return productWarehouseEntityService
					.findActiveByProductAndWarehouse(productId, warehouseId)
					.map(ProductWarehouse::getMinStock);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public Optional<Integer> getOptimalStockForProduct(String sku, String warehouseCode) {
		try {
			InventoryHistory inventoryHistory = findByProductSKU(sku, warehouseCode);
			Long warehouseId = warehouseEntityService.findByCode(warehouseCode).getId();
			Long productId = inventoryHistory.getProduct().getId();

			return productWarehouseEntityService
					.findActiveByProductAndWarehouse(productId, warehouseId)
					.map(ProductWarehouse::getOptimalStock);
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}