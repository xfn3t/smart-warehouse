package ru.rtc.warehouse.robot.service.adapter.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.location.service.LocationEntityService;
import ru.rtc.warehouse.robot.service.adapter.LocationAdapter;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Component
@RequiredArgsConstructor
public class LocationAdapterImpl implements LocationAdapter {

    private final LocationEntityService locationService;

    @Override
    public Location findByWarehouseAndZoneAndRowAndShelf(Warehouse warehouse, Integer zone, Integer row, Integer shelf) {
        return locationService.findByCoordinate(zone, row, shelf, warehouse.getId());
    }
}