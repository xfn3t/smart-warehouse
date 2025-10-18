package ru.rtc.warehouse.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.inventory.common.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryCreateRequest;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryUpdateRequest;
import ru.rtc.warehouse.inventory.mapper.InventoryHistoryMapper;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.service.InventoryHistoryEntityService;
import ru.rtc.warehouse.inventory.service.InventoryHistoryService;
import ru.rtc.warehouse.inventory.service.ProductEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.RobotEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryHistoryServiceImpl implements InventoryHistoryService {

	private final InventoryHistoryEntityService ihes;
	private final InventoryHistoryMapper ihMapper;

	private final RobotEntServiceAdapter robotAdapter;
	private final ProductEntServiceAdapter productAdapter;

	public void save(InventoryHistoryCreateRequest request) {
		InventoryHistory inventoryHistory = ihMapper.toEntity(request);
		ihes.save(inventoryHistory);
	}

	public void update(InventoryHistoryUpdateRequest request, Long id) {

		InventoryHistory inventoryHistory = ihes.findById(id);

		String robotCode = request.getRobotCode();
		String productCode = request.getProductCode();
		Integer quantity = request.getQuantity();
		String zone = request.getZone();
		Integer rowNumber = request.getRowNumber();
		Integer shelfNumber = request.getShelfNumber();
		InventoryHistoryStatus status = request.getStatus();
		LocalDateTime scannedAt = request.getScannedAt();

		if (robotCode != null) inventoryHistory.setRobot(robotAdapter.findByCode(robotCode));
		if (productCode != null) inventoryHistory.setProduct(productAdapter.findByCode(productCode));
		if (quantity != null) inventoryHistory.setQuantity(quantity);
		if (zone != null) inventoryHistory.setZone(zone);
		if (rowNumber != null) inventoryHistory.setShelfNumber(shelfNumber);
		if (status != null) inventoryHistory.setStatus(status);
		if (scannedAt != null) inventoryHistory.setScannedAt(scannedAt);

		ihes.update(inventoryHistory);
	}

	public List<InventoryHistoryDTO> findAll() {
		return ihMapper.toDtoList(ihes.findAll());
	}

	public InventoryHistoryDTO findById(Long id) {
		return ihMapper.toDto(ihes.findById(id));
	}

	public void delete(Long id) {
		ihes.delete(id);
	}

}
