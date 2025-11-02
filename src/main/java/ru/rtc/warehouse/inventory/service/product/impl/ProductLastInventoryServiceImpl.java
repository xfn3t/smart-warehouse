package ru.rtc.warehouse.inventory.service.product.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.rtc.warehouse.inventory.controller.dto.request.ProductLastInventorySearchRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.inventory.service.product.dto.ProductLastInventoryDTO;
import ru.rtc.warehouse.inventory.service.product.dto.ProductLastInventoryPageDTO;
import ru.rtc.warehouse.inventory.service.product.dto.ProductWithLastInventoryProjection;
import ru.rtc.warehouse.inventory.service.product.ProductLastInventoryService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductLastInventoryServiceImpl implements ProductLastInventoryService {

	private final InventoryHistoryRepository inventoryHistoryRepository;

	@Override
	@Transactional(readOnly = true)
	public ProductLastInventoryPageDTO getLastInventoryByWarehouse(String warehouseCode,
																   ProductLastInventorySearchRequest searchRequest,
																   Pageable pageable) {
		Pageable safePageable = createSafePageable(pageable);

		// Подготавливаем параметры для фильтрации
		List<String> categories = searchRequest != null && !CollectionUtils.isEmpty(searchRequest.getCategories()) ?
				searchRequest.getCategories() : null;

		List<String> statuses = searchRequest != null && !CollectionUtils.isEmpty(searchRequest.getStatuses()) ?
				searchRequest.getStatuses().stream()
						.map(InventoryHistoryStatus.InventoryHistoryStatusCode::toString)
						.collect(Collectors.toList()) : null;

		String searchQuery = searchRequest != null && StringUtils.hasText(searchRequest.getQ()) ?
				searchRequest.getQ() : null;

		List<String> robots = searchRequest != null && !CollectionUtils.isEmpty(searchRequest.getRobots()) ?
				searchRequest.getRobots() : null;

		// Используем новый метод репозитория с фильтрацией
		Page<ProductWithLastInventoryProjection> projectionsPage =
				inventoryHistoryRepository.findProductsWithLastInventoryByWarehouseWithFilters(
						warehouseCode, categories, statuses, searchQuery, robots, safePageable);

		List<ProductLastInventoryDTO> items = projectionsPage.getContent().stream()
				.map(this::mapToDTO)
				.collect(Collectors.toList());

		return ProductLastInventoryPageDTO.builder()
				.total(projectionsPage.getTotalElements())
				.page(projectionsPage.getNumber())
				.size(projectionsPage.getSize())
				.items(items)
				.build();
	}

	private ProductLastInventoryDTO mapToDTO(ProductWithLastInventoryProjection projection) {
		ProductLastInventoryDTO dto = new ProductLastInventoryDTO();
		dto.setProductCode(projection.getProductCode());
		dto.setProductName(projection.getProductName());
		dto.setCategory(projection.getCategory());
		dto.setExpectedQuantity(projection.getExpectedQuantity());
		dto.setActualQuantity(projection.getActualQuantity());
		dto.setDifference(projection.getDifference());
		dto.setLastScannedAt(projection.getLastScannedAt());
		dto.setStatusCode(projection.getStatusCode());
		dto.setRobotCode(projection.getRobotCode());
		return dto;
	}

	private Pageable createSafePageable(Pageable pageable) {
		if (pageable.getSort().isUnsorted()) {
			return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
					Sort.by(Sort.Direction.DESC, "lastScannedAt"));
		}

		List<Sort.Order> orders = pageable.getSort().stream()
				.map(order -> new Sort.Order(order.getDirection(), convertSortProperty(order.getProperty())))
				.collect(Collectors.toList());

		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
	}

	private String convertSortProperty(String property) {
		return switch (property) {
			case "productCode" -> "productCode";
			case "productName" -> "productName";
			case "actualQuantity" -> "actualQuantity";
			case "lastScannedAt" -> "lastScannedAt";
			case "statusCode" -> "statusCode";
			case "robotCode" -> "robotCode";
			default -> "productCode";
		};
	}
}