package ru.rtc.warehouse.robot.service.adapter.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.repository.ProductRepository;
import ru.rtc.warehouse.robot.service.adapter.ProductAdapter;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductAdapterImpl implements ProductAdapter {

    private final ProductRepository productRepository;

    @Override
    public Optional<Product> findByCodeAndWarehouse(String code, Warehouse warehouse) {
        return productRepository.findByCodeAndWarehouse(code, warehouse);
    }

    @Override
    public Optional<Product> findByCode(String code) {
        return productRepository.findByCode(code);
    }
}
