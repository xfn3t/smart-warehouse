package ru.rtc.warehouse.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.service.InventoryHistoryEntityService;

@Service
@RequiredArgsConstructor
public class InventoryHistoryEntAdapter {

	private final InventoryHistoryEntityService ihes;

	public InventoryHistory findByProductSKU(String sku, String warehouseCode) {
		return ihes.findByProductSKU(sku, warehouseCode);
	}

}
