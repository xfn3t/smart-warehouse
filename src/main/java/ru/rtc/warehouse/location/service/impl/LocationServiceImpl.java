package ru.rtc.warehouse.location.service.impl;

import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.location.model.LocationStatus;
import ru.rtc.warehouse.location.service.LocationEntityService;
import ru.rtc.warehouse.location.service.LocationService;
import ru.rtc.warehouse.location.service.LocationStatusEntityService;
import ru.rtc.warehouse.warehouse.controller.dto.request.ExcludedCellDTO;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationEntityService locationEntityService;
    private final LocationStatusEntityService locationStatusEntityService;

    @Override
    @Transactional
    public List<Location> generateLocationsForWarehouse(
        Warehouse warehouse,
        List<ExcludedCellDTO> excludedCells
    ) {
        // Build a set of excluded (zone,row) pairs
        Set<String> excludedSet = Collections.emptySet();
        if (excludedCells != null && !excludedCells.isEmpty()) {
            excludedSet = new HashSet<>();
            for (ExcludedCellDTO ec : excludedCells) {
                if (ec.getZone() != null && ec.getRow() != null) {
                    excludedSet.add(ec.getZone() + "-" + ec.getRow());
                }
            }
        }

        List<Location> existingLocations =
            locationEntityService.findByWarehouse(warehouse);
        Map<String, Location> existingLocationMap = new HashMap<>();
        for (Location loc : existingLocations) {
            String key =
                loc.getZone() + "-" + loc.getRow() + "-" + loc.getShelf();
            existingLocationMap.put(key, loc);
        }

        List<Location> locations = new ArrayList<>();
        LocationStatus defaultStatus =
            locationStatusEntityService.getDefaultStatus();

        for (int zone = 1; zone <= warehouse.getZoneMaxSize(); zone++) {
            for (int row = 1; row <= warehouse.getRowMaxSize(); row++) {
                if (excludedSet.contains(zone + "-" + row)) {
                    continue;
                }
                for (
                    int shelf = 1;
                    shelf <= warehouse.getShelfMaxSize();
                    shelf++
                ) {
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

        // Remove orphan locations (no longer in the generated set) that have no FK references
        Set<Long> savedIds = saved
            .stream()
            .map(Location::getId)
            .collect(Collectors.toSet());
        for (Location existing : existingLocations) {
            if (!savedIds.contains(existing.getId())) {
                try {
                    locationEntityService.delete(existing.getId());
                } catch (Exception ignored) {
                    // FK constraint — location has inventory history, keep it
                }
            }
        }

        return saved;
    }
}
