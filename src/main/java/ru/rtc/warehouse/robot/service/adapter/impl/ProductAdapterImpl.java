package ru.rtc.warehouse.robot.service.adapter.impl;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.repository.ProductRepository;
import ru.rtc.warehouse.robot.service.adapter.ProductAdapter;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Component
@RequiredArgsConstructor
public class ProductAdapterImpl implements ProductAdapter {

    private final ProductRepository productRepository;

    @Override
    public Optional<Product> findByCodeAndWarehouse(
        String code,
        Warehouse warehouse
    ) {
        List<Product> products =
            productRepository.findBySkuCodeAndWarehouseCode(
                code,
                warehouse.getCode()
            );
        return products.isEmpty()
            ? Optional.empty()
            : Optional.of(products.get(0));
    }

    @Override
    public Optional<Product> findByCode(String code) {
        List<Product> products =
            productRepository.findAnyBySkuCodeAndIsDeletedFalse(code);
        return products.isEmpty()
            ? Optional.empty()
            : Optional.of(products.get(0));
    }
}
