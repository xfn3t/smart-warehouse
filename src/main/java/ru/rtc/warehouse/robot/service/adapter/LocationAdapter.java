package ru.rtc.warehouse.robot.service.adapter;

import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.model.Warehouse;

public interface LocationAdapter {
    Location findByWarehouseAndZoneAndRowAndShelf(Warehouse warehouse, Integer zone, Integer row, Integer shelf);
}
