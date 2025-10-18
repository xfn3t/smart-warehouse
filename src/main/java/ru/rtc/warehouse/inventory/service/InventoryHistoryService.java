package ru.rtc.warehouse.inventory.service;

import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryCreateRequest;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryUpdateRequest;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;

import java.util.List;

public interface InventoryHistoryService {
	void save(InventoryHistoryCreateRequest request);
	void update(InventoryHistoryUpdateRequest request, Long id);
	List<InventoryHistoryDTO> findAll();
	InventoryHistoryDTO findById(Long id);
	void delete(Long id);
}
