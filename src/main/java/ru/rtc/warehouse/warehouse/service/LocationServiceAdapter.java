package ru.rtc.warehouse.warehouse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.location.service.LocationService;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceAdapter {

	private final LocationService locationService;

	public List<Location> generateLocationForWarehouse(Warehouse warehouse) {
		return locationService.generateLocationsForWarehouse(warehouse);
	}

}
