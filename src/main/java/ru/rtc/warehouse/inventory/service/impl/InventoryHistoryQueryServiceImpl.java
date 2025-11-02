package ru.rtc.warehouse.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.mapper.InventoryHistoryMapper;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.service.adapter.IHWarehouseEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.helper.InventoryHistoryQueryHelper;
import ru.rtc.warehouse.inventory.service.InventoryHistoryQueryService;
import ru.rtc.warehouse.inventory.service.dto.HistoryPageDTO;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;
import ru.rtc.warehouse.inventory.spec.InventoryHistorySpecifications;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryHistoryQueryServiceImpl implements InventoryHistoryQueryService {

    private final IHWarehouseEntServiceAdapter warehouseService;
    private final InventoryHistoryQueryHelper queryHelper;
    private final InventoryHistoryMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public HistoryPageDTO search(String warehouseCode, InventoryHistorySearchRequest request, Pageable pageable) {
        // Валидация склада
        Warehouse warehouse = warehouseService.validateAndGetWarehouse(warehouseCode);

        // Создаем безопасный Pageable с правильными путями для сортировки
        Pageable safePageable = createSafePageable(pageable);

        // Построение спецификации
        Specification<InventoryHistory> spec = InventoryHistorySpecifications.build(warehouse.getId(), request);

        // Получаем общее количество для пагинации
        long total = queryHelper.count(spec);

        // Получаем данные с пагинацией
        List<InventoryHistory> content = queryHelper.findAll(spec);

        // Применяем пагинацию вручную (так как мы уже загрузили все данные)
        List<InventoryHistory> paginatedContent = applyPagination(content, safePageable);

        // Маппим в DTO
        List<InventoryHistoryDTO> dtoContent = mapper.toDtoList(paginatedContent);

        return HistoryPageDTO.builder()
                .total(total)
                .page(safePageable.getPageNumber())
                .size(safePageable.getPageSize())
                .items(dtoContent)
                .build();
    }

    private Pageable createSafePageable(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "scannedAt"));
        }

        // Преобразуем сортировку к правильным путям сущности
        List<Sort.Order> orders = pageable.getSort().stream()
                .map(order -> new Sort.Order(order.getDirection(), convertSortProperty(order.getProperty())))
                .collect(Collectors.toList());

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }

    private String convertSortProperty(String property) {
		return switch (property) {
			case "zone" -> "location.zone";
			case "rowNumber" -> "location.rowNumber";
			case "shelfNumber" -> "location.shelfNumber";
			case "status" -> "status.code";
			case "robotCode" -> "robot.code";
			case "skuCode" -> "product.code";
			case "productName" -> "product.name";
			default -> property; // scannedAt, quantity и другие прямые поля
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