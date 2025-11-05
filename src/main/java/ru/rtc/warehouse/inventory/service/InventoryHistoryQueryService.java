package ru.rtc.warehouse.inventory.service;

import org.springframework.data.domain.Pageable;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.service.dto.HistoryPageDTO;

public interface InventoryHistoryQueryService {
    HistoryPageDTO search(String warehouseCode, InventoryHistorySearchRequest request, Pageable pageable);
}