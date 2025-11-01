package ru.rtc.warehouse.inventory.service;


import org.springframework.data.domain.Pageable;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.service.dto.HistoryPageDTO;


/**
 * Сервис выборки исторических записей с динамическими фильтрами и пагинацией.
 */
public interface InventoryHistoryQueryService {
    /**
     * Выполняет поиск по истории согласно параметрам запроса.
     * @param request фильтры поиска
     * @param pageable пагинация и сортировка
     * @return страница DTO записей истории
     */
    HistoryPageDTO search(String warehouseCode, InventoryHistorySearchRequest request, Pageable pageable);
}