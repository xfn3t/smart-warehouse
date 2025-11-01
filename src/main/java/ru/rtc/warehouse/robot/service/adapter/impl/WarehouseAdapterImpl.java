package ru.rtc.warehouse.robot.service.adapter.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ru.rtc.warehouse.robot.service.adapter.WarehouseAdapter;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

@Component
@RequiredArgsConstructor
public class WarehouseAdapterImpl implements WarehouseAdapter {

    private final WarehouseEntityService warehouseEntityService;

    @Override
    public Warehouse findById(Long id) {
        return warehouseEntityService.findById(id);
    }

    @Override
    public Warehouse findByCode(String code) {
        return warehouseEntityService.findByCode(code);
    }
}
