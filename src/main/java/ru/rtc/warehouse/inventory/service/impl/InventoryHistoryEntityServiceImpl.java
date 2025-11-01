package ru.rtc.warehouse.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.inventory.service.InventoryHistoryEntityService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryHistoryEntityServiceImpl implements InventoryHistoryEntityService {

	private final InventoryHistoryRepository inventoryHistoryRepository;

	@Override
	public InventoryHistory save(InventoryHistory inventoryHistory) {
		return inventoryHistoryRepository.save(inventoryHistory);
	}

	@Override
	public InventoryHistory update(InventoryHistory inventoryHistory) {
		return inventoryHistoryRepository.save(inventoryHistory);
	}

	@Override
	public List<InventoryHistory> findAll() {
		return inventoryHistoryRepository.findAll();
	}

	@Override
	public InventoryHistory findById(Long id) {
		return inventoryHistoryRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Inventory history not found"));
	}

	@Override
	public void delete(Long id) {
		inventoryHistoryRepository.deleteById(id);
	}

	@Override
	public InventoryHistory findByProductSKU(String sku, String warehouseCode) {
		return inventoryHistoryRepository.findByProductSKU(sku, warehouseCode)
				.orElseThrow(() -> new NotFoundException("Inventory history not found"));
	}
}
