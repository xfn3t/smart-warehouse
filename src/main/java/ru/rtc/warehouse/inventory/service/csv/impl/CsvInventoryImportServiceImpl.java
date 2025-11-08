package ru.rtc.warehouse.inventory.service.csv.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.rtc.warehouse.exception.InventoryImportException;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryCsvDto;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.inventory.service.InventoryHistoryStatusService;
import ru.rtc.warehouse.inventory.service.adapter.IHLocationEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.adapter.IHProductEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.adapter.IHWarehouseEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.csv.CsvProcessingService;
import ru.rtc.warehouse.inventory.service.csv.InventoryImportService;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.model.ProductWarehouse;
import ru.rtc.warehouse.product.service.ProductWarehouseEntityService;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvInventoryImportServiceImpl implements InventoryImportService {

	private final InventoryHistoryRepository inventoryHistoryRepository;
	private final IHWarehouseEntServiceAdapter warehouseService;
	private final IHLocationEntServiceAdapter locationService;
	private final IHProductEntServiceAdapter productService;
	private final ProductWarehouseEntityService productWarehouseEntityService;
	private final InventoryHistoryStatusService inventoryHistoryStatusService;
	private final CsvProcessingService csvProcessingService;

	@Override
	@Transactional
	public void importInventoryFromCsv(MultipartFile file, String warehouseCode) {
		log.info("Импорт инвентаря из CSV для склада: {}", warehouseCode);

		Warehouse warehouse = warehouseService.validateAndGetWarehouse(warehouseCode);
		List<InventoryCsvDto> csvRecords = csvProcessingService.parseCsvFile(file);

		for (InventoryCsvDto record : csvRecords) {
			processInventoryRecord(record, warehouse);
		}

		log.info("Успешно импортировано {} позиций инвентаря для склада: {}", csvRecords.size(), warehouseCode);
	}

	private void processInventoryRecord(InventoryCsvDto record, Warehouse warehouse) {
		try {
			validateInventoryRecord(record);

			Location location = locationService.findByCoordinate(
					record.getZone(), record.getRow(), record.getShelf(), warehouse.getId()
			);

			if (location == null) {
				throw new InventoryImportException("Локация не найдена: зона=" + record.getZone() +
						", ряд=" + record.getRow() + ", полка=" + record.getShelf());
			}

			// Ищем продукт по имени и категории или создаем новый
			Product product = findOrCreateProduct(record, warehouse);

			// Получаем параметры склада для продукта
			ProductWarehouse productWarehouse = findOrCreateProductWarehouse(product, warehouse, record);

			// Определяем статус инвентаризации
			InventoryHistoryStatus inventoryHistoryStatus = calculateInventoryStatus(
					record.getQuantity(), productWarehouse.getMinStock(), productWarehouse.getOptimalStock());

			// Создаем запись в истории инвентаризации
			InventoryHistory inventoryHistory = createInventoryHistory(record, warehouse, location, product, inventoryHistoryStatus);
			inventoryHistoryRepository.save(inventoryHistory);

		} catch (Exception e) {
			log.error("Ошибка обработки записи инвентаря: {}", record, e);
			throw new InventoryImportException("Ошибка обработки записи инвентаря: " + e.getMessage(), e);
		}
	}

	private Product findOrCreateProduct(InventoryCsvDto record, Warehouse warehouse) {
		// Пытаемся найти продукт по SKU (если он был сгенерирован ранее)
		String generatedSku = generateSkuCode(record.getName(), record.getCategory(), warehouse.getCode());

		try {
			return productService.findByCode(generatedSku);
		} catch (NotFoundException e) {
			// Если не найден - создаем новый
			Product newProduct = Product.builder()
					.name(record.getName())
					.category(record.getCategory())
					.skuCode(generatedSku)
					.isDeleted(false)
					.build();
			return productService.save(newProduct);
		}
	}

	private ProductWarehouse findOrCreateProductWarehouse(Product product, Warehouse warehouse, InventoryCsvDto record) {

		try {
			ProductWarehouse productWarehouse = productWarehouseEntityService
					.findActiveByProductAndWarehouse(product.getId(), warehouse.getId());
			// Обновляем параметры если нужно
			if (!productWarehouse.getMinStock().equals(record.getMinStock()) ||
					!productWarehouse.getOptimalStock().equals(record.getOptimalStock())) {
				productWarehouse.setMinStock(record.getMinStock());
				productWarehouse.setOptimalStock(record.getOptimalStock());
				return productWarehouseEntityService.update(productWarehouse);
			}
			return productWarehouse;

		} catch (NotFoundException e){
			// Создаем новую связь продукт-склад
			ProductWarehouse newProductWarehouse = ProductWarehouse.builder()
					.product(product)
					.warehouse(warehouse)
					.minStock(record.getMinStock())
					.optimalStock(record.getOptimalStock())
					.createdAt(LocalDateTime.now())
					.isDeleted(false)
					.build();
			return productWarehouseEntityService.save(newProductWarehouse);
		}
	}

	private String generateSkuCode(String name, String category, String warehouseCode) {
		// Генерируем SKU на основе имени, категории и склада
		String base = (name.substring(0, Math.min(3, name.length())) +
				(category != null ? category.substring(0, Math.min(3, category.length())) : "GEN"));
		String cleanBase = base.toUpperCase().replaceAll("[^A-Z0-9]", "");
		return String.format("%s-%s", cleanBase, warehouseCode);
	}

	private InventoryHistoryStatus calculateInventoryStatus(Integer quantity, Integer minStock, Integer optimalStock) {
		if (quantity <= minStock) {
			return inventoryHistoryStatusService.findByCode(InventoryHistoryStatus.InventoryHistoryStatusCode.CRITICAL);
		}
		if (quantity < optimalStock) {
			return inventoryHistoryStatusService.findByCode(InventoryHistoryStatus.InventoryHistoryStatusCode.LOW_STOCK);
		}
		return inventoryHistoryStatusService.findByCode(InventoryHistoryStatus.InventoryHistoryStatusCode.OK);
	}

	private void validateInventoryRecord(InventoryCsvDto record) {
		if (record.getName() == null || record.getName().trim().isEmpty()) {
			throw new InventoryImportException("Название продукта не может быть пустым");
		}
		if (record.getQuantity() == null || record.getQuantity() < 0) {
			throw new InventoryImportException("Количество не может быть отрицательным");
		}
		if (record.getZone() == null || record.getRow() == null || record.getShelf() == null) {
			throw new InventoryImportException("Неверный формат локации");
		}
		if (record.getMinStock() == null || record.getOptimalStock() == null) {
			throw new InventoryImportException("minStock и optimalStock обязательны");
		}
	}

	private InventoryHistory createInventoryHistory(InventoryCsvDto record, Warehouse warehouse,
													Location location, Product product, InventoryHistoryStatus status) {
		return InventoryHistory.builder()
				.messageId(UUID.randomUUID())
				.warehouse(warehouse)
				.location(location)
				.product(product)
				.quantity(record.getQuantity())
				.expectedQuantity(record.getQuantity()) // В CSV импорте expected = actual
				.difference(0)
				.status(status)
				.scannedAt(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.isDeleted(false)
				.build();
	}
}