package ru.rtc.warehouse.location.service;

import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.controller.dto.request.ExcludedCellDTO;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.List;

public interface LocationService {
	/**
	 * Generate all locations for a warehouse matrix.
	 * @param warehouse the warehouse
	 * @param excludedCells optional list of (zone,row) cells to skip; may be null or empty
	 */
	List<Location> generateLocationsForWarehouse(Warehouse warehouse, List<ExcludedCellDTO> excludedCells);
}
