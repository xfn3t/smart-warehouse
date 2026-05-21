package ru.rtc.warehouse.location.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.location.model.LocationStatus;
import ru.rtc.warehouse.location.service.LocationEntityService;
import ru.rtc.warehouse.location.service.LocationService;
import ru.rtc.warehouse.location.service.LocationStatusEntityService;
import ru.rtc.warehouse.warehouse.controller.dto.request.ExcludedCellDTO;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationServiceImpl implements LocationService {

	private final LocationEntityService locationEntityService;
	private final LocationStatusEntityService locationStatusEntityService;

	@Override
	@Transactional
	public List<Location> generateLocationsForWarehouse(Warehouse warehouse, List<ExcludedCellDTO> excludedCells) {

		// Build a set of excluded (zone,row) pairs for O(1) lookup
		Set<String> excludedSet = Collections.emptySet();
		if (excludedCells != null && !excludedCells.isEmpty()) {
			excludedSet = new HashSet<>();
			for (ExcludedCellDTO ec : excludedCells) {
				if (ec.getZone() != null && ec.getRow() != null) {
					excludedSet.add(ec.getZone() + "-" + ec.getRow());
				}
			}
		}

		// Existing locations map
		List<Location> existingLocations = locationEntityService.findByWarehouse(warehouse);
		Map<String, Location> existingLocationMap = new HashMap<>();
		for (Location existingLocation : existingLocations) {
			String key = existingLocation.getZone() + "-" + existingLocation.getRow() + "-" + existingLocation.getShelf();
			existingLocationMap.put(key, existingLocation);
		}

		List<Location> locations = new ArrayList<>();
		LocationStatus defaultStatus = locationStatusEntityService.getDefaultStatus();

		for (int zone = 1; zone <= warehouse.getZoneMaxSize(); zone++) {
			for (int row = 1; row <= warehouse.getRowMaxSize(); row++) {
				// Skip excluded (zone,row) — all shelves for this cell are omitted
				if (excludedSet.contains(zone + "-" + row)) {
					continue;
				}
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

		List<Location> saved = locationEntityService.saveAll(locations);

		// Remove orphan locations (no longer in the generated set) that have no inventory history
		Set<Long> savedIds = saved.stream().map(Location::getId).collect(java.util.stream.Collectors.toSet());
		for (Location existing : existingLocations) {
			if (!savedIds.contains(existing.getId())) {
				try {
					locationEntityService.delete(existing.getId());
				} catch (Exception e) {
					// FK constraint — location has inventory history, keep it
				}
			}
		}

		return saved;
	}
}
