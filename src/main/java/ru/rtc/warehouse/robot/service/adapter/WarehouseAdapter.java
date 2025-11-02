package ru.rtc.warehouse.robot.service.adapter;

import ru.rtc.warehouse.warehouse.model.Warehouse;

public interface WarehouseAdapter {
    Warehouse findById(Long id);
    Warehouse findByCode(String code);
}
