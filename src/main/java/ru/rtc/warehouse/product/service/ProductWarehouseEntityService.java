package ru.rtc.warehouse.product.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.product.model.ProductWarehouse;

import java.util.List;
import java.util.Optional;

public interface ProductWarehouseEntityService extends CrudEntityService<ProductWarehouse, Long> {
	Optional<ProductWarehouse> findByProductIdAndWarehouseId(Long productId, Long warehouseId);
	List<ProductWarehouse> findByProductId(Long productId);
	List<ProductWarehouse> findByWarehouseId(Long warehouseId);
	Optional<ProductWarehouse> findActiveByProductAndWarehouse(Long productId, Long warehouseId);
	List<ProductWarehouse> findActiveByProductId(Long productId);
}