package ru.rtc.warehouse.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryCsvDto;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryCreateRequest;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistoryUpdateRequest;
import ru.rtc.warehouse.inventory.mapper.InventoryHistoryMapper;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus.InventoryHistoryStatusCode;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.inventory.service.*;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryGroupedDTO;
import ru.rtc.warehouse.inventory.service.dto.LowStockProductDTO;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryHistoryServiceImpl implements InventoryHistoryService {

	private final InventoryHistoryEntityService ihes;
	private final InventoryHistoryStatusService ihss;
	private final InventoryHistoryMapper ihMapper;
	private final CsvProcessingService csvService;
	private final IHWarehouseEntServiceAdapter warehouseService;
	private final IHLocationEntServiceAdapter locationService;
	private final RobotEntServiceAdapter robotAdapter;
	private final ProductEntServiceAdapter productAdapter;
	private final InventoryHistoryRepository inventoryHistoryRepository;

	@Override
	@Transactional
	public void saveCsv(MultipartFile multipartFile, String warehouseCode) {
		List<InventoryCsvDto> inventoryCsvDtos = csvService.parseCsvFile(multipartFile);
		Warehouse warehouse = warehouseService.findByCode(warehouseCode);

		// Мапа для подсчета товаров в каждой ячейке
		Map<String, AtomicInteger> locationCounter = new HashMap<>();

		for (InventoryCsvDto dto : inventoryCsvDtos) {
			// Находим или создаем локацию
			Location location = locationService.findByCoordinate(
					dto.getZone(),
					dto.getRow(),
					dto.getShelf(),
					warehouse.getId()
			);

			// Генерируем ключ для локации
			String locationKey = String.format("%d-%d-%d-%d",
					dto.getZone(), dto.getRow(), dto.getShelf(), warehouse.getId());

			// Получаем или создаем счетчик для этой локации
			AtomicInteger counter = locationCounter.computeIfAbsent(locationKey, k -> {
				// Ищем существующие товары в этой локации для определения стартового номера
				int existingCount = countProductsInLocation(location, warehouse);
				return new AtomicInteger(existingCount);
			});

			// Генерируем SKU код
			String skuCode = generateSkuCode(dto.getZone(), dto.getRow(), dto.getShelf(),
					warehouse.getCode(), counter.incrementAndGet());

			// Создаем продукт
			Product product = Product.builder()
					.name(dto.getName())
					.category(dto.getCategory())
					.minStock(dto.getMinStock())
					.optimalStock(dto.getOptimalStock())
					.code(skuCode) // Используем сгенерированный SKU
					.build();
			product = productAdapter.save(product);

			// Определяем статус инвентаризации
			InventoryHistoryStatus inventoryHistoryStatus = ihss.findByCode(getInventoryHistoryStatusCode(dto));

			// Создаем запись в истории инвентаризации
			InventoryHistory inventoryHistory = InventoryHistory.builder()
					.status(inventoryHistoryStatus)
					.scannedAt(LocalDateTime.now())
					.createdAt(LocalDateTime.now())
					.warehouse(warehouse)
					.product(product)
					.quantity(dto.getQuantity())
					.expectedQuantity(dto.getQuantity())
					.difference(0)
					.location(location)
					.robot(null)
					.build();

			ihes.save(inventoryHistory);
		}
	}

	private String generateSkuCode(Integer zone, Integer row, Integer shelf, String warehouseCode, int itemNumber) {
		// Формат: SKU111-WH002-02
		// Где: SKU - префикс, 111 - zone+row+shelf, WH002 - код склада, 02 - номер в ячейке
		String locationPart = String.format("%d%d%d", zone, row, shelf);
		String warehousePart = warehouseCode.startsWith("WH") ? warehouseCode : "WH" + warehouseCode;
		String itemPart = String.format("%02d", itemNumber);

		return String.format("SKU%s-%s-%s", locationPart, warehousePart, itemPart);
	}

	private InventoryHistoryStatusCode getInventoryHistoryStatusCode(InventoryCsvDto dto) {
		if (dto.getQuantity() <= dto.getMinStock()) {
			return InventoryHistoryStatusCode.CRITICAL;
		}
		if (dto.getQuantity() < dto.getOptimalStock()) {
			return InventoryHistoryStatusCode.LOW_STOCK;
		}
		return InventoryHistoryStatusCode.OK;
	}

	// Остальные методы остаются без изменений
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
	public int countProductsInLocation(Location location, Warehouse warehouse) {
		return inventoryHistoryRepository.countByLocationAndWarehouse(location, warehouse);
	}

	@Override
	public List<InventoryHistoryDTO> findAllByWarehouseCodeAndProductCode(String warehouseCode, String productCode) {
		return ihMapper.toDtoList(inventoryHistoryRepository.findAllByWarehouseCodeAndProductCode(warehouseCode, productCode));
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