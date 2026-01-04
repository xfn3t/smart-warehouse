package ru.rtc.warehouse.inventory.service.product.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.rtc.warehouse.inventory.controller.dto.request.ProductLastInventorySearchRequest;
import ru.rtc.warehouse.inventory.mapper.InventoryHistoryMapper;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.inventory.service.product.dto.ProductLastInventoryDTO;
import ru.rtc.warehouse.inventory.service.product.dto.ProductLastInventoryPageDTO;
import ru.rtc.warehouse.inventory.spec.InventoryHistorySpecifications;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductLastInventorySpecService {

	private final InventoryHistoryRepository inventoryHistoryRepository;
	private final InventoryHistoryMapper inventoryHistoryMapper;

	@Transactional(readOnly = true)
	public ProductLastInventoryPageDTO getLastInventoryByWarehouse(
			String warehouseCode,
			ProductLastInventorySearchRequest searchRequest,
			Pageable pageable) {

		log.info("Getting last inventory for warehouse: {} with filters: {}", warehouseCode, searchRequest);

		// Создаем безопасный Pageable
		Pageable safePageable = createSafePageable(pageable);

		// Подготавливаем параметры фильтрации
		List<String> categories = prepareCategories(searchRequest);
		List<InventoryHistoryStatus.InventoryHistoryStatusCode> statuses = prepareStatuses(searchRequest);
		String searchQuery = prepareSearchQuery(searchRequest);
		List<String> robots = prepareRobots(searchRequest);

		// Строим спецификацию
		Specification<InventoryHistory> spec = InventoryHistorySpecifications
				.buildProductLastInventorySpec(warehouseCode, categories, statuses, searchQuery, robots);

		// Выполняем запрос с пагинацией
		Page<InventoryHistory> historyPage = inventoryHistoryRepository.findAll(spec, safePageable);

		log.info("Found {} products with last inventory", historyPage.getTotalElements());

		// Маппим в DTO
		List<ProductLastInventoryDTO> items = historyPage.getContent().stream()
				.map(this::mapToDTO)
				.collect(Collectors.toList());

		return ProductLastInventoryPageDTO.builder()
				.total(historyPage.getTotalElements())
				.page(historyPage.getNumber())
				.size(historyPage.getSize())
				.items(items)
				.build();
	}

	// Альтернативный метод для случаев, когда нужна ручная обработка
	@Transactional(readOnly = true)
	public ProductLastInventoryPageDTO getLastInventoryByWarehouseManual(
			String warehouseCode,
			ProductLastInventorySearchRequest searchRequest,
			Pageable pageable) {

		log.info("Getting last inventory manually for warehouse: {}", warehouseCode);

		Pageable safePageable = createSafePageable(pageable);

		// Получаем все последние записи для склада
		List<InventoryHistory> allLatest = inventoryHistoryRepository
				.findLatestByWarehouseGroupedByProduct(warehouseCode);

		log.info("Found {} latest records before filtering", allLatest.size());

		// Применяем фильтры
		List<InventoryHistory> filtered = allLatest.stream()
				.filter(history -> filterByCategories(history, searchRequest))
				.filter(history -> filterByStatuses(history, searchRequest))
				.filter(history -> filterBySearchQuery(history, searchRequest))
				.filter(history -> filterByRobots(history, searchRequest))
				.collect(Collectors.toList());

		log.info("After filtering: {} records", filtered.size());

		// Применяем пагинацию
		List<InventoryHistory> paginated = applyPagination(filtered, safePageable);

		List<ProductLastInventoryDTO> items = paginated.stream()
				.map(this::mapToDTO)
				.collect(Collectors.toList());

		return ProductLastInventoryPageDTO.builder()
				.total(filtered.size())
				.page(safePageable.getPageNumber())
				.size(safePageable.getPageSize())
				.items(items)
				.build();
	}

	// Вспомогательные методы для подготовки параметров

	private List<String> prepareCategories(ProductLastInventorySearchRequest searchRequest) {
		return searchRequest != null && !CollectionUtils.isEmpty(searchRequest.getCategories()) ?
				searchRequest.getCategories() : Collections.emptyList();
	}

	private List<InventoryHistoryStatus.InventoryHistoryStatusCode> prepareStatuses(ProductLastInventorySearchRequest searchRequest) {
		return searchRequest != null && !CollectionUtils.isEmpty(searchRequest.getStatuses()) ?
				searchRequest.getStatuses() : Collections.emptyList();
	}

	private String prepareSearchQuery(ProductLastInventorySearchRequest searchRequest) {
		return searchRequest != null && StringUtils.hasText(searchRequest.getQ()) ?
				searchRequest.getQ() : null;
	}

	private List<String> prepareRobots(ProductLastInventorySearchRequest searchRequest) {
		return searchRequest != null && !CollectionUtils.isEmpty(searchRequest.getRobots()) ?
				searchRequest.getRobots() : Collections.emptyList();
	}

	// Ручные фильтры (для альтернативного метода)

	private boolean filterByCategories(InventoryHistory history, ProductLastInventorySearchRequest searchRequest) {
		if (searchRequest == null || CollectionUtils.isEmpty(searchRequest.getCategories())) {
			return true;
		}
		return searchRequest.getCategories().contains(history.getProduct().getCategory());
	}

	private boolean filterByStatuses(InventoryHistory history, ProductLastInventorySearchRequest searchRequest) {
		if (searchRequest == null || CollectionUtils.isEmpty(searchRequest.getStatuses())) {
			return true;
		}
		return searchRequest.getStatuses().contains(history.getStatus().getCode());
	}

	private boolean filterBySearchQuery(InventoryHistory history, ProductLastInventorySearchRequest searchRequest) {
		if (searchRequest == null || !StringUtils.hasText(searchRequest.getQ())) {
			return true;
		}
		String query = searchRequest.getQ().toLowerCase();
		return (history.getProduct().getSkuCode() != null && history.getProduct().getSkuCode().toLowerCase().contains(query)) ||
				(history.getProduct().getName() != null && history.getProduct().getName().toLowerCase().contains(query)) ||
				(history.getRobot() != null && history.getRobot().getCode() != null &&
						history.getRobot().getCode().toLowerCase().contains(query));
	}

	private boolean filterByRobots(InventoryHistory history, ProductLastInventorySearchRequest searchRequest) {
		if (searchRequest == null || CollectionUtils.isEmpty(searchRequest.getRobots())) {
			return true;
		}
		return history.getRobot() != null &&
				history.getRobot().getCode() != null &&
				searchRequest.getRobots().contains(history.getRobot().getCode());
	}

	// Маппинг и утилиты

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

	private Pageable createSafePageable(Pageable pageable) {
		if (pageable.getSort().isUnsorted()) {
			return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
					Sort.by(Sort.Direction.DESC, "scannedAt"));
		}

		// Конвертируем свойства сортировки
		List<Sort.Order> orders = pageable.getSort().stream()
				.map(order -> new Sort.Order(order.getDirection(), convertSortProperty(order.getProperty())))
				.collect(Collectors.toList());

		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
	}

	private String convertSortProperty(String property) {
		return switch (property) {
			case "productCode" -> "product.skuCode";
			case "productName" -> "product.name";
			case "actualQuantity" -> "quantity";
			case "lastScannedAt" -> "scannedAt";
			case "statusCode" -> "status.code";
			case "robotCode" -> "robot.code";
			default -> property;
		};
	}

	private List<InventoryHistory> applyPagination(List<InventoryHistory> content, Pageable pageable) {
		int start = (int) pageable.getOffset();
		int end = Math.min((start + pageable.getPageSize()), content.size());

		if (start > content.size()) {
			return List.of();
		}

		return content.subList(start, end);
	}
}