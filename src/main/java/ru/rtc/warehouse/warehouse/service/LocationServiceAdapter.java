package ru.rtc.warehouse.warehouse.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.location.service.LocationService;
import ru.rtc.warehouse.warehouse.controller.dto.request.ExcludedCellDTO;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Service
@RequiredArgsConstructor
public class LocationServiceAdapter {

    private final LocationService locationService;

    public List<Location> generateLocationForWarehouse(
        Warehouse warehouse,
        List<ExcludedCellDTO> excludedCells
    ) {
        return locationService.generateLocationsForWarehouse(
            warehouse,
            excludedCells
        );
    }
}
