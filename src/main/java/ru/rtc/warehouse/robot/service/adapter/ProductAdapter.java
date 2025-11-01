package ru.rtc.warehouse.robot.service.adapter;

import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.Optional;

public interface ProductAdapter {
    Optional<Product> findByCodeAndWarehouse(String code, Warehouse warehouse);
    Optional<Product> findByCode(String code);
}
