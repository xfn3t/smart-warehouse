package ru.rtc.warehouse.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.mapper.InventoryHistoryMapper;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.inventory.service.InventoryHistoryQueryService;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;
import ru.rtc.warehouse.inventory.spec.InventoryHistorySpecifications;

/**
 * Реализация полнотекстового поиска по истории инвентаризаций.
 * <p>
 * - Строит JPA Specification из фильтров запроса;
 * - Делает пагинацию/сортировку;
 * - Маппит сущности в DTO через MapStruct.
 */
@Service
@RequiredArgsConstructor
public class InventoryHistoryQueryServiceImpl implements InventoryHistoryQueryService {

    private final InventoryHistoryRepository repository;
    private final InventoryHistoryMapper mapper;

    /**
     * Выполняет поиск по истории инвентаризаций с фильтрами, пагинацией и сортировкой.
     *
     * @param request  фильтры поиска (период, зоны, статусы, категории, q, роботы)
     * @param pageable пагинация/сортировка; если null — берётся DESC по {@code scannedAt}, size=20
     * @return страница DTO
     */
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryHistoryDTO> search(InventoryHistorySearchRequest request, Pageable pageable) {
        Pageable pg = (pageable == null)
                ? PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "scannedAt"))
                : pageable;

        Specification<InventoryHistory> spec = InventoryHistorySpecifications.build(request);

        // findAll со связями (robot, product) — см. @EntityGraph в репозитории
        return repository.findAll(spec, pg).map(mapper::toDto);
    }
}
