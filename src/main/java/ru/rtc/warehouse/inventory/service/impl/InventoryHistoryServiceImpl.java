package ru.rtc.warehouse.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryCreateRequest;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryUpdateRequest;
import ru.rtc.warehouse.inventory.mapper.InventoryHistoryMapper;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus.InventoryHistoryStatusCode;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.inventory.service.InventoryHistoryEntityService;
import ru.rtc.warehouse.inventory.service.InventoryHistoryService;
import ru.rtc.warehouse.inventory.service.InventoryHistoryStatusService;
import ru.rtc.warehouse.inventory.service.adapter.IHProductEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.adapter.IHRobotEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryGroupedDTO;
import ru.rtc.warehouse.inventory.service.product.dto.LowStockProductDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryHistoryServiceImpl implements InventoryHistoryService {

	private final InventoryHistoryEntityService ihes;
	private final InventoryHistoryStatusService ihss;
	private final InventoryHistoryMapper ihMapper;
	private final IHRobotEntServiceAdapter robotAdapter;
	private final IHProductEntServiceAdapter productAdapter;
	private final InventoryHistoryRepository inventoryHistoryRepository;

	public void save(InventoryHistoryCreateRequest request) {
		InventoryHistory inventoryHistory = ihMapper.toEntity(request);
		ihes.save(inventoryHistory);
	}

	public void update(InventoryHistoryUpdateRequest request, Long id) {
		InventoryHistory inventoryHistory = ihes.findById(id);

		String robotCode = request.getRobotCode();
		String productCode = request.getProductCode();
		Integer quantity = request.getQuantity();
		Integer zone = request.getZone();
		Integer rowNumber = request.getRowNumber();
		Integer shelfNumber = request.getShelfNumber();
		InventoryHistoryStatusCode status = InventoryHistoryStatusCode.from(String.valueOf(request.getStatus()));
		LocalDateTime scannedAt = request.getScannedAt();

		if (robotCode != null) inventoryHistory.setRobot(robotAdapter.findByCode(robotCode));
		if (productCode != null) inventoryHistory.setProduct(productAdapter.findByCode(productCode));
		if (quantity != null) inventoryHistory.setQuantity(quantity);
		if (zone != null) inventoryHistory.getLocation().setZone(zone);
		if (rowNumber != null) inventoryHistory.getLocation().setRow(rowNumber);
		if (shelfNumber != null) inventoryHistory.getLocation().setShelf(shelfNumber);
		if (status != null) inventoryHistory.setStatus(ihss.findByCode(status));
		if (scannedAt != null) inventoryHistory.setScannedAt(scannedAt);

		ihes.update(inventoryHistory);
	}

	@Transactional(readOnly = true)
	public List<InventoryHistoryDTO> findAll() {
		return ihMapper.toDtoList(ihes.findAll());
	}

	@Transactional(readOnly = true)
	public InventoryHistoryDTO findById(Long id) {
		return ihMapper.toDto(ihes.findById(id));
	}

	public void delete(Long id) {
		ihes.delete(id);
	}

	@Override
	@Transactional(readOnly = true)
	public InventoryHistory findLatestByProductId(Long productId) {
		log.info("Finding latest inventory history for product ID: {}", productId);
		try {
			Optional<InventoryHistory> history = inventoryHistoryRepository.findLatestByProductId(productId);
			return history.orElse(null);
		} catch (Exception e) {
			log.error("Error finding latest inventory history for product: {}", productId, e);
			throw new RuntimeException("Failed to retrieve inventory history", e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<Object[]> findAggregatedDailyInventory(Long warehouseId, List<String> skuCodes,
													   LocalDateTime startDate, LocalDateTime endDate) {
		log.info("Finding aggregated daily inventory for warehouse: {}, period: {} to {}",
				warehouseId, startDate, endDate);
		try {
			List<Object[]> results = inventoryHistoryRepository.findAggregatedDailyInventory(
					warehouseId, skuCodes, startDate, endDate);
			log.info("Found {} days of aggregated inventory data", results.size());
			return results;
		} catch (Exception e) {
			log.error("Error finding aggregated daily inventory for warehouse: {}", warehouseId, e);
			throw new RuntimeException("Failed to retrieve aggregated inventory data", e);
		}
	}

	@Override
	public List<InventoryHistoryDTO> findAllByWarehouseCodeAndProductCode(String warehouseCode, String productCode) {
		return ihMapper.toDtoList(inventoryHistoryRepository.findAllByWarehouseCodeAndProductSkuCode(warehouseCode, productCode));
	}

	@Override
	public List<InventoryHistoryGroupedDTO> findAllByWarehouseCodeAndProductCodes(String warehouseCode, List<String> productCodes) {
		List<InventoryHistory> histories = inventoryHistoryRepository.findAllByWarehouseCodeAndProductCodes(warehouseCode, productCodes);
		List<InventoryHistoryDTO> dtoList = ihMapper.toDtoList(histories);

		return dtoList.stream()
				.collect(Collectors.groupingBy(InventoryHistoryDTO::getSkuCode))
				.entrySet()
				.stream()
				.map(entry -> new InventoryHistoryGroupedDTO(entry.getKey(), entry.getValue()))
				.toList();
	}

	@Override
	public List<LowStockProductDTO> findLowStockProducts(String warehouseCode) {
		return inventoryHistoryRepository.findLowStockProductsByWarehouse(warehouseCode);
	}


}