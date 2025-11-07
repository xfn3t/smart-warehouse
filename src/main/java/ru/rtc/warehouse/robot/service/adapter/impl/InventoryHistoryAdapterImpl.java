package ru.rtc.warehouse.robot.service.adapter.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.robot.service.adapter.InventoryHistoryAdapter;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InventoryHistoryAdapterImpl implements InventoryHistoryAdapter {

    private final InventoryHistoryRepository inventoryHistoryRepository;

    @Override
    public Optional<InventoryHistory> findLatestByProductCodeAndLocationAndWarehouse(String productCode, Location location, Warehouse warehouse) {
        return inventoryHistoryRepository.findFirstByProduct_SkuCodeAndLocationAndWarehouseOrderByScannedAtDesc(productCode, location, warehouse);
    }
}