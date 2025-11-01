package ru.rtc.warehouse.inventory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

@Service
@RequiredArgsConstructor
public class IHWarehouseEntServiceAdapter {

	private final WarehouseEntityService warehouseEntityService;

	public Warehouse validateAndGetWarehouse(String warehouseCode) {
		return warehouseEntityService.validateAndGetWarehouse(warehouseCode);
	}
}
