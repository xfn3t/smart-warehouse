package ru.rtc.warehouse.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryStatusRepository;
import ru.rtc.warehouse.inventory.service.InventoryHistoryStatusService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryHistoryStatusServiceImpl implements InventoryHistoryStatusService {

	private final InventoryHistoryStatusRepository repository;

	@Override
	public InventoryHistoryStatus findByCode(InventoryHistoryStatus.InventoryHistoryStatusCode code) {
		return repository.findByCode(code)
				.orElseThrow(() -> new NotFoundException("Inventory history status not found"));
	}

	@Override
	public InventoryHistoryStatus save(InventoryHistoryStatus inventoryHistoryStatus) {
		return repository.save(inventoryHistoryStatus);
	}

	@Override
	public InventoryHistoryStatus update(InventoryHistoryStatus inventoryHistoryStatus) {
		return repository.save(inventoryHistoryStatus);
	}

	@Override
	public List<InventoryHistoryStatus> findAll() {
		return repository.findAll();
	}

	@Override
	public InventoryHistoryStatus findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Inventory history status not found"));
	}

	@Override
	public void delete(Long id) {
		repository.deleteById(id);
	}
}
