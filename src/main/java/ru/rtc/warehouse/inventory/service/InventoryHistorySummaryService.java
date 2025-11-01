package ru.rtc.warehouse.inventory.service;

import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.service.dto.HistorySummaryDTO;

public interface InventoryHistorySummaryService {
    HistorySummaryDTO summarize(String warehouseCode, InventoryHistorySearchRequest rq);
}
