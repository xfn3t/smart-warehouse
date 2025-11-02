package ru.rtc.warehouse.robot.service.adapter;

import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.Optional;

public interface InventoryHistoryAdapter {
    Optional<InventoryHistory> findLatestByProductCodeAndLocationAndWarehouse(String productCode, Location location, Warehouse warehouse);
}
