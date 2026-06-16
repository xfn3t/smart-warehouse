package ru.rtc.warehouse.robot.service.adapter.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.robot.service.adapter.InventoryHistoryAdapter;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Component
@RequiredArgsConstructor
public class InventoryHistoryAdapterImpl implements InventoryHistoryAdapter {

    private final InventoryHistoryRepository inventoryHistoryRepository;

    @Override
    public Optional<
        InventoryHistory
    > findLatestByProductCodeAndZoneAndRowAndShelfAndWarehouse(
        String productCode,
        Integer zone,
        Integer row,
        Integer shelf,
        Warehouse warehouse
    ) {
        return inventoryHistoryRepository.findFirstByProduct_SkuCodeAndZoneAndRowAndShelfAndWarehouseOrderByScannedAtDesc(
            productCode,
            zone,
            row,
            shelf,
            warehouse
        );
    }
}
