package ru.rtc.warehouse.inventory.service.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.location.service.LocationEntityService;

@Service
@RequiredArgsConstructor
public class IHLocationEntServiceAdapter {

	private final LocationEntityService locationService;

	public Location findByCoordinate(Integer zone, Integer row, Integer shelf, Long warehouseId) {
		return locationService.findByCoordinate(zone, row, shelf, warehouseId);
	}

}
