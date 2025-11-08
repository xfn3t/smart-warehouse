package ru.rtc.warehouse.product.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.product.model.Product;

import java.util.List;

public interface ProductEntityService extends CrudEntityService<Product, Long> {
	Product findBySkuCode(String skuCode);
	List<Product> findAllActiveProducts();
	Long count();
	Product findByNameAndCategory(String name, String category);

	List<Product> findAllByWarehouseCode(String warehouseCode);
}