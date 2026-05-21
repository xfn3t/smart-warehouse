package ru.rtc.warehouse.location.service;

import java.util.List;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.controller.dto.request.ExcludedCellDTO;
import ru.rtc.warehouse.warehouse.model.Warehouse;

public interface LocationService {
    List<Location> generateLocationsForWarehouse(
        Warehouse warehouse,
        List<ExcludedCellDTO> excludedCells
    );
}
