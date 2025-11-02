package ru.rtc.warehouse.location.service;

import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.List;

public interface LocationService {
	List<Location> generateLocationsForWarehouse(Warehouse warehouse);
}
