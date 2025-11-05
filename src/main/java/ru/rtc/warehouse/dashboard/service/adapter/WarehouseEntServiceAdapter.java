package ru.rtc.warehouse.dashboard.service.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseEntServiceAdapter {

	private final WarehouseEntityService warehouseEntityService;

	public List<Warehouse> findAll() {
		return warehouseEntityService.findAll();
	}
}
