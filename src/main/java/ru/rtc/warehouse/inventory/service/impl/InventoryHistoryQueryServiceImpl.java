package ru.rtc.warehouse.inventory.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.inventory.service.InventoryHistoryQueryService;
import ru.rtc.warehouse.inventory.service.dto.InventoryHistoryDTO;
import ru.rtc.warehouse.inventory.spec.InventoryHistorySpecifications;


import java.util.stream.Collectors;


/**
 * Реализация сервиса поиска с использованием JPA Specifications.
 */
@Service
@RequiredArgsConstructor
public class InventoryHistoryQueryServiceImpl implements InventoryHistoryQueryService {


    private final InventoryHistoryRepository repository;


    @Override
    @Transactional(readOnly = true)
    public Page<InventoryHistoryDTO> search(InventoryHistorySearchRequest request, Pageable pageable) {
        // дефолтная пагинация/сортировка, если контроллер не прислал
        if (pageable == null) {
            pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "scannedAt"));
        }

        Specification<InventoryHistory> spec = InventoryHistorySpecifications.build(request);

        // основная выборка
        Page<InventoryHistory> page = repository.findAll(spec, pageable);

        // маппинг Page<entity> -> Page<dto>
        return page.map(this::toDto);
    }


    /**
     * Маппинг сущности в DTO, без использования сторонних мапперов,
     * чтобы избежать N+1 и контролировать выборку полей.
     */
    private InventoryHistoryDTO toDto(InventoryHistory e) {
        InventoryHistoryDTO dto = new InventoryHistoryDTO();
        dto.setId(e.getId());
        dto.setRobotCode(e.getRobot() != null ? e.getRobot().getCode() : null);
        dto.setSkuCode(e.getProduct() != null ? e.getProduct().getCode() : null);
        dto.setProductName(e.getProduct() != null ? e.getProduct().getName() : null);
        dto.setQuantity(e.getQuantity());
        dto.setZone(e.getZone());
        dto.setRowNumber(e.getRowNumber());
        dto.setShelfNumber(e.getShelfNumber());
        dto.setStatus(e.getStatus());
        dto.setScannedAt(e.getScannedAt());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}