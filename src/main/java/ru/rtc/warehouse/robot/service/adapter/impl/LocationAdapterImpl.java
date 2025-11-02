package ru.rtc.warehouse.robot.service.adapter.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.location.repository.LocationRepository;
import ru.rtc.warehouse.robot.service.adapter.LocationAdapter;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LocationAdapterImpl implements LocationAdapter {

    private final LocationRepository locationRepository;

    @Override
    public Optional<Location> findByWarehouseAndZoneAndRowAndShelf(Warehouse warehouse, Integer zone, Integer row, Integer shelf) {
        return locationRepository.findByWarehouseAndZoneAndRowAndShelf(warehouse, zone, row, shelf);
    }
}