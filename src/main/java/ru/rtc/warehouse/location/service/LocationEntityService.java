package ru.rtc.warehouse.location.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.List;

public interface LocationEntityService extends CrudEntityService<Location, Long> {
	List<Location> saveAll(List<Location> locations);

	List<Location> findByWarehouse(Warehouse warehouse);
}
