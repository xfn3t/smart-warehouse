package ru.rtc.warehouse.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.rtc.warehouse.inventory.controller.dto.request.InventoryHistorySearchRequest;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.service.adapter.IHWarehouseEntServiceAdapter;
import ru.rtc.warehouse.inventory.service.helper.InventoryHistoryQueryHelper;
import ru.rtc.warehouse.inventory.service.InventoryHistorySummaryService;
import ru.rtc.warehouse.inventory.service.dto.HistorySummaryDTO;
import ru.rtc.warehouse.inventory.spec.InventoryHistorySpecifications;
import ru.rtc.warehouse.inventory.util.QuickRangeResolver;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.time.Instant;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class InventoryHistorySummaryServiceImpl implements InventoryHistorySummaryService {

    private final IHWarehouseEntServiceAdapter warehouseService;
    private final InventoryHistoryQueryHelper queryHelper;

    @Override
    @Transactional(readOnly = true)
    public HistorySummaryDTO summarize(String warehouseCode, InventoryHistorySearchRequest rq) {
        // Получаем warehouseId
        Long warehouseId = getWarehouseId(warehouseCode);

        // null-safe: пустые фильтры допустимы
        InventoryHistorySearchRequest request = (rq == null) ? new InventoryHistorySearchRequest() : rq;

        // Валидация и обработка параметров периода
        validateAndProcessDateParameters(request);

        // Единая спецификация для всех запросов
        Specification<InventoryHistory> spec = InventoryHistorySpecifications.build(warehouseId, request);

        // Выполняем все запросы через helper
        long total = queryHelper.count(spec);
        long uniqueProducts = queryHelper.countDistinctProducts(spec);
        long discrepancies = queryHelper.countWithStatusNotOk(spec);

        // Используем безопасный вариант вычисления среднего
        Double avgMinutes = queryHelper.calculateAvgMinutesSafe(spec);

        return HistorySummaryDTO.builder()
                .total(total)
                .uniqueProducts(uniqueProducts)
                .discrepancies(discrepancies)
                .avgZoneScanMinutes(avgMinutes)
                .build();
    }

    private Long getWarehouseId(String warehouseCode) {
        Warehouse warehouse = warehouseService.validateAndGetWarehouse(warehouseCode);
        return warehouse.getId();
    }

    private void validateAndProcessDateParameters(InventoryHistorySearchRequest request) {
        if (request == null) return;

        // Проверка конфликтующих параметров
        if (request.getQuick() != null && (request.getFrom() != null || request.getTo() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Укажите либо quick, либо from/to — одновременно нельзя.");
        }

        // Обработка быстрого диапазона
        if (request.getQuick() != null && request.getFrom() == null && request.getTo() == null) {
            var zone = ZoneId.systemDefault();
            var range = QuickRangeResolver.resolve(request.getQuick(), zone);
            request.setFrom(range[0]);
            request.setTo(range[1]);
        }

        // Валидация from/to
        if (request.getFrom() != null && request.getTo() != null) {
            Instant from = request.getFrom();
            Instant to = request.getTo();
            if (!from.isBefore(to)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be < 'to'");
            }
        }
    }
}