package ru.rtc.warehouse.inventory.service.product.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.rtc.warehouse.inventory.controller.dto.request.ProductLastInventorySearchRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.inventory.service.product.ProductLastInventoryService;
import ru.rtc.warehouse.inventory.service.product.dto.ProductLastInventoryDTO;
import ru.rtc.warehouse.inventory.service.product.dto.ProductLastInventoryPageDTO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductLastInventoryServiceImpl implements ProductLastInventoryService {

	private final InventoryHistoryRepository inventoryHistoryRepository;

	@Override
	@Transactional(readOnly = true)
	public ProductLastInventoryPageDTO getLastInventoryByWarehouse(
			String warehouseCode,
			ProductLastInventorySearchRequest searchRequest,
			Pageable pageable) {

		log.info("Getting last inventory records for warehouse: {}", warehouseCode);

		// Получаем ВСЕ последние записи для каждого продукта
		List<InventoryHistory> allLatest = inventoryHistoryRepository.findLatestByWarehouseGroupedByProduct(warehouseCode);

		log.info("Total latest records from repository: {}", allLatest.size());

		// Логируем для отладки
		debugRecords(allLatest);

		// Применяем фильтры
		List<InventoryHistory> filtered = applyFilters(allLatest, searchRequest);

		log.info("After filtering: {} records", filtered.size());

		// Сортируем
		List<InventoryHistory> sorted = applySorting(filtered, pageable.getSort());

		// Применяем пагинацию
		List<InventoryHistory> paginated = applyPagination(sorted, pageable);

		// Маппим в DTO
		List<ProductLastInventoryDTO> items = paginated.stream()
				.map(this::mapToDTO)
				.collect(Collectors.toList());

		return ProductLastInventoryPageDTO.builder()
				.total((long) filtered.size())
				.page(pageable.getPageNumber())
				.size(pageable.getPageSize())
				.items(items)
				.build();
	}

	private List<InventoryHistory> applyFilters(List<InventoryHistory> records,
												ProductLastInventorySearchRequest searchRequest) {
		if (searchRequest == null) {
			return records;
		}

		return records.stream()
				.filter(history -> filterByCategories(history, searchRequest))
				.filter(history -> filterByStatuses(history, searchRequest))
				.filter(history -> filterBySearchQuery(history, searchRequest))
				.filter(history -> filterByRobots(history, searchRequest))
				.collect(Collectors.toList());
	}

	private boolean filterByCategories(InventoryHistory history, ProductLastInventorySearchRequest searchRequest) {
		if (CollectionUtils.isEmpty(searchRequest.getCategories())) {
			return true;
		}
		return searchRequest.getCategories().contains(history.getProduct().getCategory());
	}

	private boolean filterByStatuses(InventoryHistory history, ProductLastInventorySearchRequest searchRequest) {
		if (CollectionUtils.isEmpty(searchRequest.getStatuses())) {
			return true;
		}
		return searchRequest.getStatuses().contains(history.getStatus().getCode());
	}

	private boolean filterBySearchQuery(InventoryHistory history, ProductLastInventorySearchRequest searchRequest) {
		if (!StringUtils.hasText(searchRequest.getQ())) {
			return true;
		}

		String query = searchRequest.getQ().toLowerCase();
		return (history.getProduct().getSkuCode() != null &&
				history.getProduct().getSkuCode().toLowerCase().contains(query)) ||
				(history.getProduct().getName() != null &&
						history.getProduct().getName().toLowerCase().contains(query)) ||
				(history.getRobot() != null &&
						history.getRobot().getCode() != null &&
						history.getRobot().getCode().toLowerCase().contains(query));
	}

	private boolean filterByRobots(InventoryHistory history, ProductLastInventorySearchRequest searchRequest) {
		if (CollectionUtils.isEmpty(searchRequest.getRobots())) {
			return true;
		}
		return history.getRobot() != null &&
				history.getRobot().getCode() != null &&
				searchRequest.getRobots().contains(history.getRobot().getCode());
	}

	private List<InventoryHistory> applySorting(List<InventoryHistory> records, Sort sort) {
		if (sort.isUnsorted()) {
			// Сортировка по умолчанию - по scannedAt (сначала новые, потом null)
			return records.stream()
					.sorted((a, b) -> {
						if (a.getScannedAt() == null && b.getScannedAt() == null) return 0;
						if (a.getScannedAt() == null) return 1;
						if (b.getScannedAt() == null) return -1;
						return b.getScannedAt().compareTo(a.getScannedAt());
					})
					.collect(Collectors.toList());
		}

		// Применяем сортировку из pageable
		return records.stream()
				.sorted((a, b) -> {
					for (Sort.Order order : sort) {
						int comparison = compareByField(a, b, order.getProperty(), order.getDirection());
						if (comparison != 0) {
							return comparison;
						}
					}
					return 0;
				})
				.collect(Collectors.toList());
	}

	private int compareByField(InventoryHistory a, InventoryHistory b, String field, Sort.Direction direction) {
		int multiplier = direction.isAscending() ? 1 : -1;

		switch (field) {
			case "productCode":
				return multiplier * compareStrings(a.getProduct().getSkuCode(), b.getProduct().getSkuCode());
			case "productName":
				return multiplier * compareStrings(a.getProduct().getName(), b.getProduct().getName());
			case "actualQuantity":
				return multiplier * Integer.compare(a.getQuantity(), b.getQuantity());
			case "lastScannedAt":
				return multiplier * compareDates(a.getScannedAt(), b.getScannedAt());
			case "statusCode":
				return multiplier * compareStrings(
						a.getStatus().getCode().toString(),
						b.getStatus().getCode().toString()
				);
			case "robotCode":
				return multiplier * compareStrings(
						a.getRobot() != null ? a.getRobot().getCode() : null,
						b.getRobot() != null ? b.getRobot().getCode() : null
				);
			default:
				return 0;
		}
	}

	private int compareStrings(String a, String b) {
		if (a == null && b == null) return 1;
		if (a == null) return -1;
		if (b == null) return 1;
		return a.compareToIgnoreCase(b);
	}

	private int compareDates(java.time.LocalDateTime a, java.time.LocalDateTime b) {
		if (a == null && b == null) return 0;
		if (a == null) return -1;
		if (b == null) return 1;
		return a.compareTo(b);
	}

	private List<InventoryHistory> applyPagination(List<InventoryHistory> records, Pageable pageable) {
		int start = (int) pageable.getOffset();
		int end = Math.min((start + pageable.getPageSize()), records.size());

		if (start > records.size()) {
			return Collections.emptyList();
		}

		return records.subList(start, end);
	}

	private ProductLastInventoryDTO mapToDTO(InventoryHistory history) {
		ProductLastInventoryDTO dto = new ProductLastInventoryDTO();
		dto.setProductCode(history.getProduct().getSkuCode());
		dto.setProductName(history.getProduct().getName());
		dto.setCategory(history.getProduct().getCategory());
		dto.setExpectedQuantity(history.getExpectedQuantity());
		dto.setActualQuantity(history.getQuantity());
		dto.setDifference(history.getDifference());
		dto.setLastScannedAt(history.getScannedAt());
		dto.setStatusCode(history.getStatus().getCode().toString());
		dto.setRobotCode(history.getRobot() != null ? history.getRobot().getCode() : null);
		return dto;
	}

	private void debugRecords(List<InventoryHistory> records) {
		log.info("=== DEBUG: First 10 records ===");
		for (int i = 0; i < Math.min(records.size(), 10); i++) {
			InventoryHistory record = records.get(i);
			log.info("Record {}: Product: {} (ID: {}), ScannedAt: {}",
					i,
					record.getProduct().getSkuCode(),
					record.getProduct().getId(),
					record.getScannedAt()
			);
		}
		log.info("=== END DEBUG ===");
	}
}