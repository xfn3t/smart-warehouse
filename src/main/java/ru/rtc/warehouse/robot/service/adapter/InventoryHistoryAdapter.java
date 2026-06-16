package ru.rtc.warehouse.robot.service.adapter;

import java.util.Optional;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.warehouse.model.Warehouse;

public interface InventoryHistoryAdapter {
    Optional<
        InventoryHistory
    > findLatestByProductCodeAndZoneAndRowAndShelfAndWarehouse(
        String productCode,
        Integer zone,
        Integer row,
        Integer shelf,
        Warehouse warehouse
    );
}
