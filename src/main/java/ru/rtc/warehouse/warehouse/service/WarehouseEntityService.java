package ru.rtc.warehouse.warehouse.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.warehouse.model.Warehouse;

public interface WarehouseEntityService extends CrudEntityService<Warehouse, Long> {
	Warehouse findByCode(String code);
}