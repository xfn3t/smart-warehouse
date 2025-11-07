package ru.rtc.warehouse.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.inventory.service.InventoryHistoryEntityService;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
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

	@Override
	public List<InventoryHistory> findAllByWarehouseCodeAndProductCode(String warehouseCode, String productCode) {
		return inventoryHistoryRepository.findAllByWarehouseCodeAndProductSkuCode(warehouseCode, productCode);
	}

	@Override
	public long countByWarehouseAndScannedAtBetween(Warehouse warehouse, LocalDateTime todayStart, LocalDateTime todayEnd) {
		return inventoryHistoryRepository.countByWarehouseAndScannedAtBetween(warehouse, todayStart, todayEnd);
	}

	@Override
	public long countByWarehouseAndStatusAndScannedAtAfter(Warehouse warehouse, InventoryHistoryStatus.InventoryHistoryStatusCode inventoryHistoryStatusCode, LocalDateTime last24Hours) {
		return inventoryHistoryRepository.countByWarehouseAndStatusAndScannedAtAfter(warehouse, inventoryHistoryStatusCode, last24Hours);
	}

	@Override
	public boolean existsByLocationAndScannedAtAfter(Location location, LocalDateTime since) {
		return inventoryHistoryRepository.existsByLocationAndScannedAtAfter(location, since);
	}

	@Transactional(readOnly = true)
	public InventoryHistory findLatestByProductId(Long productId) {
		log.info("Finding latest inventory history for product ID: {}", productId);

		try {
			Optional<InventoryHistory> history = inventoryHistoryRepository.findLatestByProductId(productId);

			if (history.isPresent()) {
				log.debug("Found inventory history for product {}: quantity={}, expected={}",
						productId, history.get().getQuantity(), history.get().getExpectedQuantity());
				return history.get();
			} else {
				log.debug("No inventory history found for product: {}", productId);
				return null;
			}
		} catch (Exception e) {
			log.error("Error finding latest inventory history for product: {}", productId, e);
			throw new RuntimeException("Failed to retrieve inventory history", e);
		}
	}

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

	@Transactional(readOnly = true)
	public List<Object[]> findScanCountsByRobotAndPeriod(LocalDateTime startDate, LocalDateTime endDate) {
		log.info("Finding scan counts by robot for period: {} to {}", startDate, endDate);

		try {
			List<Object[]> results = inventoryHistoryRepository.findScanCountsByRobotAndPeriod(startDate, endDate);

			log.info("Found scan counts for {} robots", results.size());
			return results;

		} catch (Exception e) {
			log.error("Error finding scan counts by robot for period: {} to {}", startDate, endDate, e);
			throw new RuntimeException("Failed to retrieve scan counts", e);
		}
	}

	@Transactional(readOnly = true)
	public List<InventoryHistory> findByProductId(Long productId) {
		log.info("Finding all inventory history for product ID: {}", productId);

		try {

			return inventoryHistoryRepository.findAll().stream()
					.filter(history -> history.getProduct().getId().equals(productId))
					.filter(history -> !history.isDeleted())
					.collect(java.util.stream.Collectors.toList());
		} catch (Exception e) {
			log.error("Error finding inventory history for product: {}", productId, e);
			throw new RuntimeException("Failed to retrieve inventory history by product", e);
		}
	}
}
