package ru.rtc.warehouse.warehouse.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.List;

public interface WarehouseEntityService extends CrudEntityService<Warehouse, Long> {
	Warehouse findByCode(String code);
	List<Warehouse> findByUserId(Long id);
	Warehouse saveAndReturn(Warehouse warehouse);
}