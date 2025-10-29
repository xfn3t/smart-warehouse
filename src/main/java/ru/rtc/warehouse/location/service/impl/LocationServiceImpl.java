package ru.rtc.warehouse.location.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.location.model.LocationStatus;
import ru.rtc.warehouse.location.service.LocationEntityService;
import ru.rtc.warehouse.location.service.LocationService;
import ru.rtc.warehouse.location.service.LocationStatusEntityService;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

	private final LocationEntityService locationEntityService;
	private final LocationStatusEntityService locationStatusEntityService;

	@Transactional
	public List<Location> generateLocationsForWarehouse(Warehouse warehouse) {

		// Получаем существующие локации для этого склада
		List<Location> existingLocations = locationEntityService.findByWarehouse(warehouse);
		Map<String, Location> existingLocationMap = new HashMap<>();

		// Создаем карту существующих локаций по их координатам
		for (Location existingLocation : existingLocations) {
			String key = existingLocation.getZone() + "-" + existingLocation.getRow() + "-" + existingLocation.getShelf();
			existingLocationMap.put(key, existingLocation);
		}

		List<Location> locations = new ArrayList<>();
		LocationStatus defaultStatus = locationStatusEntityService.getDefaultStatus();

		for (int zone = 1; zone <= warehouse.getZoneMaxSize(); zone++) {
			for (int row = 1; row <= warehouse.getRowMaxSize(); row++) {
				for (int shelf = 1; shelf <= warehouse.getShelfMaxSize(); shelf++) {
					String key = zone + "-" + row + "-" + shelf;

					if (existingLocationMap.containsKey(key)) {
						locations.add(existingLocationMap.get(key));
					} else {
						Location location = new Location();
						location.setZone(zone);
						location.setRow(row);
						location.setShelf(shelf);
						location.setWarehouse(warehouse);
						location.setLocationStatus(defaultStatus);
						locations.add(location);
					}
				}
			}
		}

		return locationEntityService.saveAll(locations);
	}
}
